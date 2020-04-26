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

import com.dajudge.proxybase.Sink;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReceiveHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(ReceiveHandler.class);
    private final Sink<ByteBuf> sink;

    public ReceiveHandler(final Sink<ByteBuf> sink) {
        this.sink = sink;
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
        sink.accept((ByteBuf) msg);
    }


    @Override
    public void channelUnregistered(final ChannelHandlerContext ctx) {
        sink.close().addListener(future -> LOG.info("Upstream sink closed: {}", future.isSuccess()));
    }
}
