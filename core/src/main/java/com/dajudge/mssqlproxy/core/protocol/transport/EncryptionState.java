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

public class EncryptionState {
    private final EncryptionLevel level;
    private final boolean clientCert;

    public EncryptionState(final EncryptionLevel level, final boolean clientCert) {
        this.level = level;
        this.clientCert = clientCert;
    }

    public byte serialize() {
        final byte cert = (byte) (clientCert ? 0x80 : 0);
        switch (level) {
            case OFF:
                return cert;
            case ON:
                return (byte) (1 | cert);
            case NOT_SUPPORTED:
                return (byte) (2 | cert);
            case REQUIRED:
                return (byte) (3 | cert);
            default:
                throw new IllegalStateException();
        }
    }

    @Override
    public String toString() {
        return "EncryptionState{" +
                "level=" + level +
                ", clientCert=" + clientCert +
                '}';
    }
}
