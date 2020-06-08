package com.fd.http2socks5.utils;

import com.google.common.net.HostAndPort;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import org.junit.Test;

import static org.junit.Assert.*;

public class ProxyUtilsTest {

    @Test
    public void testParseUri() {
        assertEquals("/", ProxyUtils.parseUri("https://www.google.com/"));
        assertEquals("/", ProxyUtils.parseUri("https://www.google.com"));
        assertEquals("/?abc=1", ProxyUtils.parseUri("/?abc=1"));
        assertEquals("//www.google.com/?a=123", ProxyUtils.parseUri("//www.google.com/?a=123"));
        try {
            ProxyUtils.parseUri(null);
        } catch (Exception error) {
            assertTrue(error instanceof NullPointerException);
        }
    }

    @Test
    public void testParseHostAndPort() {
        assertEquals("www.google.com", ProxyUtils.parseHostAndPort("https://www.google.com"));
        assertEquals("www.google.com", ProxyUtils.parseHostAndPort("https://www.google.com/?kw=1"));
        assertEquals("www.google.com:443", ProxyUtils.parseHostAndPort("https://www.google.com:443"));
        assertEquals("www.google.com:443", ProxyUtils.parseHostAndPort("https://www.google.com:443/"));
        assertNull(ProxyUtils.parseHostAndPort("www.google.com"));
        try {
            ProxyUtils.parseHostAndPort(null);
        } catch (Exception error) {
            assertTrue(error instanceof NullPointerException);
        }
    }

    @Test
    public void testParseHostAndPortString() {
        HostAndPort hostAndPort = ProxyUtils.parseHostAndPortString("www.google.com:443", 80);
        assertNotNull(hostAndPort);
        assertEquals(443, hostAndPort.getPort());
        assertEquals("www.google.com", hostAndPort.getHost());
        hostAndPort = ProxyUtils.parseHostAndPortString("www.google.com", 80);
        assertNotNull(hostAndPort);
        assertEquals(80, hostAndPort.getPort());
        assertEquals("www.google.com", hostAndPort.getHost());
        try {
            ProxyUtils.parseHostAndPortString(null, 80);
        } catch (Exception error) {
            assertTrue(error instanceof NullPointerException);
        }
        try {
            ProxyUtils.parseHostAndPortString("https://www.google.com:443/", 443);
        } catch (Exception error) {
            assertTrue(error instanceof IllegalArgumentException);
        }
    }

    @Test
    public void testIsHttpsTunnelingRequest() {
        assertTrue(ProxyUtils.isHttpsTunnelingRequest(new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.CONNECT, "/")));
        assertFalse(ProxyUtils.isHttpsTunnelingRequest(new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/")));
        assertFalse(ProxyUtils.isHttpsTunnelingRequest(new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/")));
    }

    @Test
    public void testIsShouldBeRemovedHeader() {
        assertTrue(ProxyUtils.isShouldBeRemovedHeader(HttpHeaderNames.PROXY_AUTHENTICATE.toString()));
        assertTrue(ProxyUtils.isShouldBeRemovedHeader(HttpHeaderNames.PROXY_AUTHORIZATION.toString()));
        assertTrue(ProxyUtils.isShouldBeRemovedHeader(HttpHeaderNames.PROXY_CONNECTION.toString()));
        assertFalse(ProxyUtils.isShouldBeRemovedHeader(HttpHeaderNames.CONTENT_LENGTH.toString()));
        assertFalse(ProxyUtils.isShouldBeRemovedHeader(HttpHeaderNames.CONTENT_TYPE.toString()));
    }
}
