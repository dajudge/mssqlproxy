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

import com.dajudge.mssqlproxy.core.client.DownstreamPipelineCustomizer;
import com.dajudge.mssqlproxy.core.client.RequestParser;
import com.dajudge.mssqlproxy.core.client.ResponseParser;
import com.dajudge.mssqlproxy.core.client.ResponseRelay;
import com.dajudge.mssqlproxy.core.client.requests.ParsedRequest;
import com.dajudge.mssqlproxy.core.protocol.MessageJoiner;
import com.dajudge.mssqlproxy.core.protocol.MessageSplitter;
import com.dajudge.proxybase.*;
import com.dajudge.proxybase.ca.NullCertificateAuthority;
import com.dajudge.proxybase.config.DownstreamSslConfig;
import com.dajudge.proxybase.config.Endpoint;
import com.dajudge.proxybase.config.UpstreamSslConfig;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelPipeline;

import java.util.Collection;

import static java.util.stream.Collectors.toList;

public class MssqlProxyApplication extends ProxyApplication<ByteBuf, ByteBuf, ByteBuf, ParsedRequest> {
    private final Collection<ProxyConfig> configs;

    public MssqlProxyApplication(final Collection<ProxyConfig> configs) {
        super(UpstreamSslConfig.NO_SSL, DownstreamSslConfig.NO_SSL, NullCertificateAuthority.INSTANCE);
        this.configs = configs;
    }

    @Override
    protected Collection<ProxyChannel<ByteBuf, ByteBuf, ByteBuf, ParsedRequest>> initializeProxyChannels(
            final ProxyChannelFactory<ByteBuf, ByteBuf, ByteBuf, ParsedRequest> proxyChannelFactory
    ) {
        return configs.stream()
                .map(config -> createChannel(proxyChannelFactory, config))
                .peek(ProxyChannel::start)
                .collect(toList());
    }

    private ProxyChannel<ByteBuf, ByteBuf, ByteBuf, ParsedRequest> createChannel(
            final ProxyChannelFactory<ByteBuf, ByteBuf, ByteBuf, ParsedRequest> proxyChannelFactory,
            final ProxyConfig config
    ) {
        return proxyChannelFactory.createProxyChannel(
                config.upstream,
                config.downstream,
                () -> createProxyContext(config)
        );
    }

    private ProxyContextFactory.ProxyContext<ByteBuf, ByteBuf, ByteBuf, ParsedRequest> createProxyContext(
            final ProxyConfig config
    ) {
        return new ProxyContextFactory.ProxyContext<ByteBuf, ByteBuf, ByteBuf, ParsedRequest>() {
            @Override
            public void customizeDownstreamPipeline(final ChannelPipeline channelPipeline) {
                new DownstreamPipelineCustomizer(config.username, config.password)
                        .customize(channelPipeline);
            }

            @Override
            public void customizeUpstreamPipeline(final ChannelPipeline channelPipeline) {

            }

            @Override
            public Sink<ByteBuf> downstreamFilter(final Sink<ParsedRequest> sink) {
                return new MessageSplitter(new RequestParser(sink));
            }

            @Override
            public Sink<ByteBuf> upstreamFilter(final Sink<ByteBuf> sink) {
                return new MessageSplitter(new ResponseParser(new ResponseRelay(new MessageJoiner(sink))));
            }
        };
    }

    public static class ProxyConfig {
        private final Endpoint upstream;
        private final Endpoint downstream;
        private final String username;
        private final String password;

        public ProxyConfig(
                final Endpoint upstream,
                final Endpoint downstream,
                final String username,
                final String password
        ) {
            this.upstream = upstream;
            this.downstream = downstream;
            this.username = username;
            this.password = password;
        }
    }
}
