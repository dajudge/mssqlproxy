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

import com.dajudge.mssqlproxy.core.client.requests.LoginRequest;
import com.dajudge.mssqlproxy.core.protocol.SqlServerMessage;
import com.dajudge.mssqlproxy.core.protocol.transport.Login7Message;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.r2dbc.mssql.client.ssl.SslState;

public class LoginHandler extends ChannelInboundHandlerAdapter {
    private final String username;
    private final String password;
    private LoginRequest loginRequest;

    public LoginHandler(final String username, final String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public void userEventTriggered(final ChannelHandlerContext ctx, final Object evt) throws Exception {
        if (evt instanceof LoginRequest) {
            this.loginRequest = (LoginRequest) evt;
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    @SuppressWarnings("PMD.NullAssignment") // we use the null reference as a state
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        if (msg instanceof SslState && loginRequest != null) {
            ctx.channel().writeAndFlush(rewriteLogin(loginRequest));
            loginRequest = null;
        } else {
            super.channelRead(ctx, msg);
        }
    }

    private SqlServerMessage rewriteLogin(final LoginRequest loginRequest) {
        try {
            final Login7Message login7Message = loginRequest.parsed();
            login7Message.setUsername(username);
            login7Message.setPassword(password);
            return loginRequest.getMessage().withNewPayload(login7Message.serialize());
        } finally {
            loginRequest.getMessage().release();
        }
    }
}
