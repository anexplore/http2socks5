package com.fd.http2socks5;

import io.netty.handler.codec.socksx.v5.Socks5AuthMethod;

public class Socks5Proxy {
    private final String host;
    private final int port;

    private final String username;
    private final String password;

    private final Socks5AuthMethod authMethod;

    public Socks5Proxy(String host, int port) {
        this(host, port, null, null, null);
    }

    public Socks5Proxy(String host, int port, Socks5AuthMethod authMethod, String username, String password) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.authMethod = authMethod;
    }

    public String host() {
        return host;
    }

    public int port() {
        return port;
    }

    public String username() {
        return username;
    }

    public String password() {
        return password;
    }

    public Socks5AuthMethod authMethod() {
        return authMethod;
    }

    @Override
    public String toString() {
        return "Socks5Proxy{" +
                "host='" + host + '\'' +
                ", port=" + port +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", authMethod=" + authMethod +
                '}';
    }
}
