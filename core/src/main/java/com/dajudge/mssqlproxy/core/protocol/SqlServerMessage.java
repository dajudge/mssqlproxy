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
package com.dajudge.mssqlproxy.core.protocol;

import com.dajudge.mssqlproxy.core.protocol.transport.TDSHeader;
import com.dajudge.proxybase.message.AbstractFixedSizeHeaderMessage;
import io.netty.buffer.ByteBuf;

import static com.dajudge.mssqlproxy.core.protocol.transport.TDSHeader.TDS_HEADER_SIZE;
import static io.netty.buffer.Unpooled.copiedBuffer;

public class SqlServerMessage extends AbstractFixedSizeHeaderMessage {

    protected SqlServerMessage() {
        super(TDS_HEADER_SIZE);
    }

    private SqlServerMessage(final TDSHeader oldHeader, final byte[] payload) {
        super(header(oldHeader, payload.length), payload(payload));
    }

    private static ByteBuf payload(final byte[] payload) {
        return copiedBuffer(payload);
    }

    private static ByteBuf header(
            final TDSHeader oldHeader,
            final int payloadLength
    ) {
        final TDSHeader header = new TDSHeader(
                oldHeader.getMessageType(),
                oldHeader.getStatus(),
                payloadLength + TDS_HEADER_SIZE,
                oldHeader.getSpid(),
                oldHeader.getSequenceNum(),
                oldHeader.getWindow());
        return copiedBuffer(header.serialize());
    }

    public SqlServerMessage withNewPayload(final byte[] newPayload) {
        return new SqlServerMessage(parsedHeader(), newPayload);
    }

    public TDSHeader parsedHeader() {
        return new TDSHeader(header().array());
    }

    @Override
    protected int getPayloadLength(final ByteBuf byteBuf) {
        return new TDSHeader(byteBuf.array()).getLength() - TDS_HEADER_SIZE;
    }

}
