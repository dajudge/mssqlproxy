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

public final class PasswordUtil {
    private PasswordUtil() {
    }

    public static byte[] encryptPassword(final String pwd) {
        final byte[] data = new byte[pwd.length() * 2];
        for (int i = 0; i < pwd.length(); i++) {
            final int c = pwd.charAt(i) ^ 0x5a5a;
            final int j = (c & 0xf) << 4 | (c & 0xf0) >> 4 | (c & 0xf00) << 4 | (c & 0xf000) >> 4;
            data[(i * 2) + 1] = (byte) ((j & 0xFF00) >> 8);
            data[(i * 2)] = (byte) ((j & 0x00FF));
        }
        return data;
    }

    public static String decryptPassword(final byte[] bytes) {
        final char[] pwd = new char[bytes.length / 2];
        for (int i = 0; i < bytes.length / 2; i++) {
            final int j = (((bytes[(i * 2) + 1] << 8) & 0xFF00) | bytes[(i * 2)] & 0xFF) & 0xFFFF;
            final int c = (j & 0xf0) >> 4 | (j & 0x0f) << 4 | (j & 0xf000) >> 4 | (j & 0x0f00) << 4;
            pwd[i] = (char) (c ^ 0x5a5a);
        }
        return new String(pwd);
    }
}
