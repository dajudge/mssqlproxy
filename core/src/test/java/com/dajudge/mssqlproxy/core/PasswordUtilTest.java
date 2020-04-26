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
package com.dajudge.mssqlproxy.core;

import org.junit.Test;

import java.util.UUID;

import static com.dajudge.mssqlproxy.core.util.PasswordUtil.decryptPassword;
import static com.dajudge.mssqlproxy.core.util.PasswordUtil.encryptPassword;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

public class PasswordUtilTest {
    @Test
    public void decrypts_properly() {
        asList(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString()
        ).forEach(pwd -> assertEquals(pwd, decryptPassword(encryptPassword(pwd))));
    }
}
