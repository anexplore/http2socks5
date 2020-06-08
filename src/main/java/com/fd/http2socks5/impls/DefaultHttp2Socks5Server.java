package com.fd.http2socks5.impls;

import com.fd.http2socks5.*;
import com.fd.http2socks5.utils.ChannelUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class DefaultHttp2Socks5Server implements Http2Socks5Server {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultHttp2Socks5Server.class);
    private Configuration configuration;
    private volatile EventLoopGroup bossGroup;
    private volatile EventLoopGroup workerGroup;
    private volatile Channel channel;

    public DefaultHttp2Socks5Server(Configuration configuration) {
        this.configuration = configuration;
    }

    public void startup() {
        bossGroup = new NioEventLoopGroup(configuration.mainEventGroupNumber());
        workerGroup = new NioEventLoopGroup(configuration.workerEventGroupNumber());
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            ChannelPipeline pipeline = socketChannel.pipeline();
                            pipeline.addLast(new IdleStateHandler(configuration.idleTimeoutForClient(),
                                    configuration.idleTimeoutForClient(), configuration.idleTimeoutForClient(), TimeUnit.MILLISECONDS));
                            pipeline.addLast(new StateHandler());
                            pipeline.addLast(Constants.HTTP_REQUEST_DECODER_NAME, new HttpRequestDecoder());
                            pipeline.addLast(Constants.MAIN_HANDLER, new HttpServerHandler(configuration));
                            if (configuration.openNettyLoggingHandler()) {
                                pipeline.addFirst(Constants.NETTY_LOGGING_HANDLER_NAME, Constants.DEBUG_LOGGING_HANDLER);
                            }
                        }
                    })
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .option(ChannelOption.SO_BACKLOG, configuration.maxConnectionBacklog())
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.AUTO_READ, configuration.channelAutoRead());
            if (configuration.openNettyLoggingHandler()) {
                b.handler(Constants.DEBUG_LOGGING_HANDLER);
            }
            channel = b.bind(configuration.httpServerBindLocalAddress(), configuration.httpServerBindLocalPort()).sync().channel();
            LOG.info("http2socks5 server success bind on: {}:{}", configuration.httpServerBindLocalAddress(), configuration.httpServerBindLocalPort());
        } catch (Exception error){
            LOG.error("start http2socks server occurs error", error);
            shutdown();
        }
    }

    public void shutdown() {
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
            bossGroup = null;
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
            workerGroup = null;
        }
        if (channel != null) {
            ChannelUtils.closeOnFlush(channel);
            channel = null;
        }
        LOG.info("http2socks server shutdown");
    }

}
