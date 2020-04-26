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
import com.dajudge.mssqlproxy.core.client.requests.ParsedRequest.ParsedRequestVisitor;
import com.dajudge.mssqlproxy.core.client.requests.PreloginRequest;
import com.dajudge.mssqlproxy.core.client.responses.ParsedResponse;
import com.dajudge.mssqlproxy.core.protocol.MessageSplitter;
import com.dajudge.mssqlproxy.core.protocol.SqlServerMessage;
import com.dajudge.mssqlproxy.core.protocol.transport.EncryptionState;
import com.dajudge.mssqlproxy.core.protocol.transport.Login7Message;
import com.dajudge.mssqlproxy.core.protocol.transport.OptionContainer;
import com.dajudge.proxybase.Sink;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.r2dbc.mssql.client.ConnectionContext;
import io.r2dbc.mssql.client.ReactorNettyClient;
import io.r2dbc.mssql.client.TdsEncoder;
import io.r2dbc.mssql.client.ssl.SslConfiguration;
import io.r2dbc.mssql.client.ssl.SslState;
import io.r2dbc.mssql.client.ssl.TdsSslHandler;
import io.r2dbc.mssql.message.header.PacketIdProvider;
import io.r2dbc.mssql.message.tds.ContextualTdsFragment;

import java.util.UUID;

import static com.dajudge.mssqlproxy.core.protocol.transport.EncryptionLevel.REQUIRED;
import static io.netty.buffer.Unpooled.copiedBuffer;
import static io.netty.channel.ChannelOption.SO_KEEPALIVE;
import static io.r2dbc.mssql.message.header.Header.decode;

public class MssqlProxyClient implements Sink<ParsedRequest> {
    private static final byte ENCRYPTION_OPTION = (byte) 1;
    private static final String UPSTREAM_HANDLER_NAME = MssqlProxyClient.class.getName() + "/upstream";
    private static final String LOGIN_HANDLER_NAME = MssqlProxyClient.class.getSimpleName() + "/login";
    private final Channel channel;
    private final String username;
    private final String password;

    public MssqlProxyClient(final Channel channel, final String username, final String password) {
        this.channel = channel;
        this.username = username;
        this.password = password;
    }

    public static MssqlProxyClient connect(
            final String host,
            final Integer port,
            final NioEventLoopGroup serverWorkerGroup,
            final Sink<ParsedResponse> replySink,
            final String username,
            final String password
    ) {
        try {
            final Channel channel = new Bootstrap()
                    .group(serverWorkerGroup)
                    .channel(NioSocketChannel.class)
                    .option(SO_KEEPALIVE, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            initServerChannel(ch.pipeline(), UUID.randomUUID().toString(), replySink);
                        }
                    })
                    .connect(host, port).sync().channel();
            return new MssqlProxyClient(channel, username, password);
        } catch (final Exception e) {
            throw new RuntimeException("Failed to establish server connection", e);
        }
    }

    private static void initServerChannel(
            final ChannelPipeline pipeline,
            final String channelId,
            final Sink<ParsedResponse> replySink
    ) {
        final ConnectionContext connectionContext = new ConnectionContext(null, null);
        final PacketIdProvider packetIdProvider = PacketIdProvider.atomic();
        final SslConfiguration configuration = new SslConfiguration() {
            @Override
            public boolean isSslEnabled() {
                return false;
            }

            @Override
            public String getHostNameInCertificate() {
                return null;
            }
        };

        final TdsEncoder tdsEncoder = new TdsEncoder(packetIdProvider);
        pipeline.addFirst(tdsEncoder.getClass().getName(), tdsEncoder);
        final TdsSslHandler handler = new TdsSslHandler(packetIdProvider, configuration, connectionContext.withChannelId(channelId));
        pipeline.addAfter(tdsEncoder.getClass().getName(), handler.getClass().getName(), handler);
        pipeline.addLast(UPSTREAM_HANDLER_NAME, createUpstreamSink(replySink));

        final InternalLogger logger = InternalLoggerFactory.getInstance(ReactorNettyClient.class);
        if (logger.isTraceEnabled()) {
            pipeline.addFirst(
                    LoggingHandler.class.getSimpleName(),
                    new LoggingHandler(MssqlProxyClient.class, LogLevel.TRACE)
            );
        }
    }

    private static ReceiveHandler createUpstreamSink(final Sink<ParsedResponse> replySink) {
        return new ReceiveHandler(new MessageSplitter(new ResponseParser(replySink)));
    }

    private void send(final SqlServerMessage msg) {
        try {
            channel.write(new ContextualTdsFragment(decode(msg.header()), copiedBuffer(msg.payload())));
            channel.flush();
        } finally {
            msg.release();
        }
    }

    @Override
    public ChannelFuture close() {
        return channel.close();
    }

    @Override
    public void accept(final ParsedRequest parsedRequest) {
        parsedRequest.visit(new ParsedRequestVisitor() {

            @Override
            public void onGenericRequest(final GenericRequest genericRequest) {
                send(genericRequest.getMessage());
            }

            @Override
            public void onPreloginRequest(final PreloginRequest preloginRequest) {
                // Force server communication to be fully encrypted
                final OptionContainer encryptionOption = preloginRequest.getOptions().get(ENCRYPTION_OPTION);
                final byte[] encryptionData = encryptionOption.getOptionData();
                encryptionData[0] = new EncryptionState(REQUIRED, false).serialize();
                encryptionOption.setOptionData(encryptionData);
                send(preloginRequest.getMessage());
            }

            @Override
            public void onLoginRequest(final LoginRequest loginRequest) {
                channel.pipeline().addBefore(
                        UPSTREAM_HANDLER_NAME,
                        LOGIN_HANDLER_NAME,
                        sendLoginWhenSslNegotiated(loginRequest)
                );
                channel.pipeline().fireUserEventTriggered(SslState.CONNECTION);
            }
        });
    }

    private ChannelInboundHandlerAdapter sendLoginWhenSslNegotiated(final LoginRequest loginRequest) {
        return new ChannelInboundHandlerAdapter() {
            @Override
            public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
                if (msg instanceof SslState) {
                    send(rewriteLogin(loginRequest));
                    channel.pipeline().remove(LOGIN_HANDLER_NAME);
                    return;
                }
                ctx.write(msg);
            }
        };
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
