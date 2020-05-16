package com.dajudge.mssqlproxy.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;

import java.io.IOException;

import static java.lang.String.format;
import static org.junit.Assert.assertTrue;

public class CmdlineClientContainer extends GenericContainer<CmdlineClientContainer> {
    private static final Logger LOG = LoggerFactory.getLogger(CmdlineClientContainer.class);

    public CmdlineClientContainer() {
        super("mcr.microsoft.com/mssql-tools");
        withCommand("sleep", "infinity");
        withNetworkMode("host");
    }

    public void assertCanConnect(
            final String hostname,
            final int port,
            final String username,
            final String password
    ){
        final String command = format(
                "/opt/mssql-tools/bin/sqlcmd -S tcp:%s,%s -U %s -P %s -Q \"SELECT @@version\"",
                hostname,
                port,
                username,
                password
        );
        try {
            final String stdout = execInContainer("sh", "-c", command).getStdout();
            assertTrue(stdout, stdout.contains("Microsoft SQL Server"));
        } catch (final IOException | InterruptedException e) {
            throw new AssertionError(e);
        }
    }
}
