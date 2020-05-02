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

import io.netty.channel.ChannelPipeline;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.r2dbc.mssql.client.ConnectionContext;
import io.r2dbc.mssql.client.ReactorNettyClient;
import io.r2dbc.mssql.client.TdsEncoder;
import io.r2dbc.mssql.client.ssl.SslConfiguration;
import io.r2dbc.mssql.client.ssl.TdsSslHandler;
import io.r2dbc.mssql.message.header.PacketIdProvider;

import java.util.UUID;

import static com.dajudge.proxybase.ProxyChannel.DOWNSTREAM_INBOUND_HANDLER;

public class DownstreamPipelineCustomizer {
    private final String username;
    private final String password;

    public DownstreamPipelineCustomizer(final String username, final String password) {
        this.username = username;
        this.password = password;
    }

    public void customize(final ChannelPipeline pipeline) {
        addTdsProtocolHandlers(pipeline);
        addProxyHandlers(pipeline);
        addLoggingHandler(pipeline);
    }

    private void addLoggingHandler(final ChannelPipeline pipeline) {
        final InternalLogger logger = InternalLoggerFactory.getInstance(ReactorNettyClient.class);
        if (logger.isTraceEnabled()) {
            pipeline.addFirst(
                    LoggingHandler.class.getSimpleName(),
                    new LoggingHandler(DownstreamPipelineCustomizer.class, LogLevel.TRACE)
            );
        }
    }

    private void addProxyHandlers(final ChannelPipeline pipeline) {
        pipeline.addBefore(
                DOWNSTREAM_INBOUND_HANDLER,
                LoginHandler.class.getName(),
                new LoginHandler(username, password)
        );
        pipeline.addBefore(
                DOWNSTREAM_INBOUND_HANDLER,
                DownstreamOutboundSqlServerMessageHandler.class.getName(),
                new DownstreamOutboundSqlServerMessageHandler()
        );
        pipeline.addBefore(
                DOWNSTREAM_INBOUND_HANDLER,
                DownstreamOutboundParsedRequestHandler.class.getName(),
                new DownstreamOutboundParsedRequestHandler()
        );
    }

    private void addTdsProtocolHandlers(final ChannelPipeline pipeline) {
        final String channelId = UUID.randomUUID().toString();
        final ConnectionContext connectionContext = new ConnectionContext(null, null);
        final PacketIdProvider packetIdProvider = PacketIdProvider.atomic();
        final SslConfiguration configuration = new SslConfiguration() {
            @Override
            public boolean isSslEnabled() {
                return false;
            }

            @Override
            public String getHostNameInCertificate() {
                return "";
            }
        };

        final TdsEncoder tdsEncoder = new TdsEncoder(packetIdProvider);
        pipeline.addFirst(tdsEncoder.getClass().getName(), tdsEncoder);
        final TdsSslHandler handler = new TdsSslHandler(
                packetIdProvider,
                configuration,
                connectionContext.withChannelId(channelId)
        );
        pipeline.addAfter(tdsEncoder.getClass().getName(), handler.getClass().getName(), handler);
    }
}
