/*
 * Copyright 2020 The mssqlproxy developers (see CONTRIBUTORS)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dajudge.mssqlproxy.core.protocol.transport;

import com.dajudge.mssqlproxy.core.util.EncodingUtil;
import com.dajudge.mssqlproxy.core.util.PasswordUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static com.dajudge.mssqlproxy.core.util.BinaryUtils.*;
import static java.lang.System.arraycopy;
import static java.util.function.Function.identity;

public class Login7Message {
    private static final Logger LOG = LoggerFactory.getLogger(Login7Message.class);

    protected static final int YUKON = 0x72090002;
    private int tdsVersion;
    private int requestedPacketSize;
    private byte[] interfaceLibVersion = new byte[4];
    private int clientProcessId;
    private int primaryServerConnectionId;
    private byte optionFlags1;
    private byte optionFlags2;
    private byte typeFlags;
    private byte optionFlags3;
    private int clientTimeZone;
    private int clientLCID;
    private byte[] clientId = new byte[6];
    private String hostname;
    private String username;
    private String password;
    private String appName;
    private String serverName;
    private String libraryName;
    private String database;
    private String language;
    private int secBlobLengthOverride;
    private byte[] secBlob;
    private String passwordChange;
    private String atchDbFile;
    private List<Extension> extensions = new ArrayList<>();

    public Login7Message(final byte[] payload) {
        // SQLServerConnectioStringn.java L4981
        tdsVersion = readInt(payload, 4);
        requestedPacketSize = readInt(payload, 8);
        arraycopy(payload, 12, interfaceLibVersion, 0, interfaceLibVersion.length);
        clientProcessId = readInt(payload, 16);
        primaryServerConnectionId = readInt(payload, 20);
        optionFlags1 = payload[24];
        optionFlags2 = payload[25];
        typeFlags = payload[26];
        optionFlags3 = payload[27];
        clientTimeZone = readInt(payload, 28);
        clientLCID = readInt(payload, 32);
        hostname = readString(payload, 36, EncodingUtil::fromUCS16);
        username = readString(payload, 40, EncodingUtil::fromUCS16);
        password = readString(payload, 44, PasswordUtil::decryptPassword);
        appName = readString(payload, 48, EncodingUtil::fromUCS16);
        serverName = readString(payload, 52, EncodingUtil::fromUCS16);
        final byte[] aeBlob = readBlob(payload, 56, identity());
        libraryName = readString(payload, 60, EncodingUtil::fromUCS16);
        language = readString(payload, 64, EncodingUtil::fromUCS16);
        database = readString(payload, 68, EncodingUtil::fromUCS16);
        arraycopy(payload, 72, clientId, 0, clientId.length);
        secBlobLengthOverride = tdsVersion < YUKON
                ? 0
                : readInt(payload, 90);
        secBlob = readBlob(payload, 78, secBlobLengthOverride, 1, identity());
        atchDbFile = readString(payload, 82, EncodingUtil::fromUCS16);
        passwordChange = tdsVersion < YUKON
                ? ""
                : readString(payload, 86, PasswordUtil::decryptPassword);
        LOG.info("AE blob length: {}", aeBlob.length);
        if (aeBlob.length > 0) {
            final int aeOffset = readInt(aeBlob, 0);
            LOG.info("AE offset: {}", aeOffset);
            if (hasExtensions(optionFlags3)) {
                int currentOffset = aeOffset;
                while (payload[currentOffset] != -1) {
                    // https://github.com/microsoft/mssql-jdbc/blob/bfa3826038f675a75107f0723248130d25ca64c6/src/main/java/com/microsoft/sqlserver/jdbc/SQLServerConnection.java#L5103
                    final byte extType = payload[currentOffset];
                    final int extLen = readInt(payload, currentOffset + 1);
                    final byte[] data = new byte[extLen];
                    LOG.info("Ext feature: {}, {}", extType, extLen);
                    if (extLen > 0) {
                        arraycopy(payload, currentOffset + 5, data, 0, extLen);
                    }
                    extensions.add(new Extension(extType, data));
                    currentOffset += extLen + 5;
                }
                LOG.info("Done ext features");
            }
        }
    }

    private boolean hasExtensions(final byte optionFlags3) {
        return (optionFlags3 & (1 << 5)) > 0;
    }

    public byte[] serialize() {
        final int blobsLen = hostname.length() * 2 +
                username.length() * 2 +
                password.length() * 2 +
                appName.length() * 2 +
                serverName.length() * 2 +
                4 + // aeBlob
                libraryName.length() * 2 +
                language.length() * 2 +
                database.length() * 2 +
                secBlob.length +
                atchDbFile.length() * 2 +
                passwordChange.length() * 2;
        final int baseLen = 86 + (tdsVersion >= YUKON ? 8 : 0);
        final int extDataLen = 1 + (hasExtensions(optionFlags3) ? calcExtensionsLength() : 0);
        final int len = baseLen + blobsLen + extDataLen;
        final byte[] payload = new byte[len];
        for (int i = 0; i < payload.length; i++) {
            payload[i] = (byte) (Math.random() * 256);
        }
        final short hostnameOffset = (short) baseLen;
        final short usernameOffset = (short) (hostnameOffset + hostname.length() * 2);
        final short passwordOffset = (short) (usernameOffset + username.length() * 2);
        final short appNameOffset = (short) (passwordOffset + password.length() * 2);
        final short serverNameOffset = (short) (appNameOffset + appName.length() * 2);
        final short aeBlobOffset = (short) (serverNameOffset + serverName.length() * 2);
        final short libraryNameOffset = (short) (aeBlobOffset + 4);
        final short languageOffset = (short) (libraryNameOffset + libraryName.length() * 2);
        final short databaseOffset = (short) (languageOffset + language.length() * 2);
        final short secBlobOffset = (short) (databaseOffset + database.length() * 2);
        final short atchDbFileOffset = (short) (secBlobOffset + secBlob.length);
        final short passwordChangeOffset = (short) (atchDbFileOffset + atchDbFile.length() * 2);
        final short extBlobOffset = (short) (passwordChangeOffset + passwordChange.length() * 2);
        writeString(payload, 36, hostname, hostnameOffset);
        writeInt(payload, 0, len);
        writeInt(payload, 4, tdsVersion);
        writeInt(payload, 8, requestedPacketSize);
        arraycopy(interfaceLibVersion, 0, payload, 12, interfaceLibVersion.length);
        writeInt(payload, 16, clientProcessId);
        writeInt(payload, 20, primaryServerConnectionId);
        payload[24] = optionFlags1;
        payload[25] = optionFlags2;
        payload[26] = typeFlags;
        payload[27] = optionFlags3;
        writeInt(payload, 28, clientTimeZone);
        writeInt(payload, 32, clientLCID);
        writeString(payload, 40, username, usernameOffset);
        writePassword(payload, 44, password, passwordOffset);
        writeString(payload, 48, appName, appNameOffset);
        writeString(payload, 52, serverName, serverNameOffset);
        final byte[] aeBlob = new byte[4];
        writeInt(aeBlob, 0, extBlobOffset);
        writeBlob(payload, 56, aeBlob, aeBlobOffset, (short) aeBlob.length, identity());
        writeString(payload, 60, libraryName, libraryNameOffset);
        writeString(payload, 64, language, languageOffset);
        writeString(payload, 68, database, databaseOffset);
        arraycopy(clientId, 0, payload, 72, clientId.length);
        writeBlob(payload, 78, secBlob, secBlobOffset, (short) secBlob.length, identity());
        writeString(payload, 82, atchDbFile, atchDbFileOffset);
        if (tdsVersion >= YUKON) {
            writePassword(payload, 86, passwordChange, passwordChangeOffset);
            writeInt(payload, 90, secBlob.length <= 65535 ? 0 : secBlob.length);
        }
        if (readInt(aeBlob, 0) > 0) {
            final byte[] extBlob = buildExtBlob();
            arraycopy(extBlob, 0, payload, extBlobOffset, extBlob.length);
            payload[extBlobOffset + extBlob.length] = -1;
        }
        return payload;
    }

    private Integer calcExtensionsLength() {
        return extensions.stream().map(Extension::length).reduce(0, Integer::sum);
    }

    public int getTdsVersion() {
        return tdsVersion;
    }

    public void setTdsVersion(final int tdsVersion) {
        this.tdsVersion = tdsVersion;
    }

    public int getRequestedPacketSize() {
        return requestedPacketSize;
    }

    public void setRequestedPacketSize(final int requestedPacketSize) {
        this.requestedPacketSize = requestedPacketSize;
    }

    public byte[] getInterfaceLibVersion() {
        return interfaceLibVersion;
    }

    public void setInterfaceLibVersion(final byte[] interfaceLibVersion) {
        this.interfaceLibVersion = interfaceLibVersion;
    }

    public int getClientProcessId() {
        return clientProcessId;
    }

    public void setClientProcessId(final int clientProcessId) {
        this.clientProcessId = clientProcessId;
    }

    public int getPrimaryServerConnectionId() {
        return primaryServerConnectionId;
    }

    public void setPrimaryServerConnectionId(final int primaryServerConnectionId) {
        this.primaryServerConnectionId = primaryServerConnectionId;
    }

    public byte getOptionFlags1() {
        return optionFlags1;
    }

    public void setOptionFlags1(final byte optionFlags1) {
        this.optionFlags1 = optionFlags1;
    }

    public byte getOptionFlags2() {
        return optionFlags2;
    }

    public void setOptionFlags2(final byte optionFlags2) {
        this.optionFlags2 = optionFlags2;
    }

    public byte getTypeFlags() {
        return typeFlags;
    }

    public void setTypeFlags(final byte typeFlags) {
        this.typeFlags = typeFlags;
    }

    public byte getOptionFlags3() {
        return optionFlags3;
    }

    public void setOptionFlags3(final byte optionFlags3) {
        this.optionFlags3 = optionFlags3;
    }

    public int getClientTimeZone() {
        return clientTimeZone;
    }

    public void setClientTimeZone(final int clientTimeZone) {
        this.clientTimeZone = clientTimeZone;
    }

    public int getClientLCID() {
        return clientLCID;
    }

    public void setClientLCID(final int clientLCID) {
        this.clientLCID = clientLCID;
    }

    public byte[] getClientId() {
        return clientId;
    }

    public void setClientId(final byte[] clientId) {
        this.clientId = clientId;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(final String hostname) {
        this.hostname = hostname;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(final String appName) {
        this.appName = appName;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(final String serverName) {
        this.serverName = serverName;
    }

    public String getLibraryName() {
        return libraryName;
    }

    public void setLibraryName(final String libraryName) {
        this.libraryName = libraryName;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(final String database) {
        this.database = database;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(final String language) {
        this.language = language;
    }

    public int getSecBlobLengthOverride() {
        return secBlobLengthOverride;
    }

    public void setSecBlobLengthOverride(final int secBlobLengthOverride) {
        this.secBlobLengthOverride = secBlobLengthOverride;
    }

    public byte[] getSecBlob() {
        return secBlob;
    }

    public void setSecBlob(final byte[] secBlob) {
        this.secBlob = secBlob;
    }

    public String getPasswordChange() {
        return passwordChange;
    }

    public void setPasswordChange(final String passwordChange) {
        this.passwordChange = passwordChange;
    }

    public String getAtchDbFile() {
        return atchDbFile;
    }

    public void setAtchDbFile(final String atchDbFile) {
        this.atchDbFile = atchDbFile;
    }

    public List<Extension> getExtensions() {
        return extensions;
    }

    public void setExtensions(final List<Extension> extensions) {
        this.extensions = extensions;
    }

    private byte[] buildExtBlob() {
        final int blobSize = calcExtensionsLength();
        final byte[] data = new byte[blobSize];
        int offset = 0;
        for (final Extension extension : extensions) {
            extension.serialize(data, offset);
            offset += extension.length();
        }
        return data;
    }

    private static void writeString(
            final byte[] payload,
            final int headerOffset,
            final String string,
            final short stringOffset
    ) {
        writeBlob(payload, headerOffset, string, stringOffset, (short) string.length(), EncodingUtil::toUCS16);
    }

    private static void writePassword(
            final byte[] payload,
            final int headerOffset,
            final String string,
            final short stringOffset
    ) {
        writeBlob(payload, headerOffset, string, stringOffset, (short) string.length(), PasswordUtil::encryptPassword);
    }

    private static <T> void writeBlob(
            final byte[] payload,
            final int headerOffset,
            final T blob,
            final short blobOffset,
            final short blobLength,
            final Function<T, byte[]> encoder
    ) {
        writeShort(payload, headerOffset, blobLength == 0 ? 0 : blobOffset);
        writeShort(payload, headerOffset + 2, blobLength);
        final byte[] bytes = encoder.apply(blob);
        arraycopy(bytes, 0, payload, blobOffset, bytes.length);
    }

    @Override
    public String toString() {
        return "Login7Message{" +
                ", tdsVersion=" + tdsVersion +
                ", requestedPacketSize=" + requestedPacketSize +
                ", interfaceLibVersion=" + Arrays.toString(interfaceLibVersion) +
                ", clientProcessId=" + clientProcessId +
                ", primaryServerConnectionId=" + primaryServerConnectionId +
                ", optionFlags1=" + optionFlags1 +
                ", optionFlags2=" + optionFlags2 +
                ", typeFlags=" + typeFlags +
                ", optionFlags3=" + optionFlags3 +
                ", clientTimeZone=" + clientTimeZone +
                ", clientLCID=" + clientLCID +
                ", clientId=" + Arrays.toString(clientId) +
                ", hostname='" + hostname + '\'' +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", appName='" + appName + '\'' +
                ", serverName='" + serverName + '\'' +
                ", libraryName='" + libraryName + '\'' +
                ", database='" + database + '\'' +
                ", language='" + language + '\'' +
                ", secBlobLengthOverride=" + secBlobLengthOverride +
                ", secBlob=" + Arrays.toString(secBlob) +
                ", passwordChange='" + passwordChange + '\'' +
                ", atchDbFile='" + atchDbFile + '\'' +
                ", extensions=" + extensions +
                '}';
    }

    private static String readString(
            final byte[] payload,
            final int locatorOffset,
            final Function<byte[], String> decoder
    ) {
        return readBlob(payload, locatorOffset, null, 2, decoder);
    }

    private static <T> T readBlob(final byte[] payload, final int locatorOffset, final Function<byte[], T> decoder) {
        return readBlob(payload, locatorOffset, null, 1, decoder);
    }

    private static <T> T readBlob(
            final byte[] payload,
            final int locatorOffset,
            final Integer lenOverride,
            final int stride,
            final Function<byte[], T> decoder
    ) {
        final short offset = readShort(payload, locatorOffset);
        final int len = (lenOverride != null && lenOverride != 0) ? lenOverride : readShort(payload, locatorOffset + 2);
        if (len != 0) {
            final byte[] bytes = new byte[len * stride];
            arraycopy(payload, offset, bytes, 0, len * stride);
            return decoder.apply(bytes);
        } else {
            return decoder.apply(new byte[0]);
        }
    }

    public static class Extension {
        private final byte type;
        private final byte[] data;

        public Extension(final byte type, final byte[] data) {
            this.type = type;
            this.data = data;
        }

        public int length() {
            return data.length + 5;
        }

        @Override
        public String toString() {
            return "Extension{" +
                    "type=" + type +
                    ", data=" + Arrays.toString(data) +
                    '}';
        }

        public void serialize(final byte[] data, final int offset) {
            data[offset] = type;
            writeInt(data, offset + 1, this.data.length);
            arraycopy(this.data, 0, data, offset + 5, this.data.length);
        }
    }
}
