package com.fd.http2socks5;

import com.fd.http2socks5.impls.DefaultHttp2Socks5ServerBootstrap;

public class Launcher {

    private Http2Socks5ServerBootstrap bootstrap;

    /**
     * @return {@link Http2Socks5ServerBootstrap}
     */
    public Http2Socks5ServerBootstrap getBootstrap() {
        return bootstrap;
    }

    /**
     * set bootstrap
     * @param bootstrap {@link Http2Socks5ServerBootstrap}
     */
    public void setBootstrap(Http2Socks5ServerBootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    public void run() {
        if (bootstrap == null) {
            bootstrap = new DefaultHttp2Socks5ServerBootstrap();
        }
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                bootstrap.shutdown();
            }
        });
        bootstrap.startup();
    }

    public static void main(String[] args) {
        new Launcher().run();
    }
}
