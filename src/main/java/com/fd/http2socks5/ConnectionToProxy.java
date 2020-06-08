package com.fd.http2socks5;

import com.fd.http2socks5.utils.ChannelUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ConnectionToProxy implements Connection {
    public static final Logger LOG = LoggerFactory.getLogger(ConnectionToProxy.class);
    private final Bootstrap bootstrap = new Bootstrap();
    private final ConnectionFromClient connectionFromClient;
    private final Configuration configuration;
    private Channel channel;

    public ConnectionToProxy(ConnectionFromClient connectionFromClient, Configuration configuration) {
        this.connectionFromClient = connectionFromClient;
        this.configuration = configuration;
    }

    /**
     * Connect to Socks5 Proxy
     * @return ChannelFuture for tcp connect
     */
    public ChannelFuture connect() {
        Socks5Proxy proxy = configuration.socks5ProxyProvider().provide();
        bootstrap.group(connectionFromClient.channel().eventLoop())
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        ChannelPipeline pipeline = socketChannel.pipeline();
                        pipeline.addLast(Constants.MAIN_HANDLER, new Socks5ProxyHandler(proxy, connectionFromClient,
                                ConnectionToProxy.this, configuration));
                        if (configuration.openNettyLoggingHandler()) {
                            pipeline.addFirst(Constants.NETTY_LOGGING_HANDLER_NAME, Constants.DEBUG_LOGGING_HANDLER);
                        }
                    }
                })
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, configuration.connectionTimeoutToSocks5ProxyServer())
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.SO_REUSEADDR, true)
                .option(ChannelOption.TCP_NODELAY, true);
        return bootstrap.connect(proxy.host(), proxy.port()).addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                if (channelFuture.isSuccess()) {
                    channel = channelFuture.channel();
                    LOG.debug("tcp connect to socks5 proxy success, {}, {}", channel, proxy);
                    // may be client channel has closed before tcp connect to proxy success
                    if (connectionFromClient.isConnectionClosed()) {
                        closeConnection();
                    }
                } else {
                    LOG.debug("tcp connect to socks5 proxy failed, {}", proxy);
                    connectionFromClient.closeConnection();
                }
            }
        });
    }

    @Override
    public ChannelFuture writeAndFlush(Object message) {
        if (!isActive()) {
            throw new RuntimeException("channel has already closed");
        }
        return channel.writeAndFlush(message);
    }

    @Override
    public Channel channel() {
        return channel;
    }

    @Override
    public boolean isActive() {
        return channel != null && channel.isActive();
    }

    @Override
    public void closeConnection() {
        LOG.debug("close connection to proxy: {}", channel);
        ChannelUtils.closeOnFlush(channel);
    }
}
