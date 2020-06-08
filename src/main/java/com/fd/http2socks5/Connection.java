package com.fd.http2socks5;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;

public interface Connection {
    /**
     * close connection
     */
    void closeConnection();

    /**
     * @return {@link Channel}
     */
    Channel channel();

    /**
     * write and flush message
     * @param message message instances
     * @return {@link ChannelFuture}
     */
    ChannelFuture writeAndFlush(Object message);

    /**
     * @return true if connection is active
     */
    boolean isActive();
}
