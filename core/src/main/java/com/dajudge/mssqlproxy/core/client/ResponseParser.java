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
package com.dajudge.mssqlproxy.core.client;

import com.dajudge.mssqlproxy.core.client.responses.GenericResponse;
import com.dajudge.mssqlproxy.core.client.responses.ParsedResponse;
import com.dajudge.mssqlproxy.core.client.responses.PreloginResponse;
import com.dajudge.mssqlproxy.core.protocol.SqlServerMessage;
import com.dajudge.proxybase.Sink;
import io.netty.channel.ChannelFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResponseParser implements Sink<SqlServerMessage> {
    private static final Logger LOG = LoggerFactory.getLogger(ResponseParser.class);
    private final Sink<ParsedResponse> replySink;
    private boolean firstPreloginSeen = false;

    public ResponseParser(final Sink<ParsedResponse> replySink) {
        this.replySink = replySink;
    }

    @Override
    public ChannelFuture close() {
        return replySink.close();
    }

    @Override
    public void accept(final SqlServerMessage sqlServerMessage) {
        if (sqlServerMessage.parsedHeader().getMessageType() == 4 && !firstPreloginSeen) { // Prelogin response
            LOG.debug("PRELOGIN RESPONSE");
            replySink.accept(new PreloginResponse(sqlServerMessage));
            firstPreloginSeen = true;
        } else {
            LOG.debug("GENERIC RESPONSE");
            replySink.accept(new GenericResponse(sqlServerMessage));
        }
    }
}
