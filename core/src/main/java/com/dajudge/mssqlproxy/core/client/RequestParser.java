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

import com.dajudge.mssqlproxy.core.client.requests.GenericRequest;
import com.dajudge.mssqlproxy.core.client.requests.LoginRequest;
import com.dajudge.mssqlproxy.core.client.requests.ParsedRequest;
import com.dajudge.mssqlproxy.core.client.requests.PreloginRequest;
import com.dajudge.mssqlproxy.core.protocol.SqlServerMessage;
import com.dajudge.proxybase.Sink;
import io.netty.channel.ChannelFuture;

public class RequestParser implements Sink<SqlServerMessage> {
    private static final byte PRE_LOGIN_REQUEST = 18;
    private static final byte LOGIN_REQUEST = 16;
    private final Sink<ParsedRequest> requestSink;

    public RequestParser(final Sink<ParsedRequest> requestSink) {
        this.requestSink = requestSink;
    }

    @Override
    public ChannelFuture close() {
        return requestSink.close();
    }

    @Override
    public void accept(final SqlServerMessage msg) {
        if (msg.parsedHeader().getMessageType() == PRE_LOGIN_REQUEST) {
            System.out.println("PRELOGIN REQUEST");
            requestSink.accept(new PreloginRequest(msg));
        } else if (msg.parsedHeader().getMessageType() == LOGIN_REQUEST) {
            System.out.println("LOGIN REQUEST");
            requestSink.accept(new LoginRequest(msg));
        } else {
            System.out.println("GENERIC REQUEST");
            requestSink.accept(new GenericRequest(msg));
        }
    }
}
