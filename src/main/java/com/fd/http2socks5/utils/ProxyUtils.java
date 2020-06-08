package com.fd.http2socks5.utils;

import com.google.common.collect.ImmutableSet;
import com.google.common.net.HostAndPort;
import io.netty.handler.codec.http.*;

import java.util.Set;

public class ProxyUtils {

    private static final Set<String> SHOULD_REMOVED_HEADER_BY_PROXY = ImmutableSet.of(
            HttpHeaderNames.PROXY_AUTHENTICATE.toString().toLowerCase(),
            HttpHeaderNames.PROXY_AUTHORIZATION.toString().toLowerCase(),
            HttpHeaderNames.PROXY_CONNECTION.toString().toLowerCase()
    );

    /**
     * find relative uri for a url
     *
     * @param url a http url
     * @return uri part of url
     */
    public static String parseUri(String url) {
        int spos = url.indexOf("://");
        if (spos < 0) {
            return url;
        }
        int npos = url.indexOf("/", spos + 3);
        if (npos > 0) {
            return url.substring(npos);
        }
        return "/";
    }

    /**
     * get host:port part for uri
     *
     * @param uri uri string
     * @return host:port or null
     */
    public static String parseHostAndPort(String uri) {
        int spos = uri.indexOf("://");
        if (spos < 0) {
            return null;
        }
        int nspos = uri.indexOf("/", spos + 3);
        if (nspos < 0) {
            return uri.substring(spos + 3);
        }
        return uri.substring(spos + 3, nspos);
    }

    /**
     * build Host And Port instance
     *
     * @param hostAndPort host and port string, google.com/google.com:443
     * @param defaultPort default port if {@code hostAndPort} string has no port
     * @return {@link HostAndPort} instance
     */
    public static HostAndPort parseHostAndPortString(String hostAndPort, int defaultPort) {
        return HostAndPort.fromString(hostAndPort).withDefaultPort(defaultPort);
    }

    /**
     * CONNECT method is https tunnel request method
     *
     * @param httpRequest http request instance
     * @return true if http method is CONNECT else false
     */
    public static boolean isHttpsTunnelingRequest(HttpRequest httpRequest) {
        return httpRequest.method().equals(HttpMethod.CONNECT);
    }

    /**
     * build Success Http Response for reply CONNECT Request
     *
     * @return {@link HttpResponse}
     */
    public static HttpResponse buildOpenHttpsTunnelingSuccessResponse() {
        DefaultHttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1,
                new HttpResponseStatus(HttpResponseStatus.OK.code(), "Connection established"));
        response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        return response;
    }

    /**
     * build Failed Http Response for reply CONNECT Request
     *
     * @return {@link HttpResponse}
     */
    public static HttpResponse buildOpenHttpsTunnelingFailedResponse() {
        DefaultHttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1,
                new HttpResponseStatus(HttpResponseStatus.BAD_REQUEST.code(), HttpResponseStatus.BAD_REQUEST.reasonPhrase()));
        response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        return response;
    }

    /**
     * @param headerName http header name
     * @return true if header name is one of {@link #SHOULD_REMOVED_HEADER_BY_PROXY}
     */
    public static boolean isShouldBeRemovedHeader(String headerName) {
        return SHOULD_REMOVED_HEADER_BY_PROXY.contains(headerName.toLowerCase());
    }
}
