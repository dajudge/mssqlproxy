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

import com.dajudge.mssqlproxy.core.ProxyApplication.ProxyServer;
import com.dajudge.proxybase.config.Endpoint;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.dajudge.mssqlproxy.core.ProxyApplication.createProxy;
import static com.dajudge.mssqlproxy.core.SqlServerContainer.SA_PASSWORD;
import static com.dajudge.mssqlproxy.core.SqlServerContainer.SA_USERNAME;
import static java.lang.String.join;
import static java.sql.DriverManager.getConnection;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

public class ProxyTest {

    @Test
    public void can_establish_connection() {
        withConnection(c -> {
            try {
                final String dbName = c.getMetaData().getDatabaseProductName();
                assertEquals("Microsoft SQL Server", dbName);
                System.out.println("DBNAME: " + dbName);
            } catch (final SQLException e) {
                throw new AssertionError(e);
            }
        });
    }


    private void withConnection(final Consumer<Connection> connConsumer) {
        withProxy(host -> {
            try (final Connection connection = getConnection(jdbcUrl(host, "master", "wrong", "wrong"))) {
                connConsumer.accept(connection);
            } catch (final SQLException e) {
                throw new RuntimeException("Failed to connect to SQL server", e);
            }
        });
    }

    private void withProxy(final Consumer<String> proxyConsumer) {
        withServer((host, port) -> {
            try (final ProxyServer proxy = createProxy(
                    new Endpoint("127.0.0.1", 0),
                    new Endpoint(host, port),
                    SA_USERNAME,
                    SA_PASSWORD
            )) {
                proxyConsumer.accept("127.0.0.1:" + proxy.port());
            }
        });
    }

    public static void withServer(final BiConsumer<String, Integer> endpointConsumer) {
        try (final SqlServerContainer c = new SqlServerContainer()) {
            c.start();
            endpointConsumer.accept(c.getContainerIpAddress(), c.getMappedPort(1433));
        }
    }

    @NotNull
    public static String jdbcUrl(
            final String host,
            final String database,
            final String username,
            final String password
    ) {
        return "jdbc:sqlserver://" + host + ";" + join(";", asList(
                "databaseName=" + database,
                "user=" + username,
                "password=" + password
        ));
    }
}
