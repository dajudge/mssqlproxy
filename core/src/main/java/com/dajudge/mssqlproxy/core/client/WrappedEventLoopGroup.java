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

import io.netty.channel.nio.NioEventLoopGroup;

public interface WrappedEventLoopGroup extends AutoCloseable {
    static WrappedEventLoopGroup wrap(final NioEventLoopGroup eventLoopGroup) {
        return new WrappedEventLoopGroup() {
            @Override
            public NioEventLoopGroup getGroup() {
                return eventLoopGroup;
            }

            @Override
            public void close() {
                try {
                    eventLoopGroup.shutdownGracefully().await(5000);
                } catch (final InterruptedException e) {
                    throw new RuntimeException("Failed to shutdown event loop group", e);
                }
            }
        };
    }

    NioEventLoopGroup getGroup();

    @Override
    void close();
}
