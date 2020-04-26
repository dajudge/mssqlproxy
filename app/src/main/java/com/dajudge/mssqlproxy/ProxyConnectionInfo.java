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

public class ProxyConnectionInfo {
    private final Endpoint proxy;
    private final Endpoint server;
    private final String username;
    private final String password;

    public ProxyConnectionInfo(
            final Endpoint proxy,
            final Endpoint server,
            final String username,
            final String password
    ) {
        this.proxy = proxy;
        this.server = server;
        this.username = username;
        this.password = password;
    }

    public Endpoint getProxy() {
        return proxy;
    }

    public Endpoint getServer() {
        return server;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public String toString() {
        return "ProxyConnectionInfo{" +
                "proxy=" + proxy +
                ", server=" + server +
                ", username='" + username + '\'' +
                ", password='" + password.replaceAll(".", "*") + '\'' +
                '}';
    }
}
