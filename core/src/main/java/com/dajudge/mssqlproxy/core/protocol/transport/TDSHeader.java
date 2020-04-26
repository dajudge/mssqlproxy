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

import static com.dajudge.mssqlproxy.core.util.BinaryUtils.*;

public class TDSHeader {
    public static final int TDS_HEADER_SIZE = 8;
    private final int messageType;
    private final int status;
    private final int length;
    private final int spid;
    private final int sequenceNum;
    private final int window;

    public TDSHeader(
            final int messageType,
            final int status,
            final int length,
            final int spid,
            final int sequenceNum,
            final int window
    ) {
        this.messageType = messageType;
        this.status = status;
        this.length = length;
        this.spid = spid;
        this.sequenceNum = sequenceNum;
        this.window = window;
    }

    public TDSHeader(final byte[] headerBytes) {
        // IOBuffer.java L4084
        this(readUnsignedByte(headerBytes, 0),
                readUnsignedByte(headerBytes, 1),
                readUnsignedShortBigEndian(headerBytes, 2),
                readUnsignedShortBigEndian(headerBytes, 4),
                readUnsignedByte(headerBytes, 6),
                readUnsignedByte(headerBytes, 7));
    }

    public int getMessageType() {
        return messageType;
    }

    public int getStatus() {
        return status;
    }

    public int getLength() {
        return length;
    }

    public int getSpid() {
        return spid;
    }

    public int getSequenceNum() {
        return sequenceNum;
    }

    public int getWindow() {
        return window;
    }

    @Override
    public String toString() {
        return "TDSHeader{" +
                "messageType=" + messageType +
                ", status=" + status +
                ", length=" + length +
                ", spid=" + spid +
                ", sequenceNum=" + sequenceNum +
                ", window=" + window +
                '}';
    }

    public byte[] serialize() {
        final byte[] data = new byte[8];
        writeUnsignedByte(data, 0, messageType);
        writeUnsignedByte(data, 1, status);
        writeUnsignedShortBigEndian(data, 2, length);
        writeUnsignedShortBigEndian(data, 4, spid);
        writeUnsignedByte(data, 6, sequenceNum);
        writeUnsignedByte(data, 7, window);
        return data;
    }
}
