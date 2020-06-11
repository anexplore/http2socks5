package com.fd.http2socks5.impls;

import com.fd.http2socks5.Configuration;
import com.fd.http2socks5.Socks5ProxyProvider;
import com.google.common.base.Strings;

public class EnvProConfiguration implements Configuration {

    private final Socks5ProxyProvider proxyProvider;

    public EnvProConfiguration(Socks5ProxyProvider proxyProvider) {
        this.proxyProvider = proxyProvider;
    }

    /**
     * Get Value for key from os system env or jvm properties, jvm priority is higher than os env
     * @param key key
     * @return value or {@code defaultValue} if pro is empty
     */
    public static String getFromEnvOrPro(String key, String defaultValue) {
        String pro = System.getProperty(key);
        if (Strings.isNullOrEmpty(pro)) {
            pro = System.getenv(key);
        }
        return Strings.isNullOrEmpty(pro) ? defaultValue : pro;
    }

    @Override
    public boolean openNettyLoggingHandler() {
        return Integer.parseInt(getFromEnvOrPro("openNettyLoggingHandler", "0")) == 1;
    }

    @Override
    public boolean channelAutoRead() {
        return Integer.parseInt(getFromEnvOrPro("channelAutoRead", "0")) == 1;
    }

    @Override
    public int timeoutToSocks5ProxyServer() {
        return Integer.parseInt(getFromEnvOrPro("timeoutToSocks5ProxyServer", "10000"));
    }

    @Override
    public int connectionTimeoutToSocks5ProxyServer() {
        return Integer.parseInt(getFromEnvOrPro("connectionTimeoutToSocks5ProxyServer", "10000"));
    }

    @Override
    public int mainEventGroupNumber() {
        return 1;
    }

    @Override
    public int workerEventGroupNumber() {
        return Integer.parseInt(getFromEnvOrPro("workerEventGroupNumber", "" + Runtime.getRuntime().availableProcessors()));
    }

    @Override
    public int maxConnectionBacklog() {
        return Integer.parseInt(getFromEnvOrPro("maxConnectionBacklog", "1000"));
    }

    @Override
    public int idleTimeoutForClient() {
        return Integer.parseInt(getFromEnvOrPro("idleTimeoutForClient", "10000"));
    }

    @Override
    public String httpServerBindLocalAddress() {
        return getFromEnvOrPro("httpServerBindLocalAddress", "0.0.0.0");
    }

    @Override
    public int httpServerBindLocalPort() {
        return Integer.parseInt(getFromEnvOrPro("httpServerBindLocalPort", "80"));
    }

    @Override
    public Socks5ProxyProvider socks5ProxyProvider() {
       return proxyProvider;
    }
}
