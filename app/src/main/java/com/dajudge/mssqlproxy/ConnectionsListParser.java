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
package com.dajudge.mssqlproxy;

import com.dajudge.proxybase.config.Endpoint;

import java.util.Collection;
import java.util.stream.Stream;

import static java.lang.Integer.parseUnsignedInt;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

public class ConnectionsListParser {
    private static final String EXPECTED = "<bindAddress>:<bindPort>=<username>:<password>@<serverAddress>:<serverPort>";

    public static Collection<ProxyConnectionInfo> parseProxyConnections(final String connections) {
        return Stream.of(connections.split(","))
                .map(ConnectionsListParser::parseConnection)
                .collect(toList());
    }

    private static ProxyConnectionInfo parseConnection(final String conn) {
        if (!conn.contains("=")) {
            throw illegalProxySpecification(conn);
        }
        final String[] connParts = conn.split("=", 2);
        final String proxyEndpoint = connParts[0];
        final String serverEndpointWithCredentials = connParts[1];
        if (!serverEndpointWithCredentials.contains("@")) {
            throw illegalProxySpecification(conn);
        }
        final String[] serverEndpointWithCredentialsParts = serverEndpointWithCredentials.split("@", 2);
        final String credentials = serverEndpointWithCredentialsParts[0];
        final String serverEndpoint = serverEndpointWithCredentialsParts[1];
        if (!credentials.contains(":")) {
            throw illegalProxySpecification(conn);
        }
        final String[] credentialsParts = credentials.split(":", 2);
        final String username = credentialsParts[0];
        final String password = credentialsParts[1];
        return new ProxyConnectionInfo(
                parseEndpoint(proxyEndpoint),
                parseEndpoint(serverEndpoint),
                username,
                password
        );
    }

    private static Endpoint parseEndpoint(final String endpoint) {
        if (!endpoint.contains(":")) {
            throw new IllegalArgumentException(format(
                    "Invalid endpoint specification: %s. Expected format: %s",
                    endpoint,
                    EXPECTED
            ));
        }
        final String[] endpointParts = endpoint.split(":", 2);
        return new Endpoint(
                endpointParts[0],
                parseUnsignedInt(endpointParts[1])
        );
    }

    private static IllegalArgumentException illegalProxySpecification(final String conn) {
        return new IllegalArgumentException(format(
                "Invalid proxy specification: %s. Expected format: %s",
                conn,
                EXPECTED
        ));
    }
}
