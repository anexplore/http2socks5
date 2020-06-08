package com.fd.http2socks5;

import com.fd.http2socks5.utils.ChannelUtils;
import com.fd.http2socks5.utils.ProxyUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ConnectionFromClient implements Connection {
    private static final Logger LOG = LoggerFactory.getLogger(ConnectionFromClient.class);
    // channel
    private final Channel channel;
    // http request target host
    private final String targetHost;
    // http request target port
    private final int targetPort;
    // is https tunneling request
    private final boolean tunneling;
    // pending messages, this message will be share in same event group
    private final List<Object> pendingMessages;
    // Configuration
    private final Configuration configuration;

    public ConnectionFromClient(Channel channel, String targetHost, int targetPort, boolean tunneling,
                                List<Object> pendingMessages, Configuration configuration) {
        this.channel = channel;
        this.targetHost = targetHost;
        this.targetPort = targetPort;
        this.tunneling = tunneling;
        this.pendingMessages = pendingMessages;
        this.configuration = configuration;
    }

    @Override
    public Channel channel() {
        return channel;
    }

    public String targetHost() {
        return targetHost;
    }

    public int targetPort() {
        return targetPort;
    }

    public boolean isTunneling() {
        return tunneling;
    }

    public List<Object> pendingMessages() {
        return pendingMessages;
    }

    public Configuration configuration() {
        return configuration;
    }

    /**
     * send tunneling established response
     * @return {@link ChannelFuture}
     */
    public ChannelFuture writeTunnelingEstablishedResponse() {
        return channel.pipeline().writeAndFlush(ProxyUtils.buildOpenHttpsTunnelingSuccessResponse());
    }

    /**
     * Enable Channel AutoRead
     */
    public void enableChannelAutoRead() {
        channel.config().setAutoRead(true);
    }

    /**
     * {@code channel} is closed
     *
     * @return true if channel is closed
     */
    public boolean isConnectionClosed() {
        return !channel.isActive();
    }

    @Override
    public ChannelFuture writeAndFlush(Object message) {
        return channel.writeAndFlush(message);
    }

    @Override
    public boolean isActive() {
        return channel.isActive();
    }

    @Override
    public void closeConnection() {
        LOG.debug("close connection from client: {}", channel);
        ChannelUtils.closeOnFlush(channel);
    }
}
