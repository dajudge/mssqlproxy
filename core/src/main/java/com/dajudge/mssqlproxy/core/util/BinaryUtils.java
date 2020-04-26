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
package com.dajudge.mssqlproxy.core.util;

public final class BinaryUtils {
    private BinaryUtils() {
    }

    public static int readUnsignedByte(final byte data[], final int offset) {
        return data[offset] & 0xFF;
    }

    public static void writeUnsignedByte(final byte data[], final int offset, final int value) {
        data[offset] = (byte) (value & 0xFF);
    }

    public static int readUnsignedShortBigEndian(final byte data[], final int offset) {
        return ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
    }

    public static void writeUnsignedShortBigEndian(final byte data[], final int offset, final int value) {
        data[offset] = (byte) ((value >> 8) & 0xFF);
        data[offset + 1] = (byte) ((value) & 0xFF);
    }

    public static short readShort(final byte[] data, final int offset) {
        return (short) ((data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8));
    }

    public static void writeShort(final byte[] data, final int offset, short value) {
        data[offset] = (byte) (value & 0xFF);
        data[offset + 1] = (byte) ((value >> 8) & 0xFF);
    }

    public static int readInt(final byte[] data, final int offset) {
        int b1 = ((int) data[offset] & 0xFF);
        int b2 = ((int) data[offset + 1] & 0xFF) << 8;
        int b3 = ((int) data[offset + 2] & 0xFF) << 16;
        int b4 = ((int) data[offset + 3] & 0xFF) << 24;
        return b4 | b3 | b2 | b1;
    }

    public static void writeInt(final byte[] data, final int offset, final int value) {
        data[offset] = (byte) (value & 0xFF);
        data[offset + 1] = (byte) ((value >> 8) & 0xFF);
        data[offset + 2] = (byte) ((value >> 16) & 0xFF);
        data[offset + 3] = (byte) ((value >> 24) & 0xFF);
    }

}
