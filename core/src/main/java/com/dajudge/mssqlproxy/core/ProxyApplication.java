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

import com.dajudge.mssqlproxy.core.client.MssqlProxyClient;
import com.dajudge.mssqlproxy.core.client.RequestParser;
import com.dajudge.mssqlproxy.core.client.ResponseRelay;
import com.dajudge.mssqlproxy.core.client.WrappedEventLoopGroup;
import com.dajudge.mssqlproxy.core.client.responses.ParsedResponse;
import com.dajudge.mssqlproxy.core.protocol.MessageJoiner;
import com.dajudge.mssqlproxy.core.protocol.MessageSplitter;
import com.dajudge.proxybase.Sink;
import com.dajudge.proxybase.config.Endpoint;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.function.Function;

import static com.dajudge.mssqlproxy.core.client.MssqlProxyClient.connect;

public class ProxyApplication {
    private static final Logger LOG = LoggerFactory.getLogger(ProxyApplication.class);

    public static ProxyServer createProxy(
            final Endpoint proxy,
            final Endpoint server,
            final String username,
            final String password
    ) {
        final WrappedEventLoopGroup downstreamWorkerGroup = WrappedEventLoopGroup.wrap(new NioEventLoopGroup());
        final WrappedEventLoopGroup upstreamWorkerGroup = WrappedEventLoopGroup.wrap(new NioEventLoopGroup());
        final WrappedEventLoopGroup serverWorkerGroup = WrappedEventLoopGroup.wrap(new NioEventLoopGroup());
        final Function<Sink<ByteBuf>, Sink<ByteBuf>> downstreamSinkFactory = upstream ->
                createDownstreamSink(
                        server.getHost(),
                        server.getPort(),
                        username,
                        password,
                        downstreamWorkerGroup.getGroup(),
                        upstream
                );
        final ProxyServer proxyServer = createProxy(
                proxy.getHost(),
                proxy.getPort(),
                serverWorkerGroup.getGroup(),
                upstreamWorkerGroup.getGroup(),
                downstreamSinkFactory
        );
        LOG.info("Proxying {} at {}", server, proxy);
        return new ProxyServer() {
            @Override
            public int port() {
                return proxyServer.port();
            }

            @Override
            public void close() {
                proxyServer.close();
                upstreamWorkerGroup.close();
                downstreamWorkerGroup.close();
                serverWorkerGroup.close();
            }
        };
    }

    private static ProxyServer createProxy(
            final String bindAddress,
            final int bindPort,
            final NioEventLoopGroup serverWorkerGroup,
            final NioEventLoopGroup upstreamWorkerGroup,
            final Function<Sink<ByteBuf>, Sink<ByteBuf>> downstreamSinkFactory
    ) {
        try {
            final Channel serverChannel = new ServerBootstrap()
                    .group(serverWorkerGroup, upstreamWorkerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(createProxyChannelInitializer(downstreamSinkFactory))
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .bind(bindAddress, bindPort)
                    .sync()
                    .channel();
            return new ProxyServer() {
                @Override
                public int port() {
                    return ((InetSocketAddress) serverChannel.localAddress()).getPort();
                }

                @Override
                public void close() {
                    try {
                        LOG.info("Closing proxy channel listening on {}:{}", bindAddress, port());
                        serverChannel.close().await(5000);
                    } catch (final InterruptedException e) {
                        throw new RuntimeException("Failed to close upstream server channel", e);
                    }
                }
            };
        } catch (final InterruptedException e) {
            throw new RuntimeException("Failed to create proxy server channel", e);
        }
    }

    private static ChannelInitializer<SocketChannel> createProxyChannelInitializer(
            final Function<Sink<ByteBuf>, Sink<ByteBuf>> downstreamSinkFactory
    ) {
        return new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(final SocketChannel ch) {
                LOG.info("Incoming connection on {} from {}",
                        ch.localAddress(),
                        ch.remoteAddress()
                );
                final Sink<ByteBuf> downstreamSink = downstreamSinkFactory.apply(new Sink<ByteBuf>() {
                    @Override
                    public ChannelFuture close() {
                        return ch.close();
                    }

                    @Override
                    public void accept(final ByteBuf byteBuf) {
                        ch.write(byteBuf);
                        ch.flush();
                    }
                });
                ch.pipeline().addFirst(new ChannelInboundHandlerAdapter() {
                    @Override
                    public void channelUnregistered(final ChannelHandlerContext ctx) {
                        downstreamSink.close();
                    }

                    @Override
                    public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
                        downstreamSink.accept((ByteBuf) msg);
                    }
                });
            }
        };
    }

    private static Sink<ByteBuf> createDownstreamSink(
            final String host,
            final int port,
            final String username,
            final String password,
            final NioEventLoopGroup downstreamWorkerGroup,
            final Sink<ByteBuf> upstream
    ) {
        final Sink<ParsedResponse> sink = new ResponseRelay(new MessageJoiner(upstream));
        final MssqlProxyClient client = connect(host, port, downstreamWorkerGroup, sink, username, password);
        return new MessageSplitter(new RequestParser(client));
    }

    public interface ProxyServer extends AutoCloseable {
        int port();

        @Override
        void close();
    }
}
