package com.fd.http2socks5.impls;

import com.fd.http2socks5.Socks5Proxy;
import com.fd.http2socks5.Socks5ProxyProvider;

public class SingleSocks5ProxyProvider implements Socks5ProxyProvider {

    private final Socks5Proxy socks5Proxy;

    public SingleSocks5ProxyProvider(Socks5Proxy socks5Proxy) {
        this.socks5Proxy = socks5Proxy;
    }

    @Override
    public Socks5Proxy provide() {
        return socks5Proxy;
    }
}
