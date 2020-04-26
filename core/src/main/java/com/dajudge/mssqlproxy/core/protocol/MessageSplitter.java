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
package com.dajudge.mssqlproxy.core.protocol;

import com.dajudge.proxybase.Sink;
import com.dajudge.proxybase.message.AbstractFixedSizeHeaderMessageSplitter;
import io.netty.buffer.ByteBuf;

public class MessageSplitter extends AbstractFixedSizeHeaderMessageSplitter<SqlServerMessage> {

    public MessageSplitter(final Sink<SqlServerMessage> requestSink) {
        super(requestSink);
    }

    @Override
    protected SqlServerMessage createEmptyRequest() {
        return new SqlServerMessage();
    }

    @Override
    protected boolean isComplete(final SqlServerMessage sqlServerMessage) {
        return sqlServerMessage.isComplete();
    }

    @Override
    protected void append(final SqlServerMessage sqlServerMessage, final ByteBuf byteBuf) {
        sqlServerMessage.append(byteBuf);
    }
}
