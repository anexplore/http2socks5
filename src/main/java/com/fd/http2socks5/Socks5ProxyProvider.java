package com.fd.http2socks5;

public interface Socks5ProxyProvider {
    /**
     * provide a socks5 proxy
     *
     * @return {@link Socks5Proxy}
     */
    Socks5Proxy provide();
}
