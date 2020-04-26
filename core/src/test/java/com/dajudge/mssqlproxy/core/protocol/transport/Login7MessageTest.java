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

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertArrayEquals;

public class Login7MessageTest {
    @Test
    public void reencodes_properly() {
        payloads().forEach(this::assertReencodesProperly);
    }

    @NotNull
    private List<byte[]> payloads() {
        return asList(readTextResource("payloads/login7.dat").split("\n")).stream()
                .map(String::trim)
                .filter(it -> !it.startsWith("#"))
                .filter(it -> it.length() > 0)
                .map(Base64.getDecoder()::decode)
                .collect(Collectors.toList());
    }

    @Test
    public void replace_password_works() {
        final Login7Message login = new Login7Message(payloads().get(1));
        login.setUsername("sa");
        login.setPassword("yourStrong(!)Password");
        final byte[] rewritten = login.serialize();
        assertArrayEquals(payloads().get(0), rewritten);
    }

    private void assertReencodesProperly(final byte[] expected) {
        final byte[] result = new Login7Message(expected).serialize();
        assertArrayEquals(expected, result);
    }

    private static String readTextResource(final String resname) {
        return new String(readResource(resname), StandardCharsets.UTF_8);
    }

    @NotNull
    private static byte[] readResource(final String resname) {
        try (final InputStream is = Login7MessageTest.class.getClassLoader().getResourceAsStream(resname)) {
            final byte[] buffer = new byte[1024];
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            int read;
            while ((read = is.read(buffer)) > 0) {
                bos.write(buffer, 0, read);
            }
            return bos.toByteArray();
        } catch (final IOException e) {
            throw new RuntimeException("Failed to read resource", e);
        }
    }
}
