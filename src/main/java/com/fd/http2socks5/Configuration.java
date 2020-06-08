package com.fd.http2socks5;

public interface Configuration {

    /**
     * @return true if add {@code LoggingHandler} to channel pipeline
     */
    boolean openNettyLoggingHandler();

    /**
     * @return true if set channel option AUTO_READ=true
     */
    boolean channelAutoRead();

    /**
     * @return ms, io timeout to socks5 proxy server
     */
    int timeoutToSocks5ProxyServer();

    /**
     * @return ms, connect timeout to socks5 proxy server
     */
    int connectionTimeoutToSocks5ProxyServer();

    /**
     * @return main event group number to accept client request
     */
    int mainEventGroupNumber();

    /**
     * @return worker event group number
     */
    int workerEventGroupNumber();

    /**
     * @return max connect backlog for tcp connect
     */
    int maxConnectionBacklog();

    /**
     * @return ms, idle timeout for client when no read/write occurs
     */
    int idleTimeoutForClient();

    /**
     * @return http server bind local address, eg: 0.0.0.0
     */
    String httpServerBindLocalAddress();

    /**
     * @return http server bind local port, eg: 1080
     */
    int httpServerBindLocalPort();

    /**
     * @return {@link Socks5ProxyProvider} proxy provider
     */
    Socks5ProxyProvider socks5ProxyProvider();

}
