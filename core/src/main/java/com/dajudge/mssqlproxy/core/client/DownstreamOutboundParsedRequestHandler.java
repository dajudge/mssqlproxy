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
import com.dajudge.mssqlproxy.core.client.requests.PreloginRequest;
import com.dajudge.mssqlproxy.core.protocol.transport.EncryptionState;
import com.dajudge.mssqlproxy.core.protocol.transport.OptionContainer;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.r2dbc.mssql.client.ssl.SslState;

import static com.dajudge.mssqlproxy.core.protocol.transport.EncryptionLevel.REQUIRED;

public class DownstreamOutboundParsedRequestHandler extends ChannelOutboundHandlerAdapter {
    private static final byte ENCRYPTION_OPTION = (byte) 1;

    @Override
    public void write(
            final ChannelHandlerContext ctx,
            final Object msg,
            final ChannelPromise promise
    ) throws Exception {
        if (msg instanceof LoginRequest) {
            ctx.pipeline().fireUserEventTriggered(msg);
            ctx.pipeline().fireUserEventTriggered(SslState.CONNECTION);
        } else if (msg instanceof PreloginRequest) {
            // Force server communication to be fully encrypted
            final PreloginRequest preloginRequest = (PreloginRequest) msg;
            final OptionContainer encryptionOption = preloginRequest.getOptions().get(ENCRYPTION_OPTION);
            final byte[] encryptionData = encryptionOption.getOptionData();
            encryptionData[0] = new EncryptionState(REQUIRED, false).serialize();
            encryptionOption.setOptionData(encryptionData);
            ctx.writeAndFlush(preloginRequest.getMessage());
        } else if (msg instanceof GenericRequest) {
            ctx.writeAndFlush(((GenericRequest) msg).getMessage());
        } else {
            super.write(ctx, msg, promise);
        }
    }
}
