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

public final class EncodingUtil {
    private EncodingUtil() {
    }

    public static byte[] toUCS16(String s) {
        final byte[] data = new byte[s.length() * 2];
        for (int i = 0; i < s.length(); i++) {
            final int c = s.charAt(i);
            data[i * 2] = (byte) (c & 0xFF);
            data[(i * 2) + 1] = (byte) ((c >> 8) & 0xFF);
        }
        return data;
    }

    public static String fromUCS16(final byte[] data) {
        final char[] string = new char[data.length / 2];
        for (int i = 0; i < data.length / 2; i++) {
            final byte b1 = data[i * 2];
            final byte b2 = data[(i * 2) + 1];
            final int c = (b1 | b2 << 8) & 0xFFFF;
            string[i] = (char) c;
        }
        return new String(string);
    }
}
