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
package com.dajudge.mssqlproxy.core.client.responses;

import com.dajudge.mssqlproxy.core.protocol.SqlServerMessage;

public abstract class ParsedResponse {
    private final SqlServerMessage msg;

    protected ParsedResponse(final SqlServerMessage msg) {
        this.msg = msg;
    }

    public SqlServerMessage getMessage() {
        return msg;
    }

    public abstract void visit(ResponseVisitorVisitor visitor);

    public interface ResponseVisitorVisitor {
        void onPreloginResponse(PreloginResponse response);

        void onGenericResponse(GenericResponse response);
    }
}
