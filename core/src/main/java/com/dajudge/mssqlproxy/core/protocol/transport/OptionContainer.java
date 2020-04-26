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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.dajudge.mssqlproxy.core.util.BinaryUtils.readUnsignedShortBigEndian;
import static java.util.stream.Collectors.toMap;

public class OptionContainer {
    private final byte optionToken;
    private final byte[] message;
    private final int optionOffset;
    private final int optionLength;

    public OptionContainer(
            final byte optionToken,
            final byte[] message,
            final int optionOffset,
            final int optionLength
    ) {
        this.optionToken = optionToken;
        this.message = message;
        this.optionOffset = optionOffset;
        this.optionLength = optionLength;
    }

    public static Map<Byte, OptionContainer> parseOptions(final byte[] payload) {
        final List<OptionContainer> options = new ArrayList<>();
        int index = 0;
        // SQLServerConnection.java L2745
        while (payload[index] != -1) { // SQLServerConnection.java L2806
            final byte optionToken = payload[index];
            final int optionOffset = readUnsignedShortBigEndian(payload, index + 1);
            final int optionLength = readUnsignedShortBigEndian(payload, index + 3);
            final OptionContainer option = new OptionContainer(optionToken, payload, optionOffset, optionLength);
            options.add(option);
            index += 5;
        }
        return options.stream().collect(toMap(it -> it.getOptionToken(), it -> it));
    }

    public byte getOptionToken() {
        return optionToken;
    }

    public byte[] getOptionData() {
        final byte[] ret = new byte[optionLength];
        System.arraycopy(message, optionOffset, ret, 0, optionLength);
        return ret;
    }

    public void setOptionData(final byte[] data) {
        System.arraycopy(data, 0, message, optionOffset, optionLength);
    }

    @Override
    public String toString() {
        return "OptionContainer{" +
                "optionToken=" + optionToken +
                '}';
    }
}
