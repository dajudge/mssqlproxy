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

import com.dajudge.mssqlproxy.core.client.responses.GenericResponse;
import com.dajudge.mssqlproxy.core.client.responses.ParsedResponse;
import com.dajudge.mssqlproxy.core.client.responses.PreloginResponse;
import com.dajudge.mssqlproxy.core.protocol.SqlServerMessage;
import com.dajudge.mssqlproxy.core.protocol.transport.EncryptionState;
import com.dajudge.mssqlproxy.core.protocol.transport.OptionContainer;
import com.dajudge.proxybase.Sink;
import io.netty.channel.ChannelFuture;

import static com.dajudge.mssqlproxy.core.protocol.transport.EncryptionLevel.NOT_SUPPORTED;

public class ResponseRelay implements Sink<ParsedResponse> {
    private static final byte ENCRYPTION_OPTION = (byte) 1;
    private final Sink<SqlServerMessage> upstreamSink;

    public ResponseRelay(final Sink<SqlServerMessage> upstreamSink) {
        this.upstreamSink = upstreamSink;
    }

    @Override
    public ChannelFuture close() {
        return upstreamSink.close();
    }

    @Override
    public void accept(final ParsedResponse parsedResponse) {
        parsedResponse.visit(new ParsedResponse.ResponseVisitorVisitor() {
            @Override
            public void onPreloginResponse(final PreloginResponse response) {
                // Rewrite the encryption state to force the client into plaintext communication
                final OptionContainer encryptionOption = response.getOptions().get(ENCRYPTION_OPTION);
                final byte[] encryptionData = encryptionOption.getOptionData();
                encryptionData[0] = new EncryptionState(NOT_SUPPORTED, false).serialize();
                encryptionOption.setOptionData(encryptionData);
                upstreamSink.accept(response.getMessage());
            }

            @Override
            public void onGenericResponse(final GenericResponse response) {
                upstreamSink.accept(response.getMessage());
            }
        });
    }
}
