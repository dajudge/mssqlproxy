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

import com.dajudge.mssqlproxy.core.ProxyApplication;
import com.dajudge.mssqlproxy.core.ProxyApplication.ProxyServer;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.event.Observes;
import java.util.Collection;

import static com.dajudge.mssqlproxy.ConnectionsListParser.parseProxyConnections;
import static java.util.stream.Collectors.toList;

public class Startup {
    private static final Logger LOG = LoggerFactory.getLogger(Startup.class);
    private Collection<ProxyServer> proxies;

    void onStart(@Observes StartupEvent ev) {
        final String proxyDefintions = System.getenv("MSSQLPROXY_PROXIES");
        if (proxyDefintions == null || proxyDefintions.trim().isEmpty()) {
            throw new IllegalArgumentException("$MSSQLPROXY_PROXIES is empty");
        }
        proxies = parseProxyConnections(proxyDefintions).stream()
                .map(this::createProxy)
                .collect(toList());
    }

    private ProxyServer createProxy(final ProxyConnectionInfo proxyConnectionInfo) {
        LOG.info("Creating proxy: {}", proxyConnectionInfo);
        return ProxyApplication.createProxy(
                proxyConnectionInfo.getProxy(),
                proxyConnectionInfo.getServer(),
                proxyConnectionInfo.getUsername(),
                proxyConnectionInfo.getPassword()
        );
    }

    void onStop(@Observes ShutdownEvent ev) {
        proxies.forEach(ProxyServer::close);
    }
}
