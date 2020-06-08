package com.fd.http2socks5.impls;

import com.fd.http2socks5.*;
import com.google.common.base.Strings;
import io.netty.handler.codec.socksx.v5.Socks5AuthMethod;

public class DefaultHttp2Socks5ServerBootstrap implements Http2Socks5ServerBootstrap {

    private volatile Socks5ProxyProvider proxyProvider;
    private volatile Configuration configuration;
    private volatile Http2Socks5Server server;

    private Configuration buildDefaultConfiguration() {
        if (proxyProvider == null) {
            proxyProvider = buildDefaultSocks5ProxyProvider();
        }
        return new EnvProConfiguration(proxyProvider);
    }

    private Socks5ProxyProvider buildDefaultSocks5ProxyProvider() {
        String host = EnvProConfiguration.getFromEnvOrPro("socks5ProxyHost", "");
        int port = Integer.parseInt(EnvProConfiguration.getFromEnvOrPro("socks5ProxyPort", "1080"));
        String username = EnvProConfiguration.getFromEnvOrPro("socks5ProxyUsername", "");
        String password = EnvProConfiguration.getFromEnvOrPro("socks5ProxyPassword", "");
        if (Strings.isNullOrEmpty(host) || port <= 0) {
            throw new RuntimeException("socks5 proxy parameters is wrong");
        }
        Socks5Proxy proxy = new Socks5Proxy(host, port,
                Strings.isNullOrEmpty(username) || Strings.isNullOrEmpty(password) ? Socks5AuthMethod.NO_AUTH : Socks5AuthMethod.PASSWORD,
                username, password);
        return new SingleSocks5ProxyProvider(proxy);
    }

    public void setProxyProvider(Socks5ProxyProvider proxyProvider) {
        this.proxyProvider = proxyProvider;
    }

    private void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }


    @Override
    public void startup() {
        if (configuration == null) {
            configuration = buildDefaultConfiguration();
        }
        server = new DefaultHttp2Socks5Server(configuration);
        server.startup();
    }

    @Override
    public void shutdown() {
        if (server != null) {
            server.shutdown();
            server = null;
        }
    }
}
