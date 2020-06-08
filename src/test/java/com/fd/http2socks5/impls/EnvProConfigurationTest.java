package com.fd.http2socks5.impls;

import org.junit.Test;

import static org.junit.Assert.*;

public class EnvProConfigurationTest {

    @Test
    public void testGetFromEnvOrPro() {
        System.setProperty("key", "frompro");
        assertEquals("frompro", EnvProConfiguration.getFromEnvOrPro("key", "x"));
        assertEquals("x", EnvProConfiguration.getFromEnvOrPro("keynoexists", "x"));
        assertNotNull(EnvProConfiguration.getFromEnvOrPro("PATH", "PATH"));
    }

    @Test
    public void testGetPros() {
        System.setProperty("openNettyLoggingHandler", "1");
        System.setProperty("channelAutoRead", "1");
        System.setProperty("timeoutToSocks5ProxyServer", "1000");
        System.setProperty("connectionTimeoutToSocks5ProxyServer", "1000");
        System.setProperty("workerEventGroupNumber", "8");
        System.setProperty("maxConnectionBacklog", "10");
        System.setProperty("idleTimeoutForClient", "1000");
        System.setProperty("httpServerBindLocalAddress", "127.0.0.1");
        System.setProperty("httpServerBindLocalPort", "8090");
        EnvProConfiguration config = new EnvProConfiguration(null);
        assertTrue(config.openNettyLoggingHandler());
        assertTrue(config.channelAutoRead());
        assertEquals(1000, config.timeoutToSocks5ProxyServer());
        assertEquals(8, config.workerEventGroupNumber());
        assertEquals(10, config.maxConnectionBacklog());
        assertEquals(1000, config.idleTimeoutForClient());
        assertEquals("127.0.0.1", config.httpServerBindLocalAddress());
        assertEquals(8090, config.httpServerBindLocalPort());
        assertNull(config.socks5ProxyProvider());
    }
}
