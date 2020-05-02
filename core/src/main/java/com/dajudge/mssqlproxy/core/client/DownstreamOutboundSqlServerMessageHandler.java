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

import com.dajudge.mssqlproxy.core.protocol.SqlServerMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.r2dbc.mssql.message.tds.ContextualTdsFragment;

import static io.netty.buffer.Unpooled.copiedBuffer;
import static io.r2dbc.mssql.message.header.Header.decode;

public class DownstreamOutboundSqlServerMessageHandler extends ChannelOutboundHandlerAdapter {
    @Override
    public void write(
            final ChannelHandlerContext ctx,
            final Object msg,
            final ChannelPromise promise
    ) throws Exception {
        if (msg instanceof SqlServerMessage) {
            final SqlServerMessage sqlServerMessage = (SqlServerMessage) msg;
            try {
                ctx.writeAndFlush(new ContextualTdsFragment(
                        decode(sqlServerMessage.header()),
                        copiedBuffer(sqlServerMessage.payload())
                ));
            } finally {
                sqlServerMessage.release();
            }
        } else {
            super.write(ctx, msg, promise);
        }
    }
}
