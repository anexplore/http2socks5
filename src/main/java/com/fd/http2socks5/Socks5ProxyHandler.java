package com.fd.http2socks5;

import com.google.common.net.InetAddresses;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.socksx.SocksMessage;
import io.netty.handler.codec.socksx.v5.*;
import io.netty.util.NetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Socks5ProxyHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(Socks5ProxyHandler.class);

    private final Configuration configuration;
    private final ConnectionFromClient connectionFromClient;
    private final ConnectionToProxy connectionToProxy;
    private final Socks5Proxy socks5Proxy;
    private ScheduledFuture<?> timeoutFuture;
    private boolean connectionSuccess = false;

    public ChannelHandlerContext ctx;

    public Socks5ProxyHandler(Socks5Proxy socks5Proxy, ConnectionFromClient connectionFromClient,
                              ConnectionToProxy connectionToProxy, Configuration configuration) {
        this.configuration = configuration;
        this.connectionFromClient = connectionFromClient;
        this.connectionToProxy = connectionToProxy;
        this.socks5Proxy = socks5Proxy;
    }

    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        LOG.debug("connection to socks5 proxy {} active", socks5Proxy);
        this.ctx = ctx;
        preparePipelineHandlerForInitialRequest();
        sendSocks5ProxyInitialRequest();
    }

    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOG.debug("socks5 proxy channel occurs error", cause);
        ctx.close().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                connectionFromClient.closeConnection();
            }
        });
    }

    private void preparePipelineHandlerForInitialRequest() {
        ctx.pipeline().addBefore(Constants.MAIN_HANDLER, Constants.ENCODER_NAME, Socks5ClientEncoder.DEFAULT);
        ctx.pipeline().addBefore(Constants.MAIN_HANDLER, Constants.DECODER_NAME, new Socks5InitialResponseDecoder());
    }

    private void sendSocks5ProxyInitialRequest() {
        // create timeout
        if (configuration.connectionTimeoutToSocks5ProxyServer() > 0) {
            // must be in same event group for no locks
            timeoutFuture = ctx.channel().eventLoop().schedule(this::establishConnectionFailed,
                    configuration.connectionTimeoutToSocks5ProxyServer(), TimeUnit.MILLISECONDS);
        }
        writeSocks5Message(newSocks5InitialRequest());
    }

    private Socks5InitialRequest newSocks5InitialRequest() {
        if (socks5Proxy.authMethod() != null) {
            return new DefaultSocks5InitialRequest(socks5Proxy.authMethod());
        } else {
            return new DefaultSocks5InitialRequest(Socks5AuthMethod.NO_AUTH);
        }
    }

    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        LOG.debug("received socks5 proxy response:{}", msg);
        if (msg instanceof Socks5InitialResponse) {
            Socks5InitialResponse res = (Socks5InitialResponse) msg;
            if (res.authMethod().equals(Socks5AuthMethod.NO_AUTH)) {
                // replace decoder to command decoder
                ctx.pipeline().replace(Constants.DECODER_NAME, Constants.DECODER_NAME, new Socks5CommandResponseDecoder());
                writeSocks5Message(newConnectRequest());
            } else if (res.authMethod().equals(socks5Proxy.authMethod())) {
                // replace decoder to auth response decoder
                ctx.pipeline().replace(Constants.DECODER_NAME, Constants.DECODER_NAME, new Socks5PasswordAuthResponseDecoder());
                writeSocks5Message(newAuthRequest());
            } else {
                LOG.error("unsupported socks5 proxy auth method, supported: {}, actual: {}",
                        socks5Proxy.authMethod(), res.authMethod());
                establishConnectionFailed();
            }
        } else if (msg instanceof Socks5PasswordAuthResponse) {
            Socks5PasswordAuthResponse res = (Socks5PasswordAuthResponse) msg;
            if (res.status().equals(Socks5PasswordAuthStatus.SUCCESS)) {
                // replace decoder to command decoder
                ctx.pipeline().replace(Constants.DECODER_NAME, Constants.DECODER_NAME, new Socks5CommandResponseDecoder());
                writeSocks5Message(newConnectRequest());
            } else {
                establishConnectionFailed();
            }
        } else if (msg instanceof Socks5CommandResponse) {
            Socks5CommandResponse res = (Socks5CommandResponse) msg;
            if (res.status().equals(Socks5CommandStatus.SUCCESS)) {
                establishConnectionSuccess();
                return;
            } else {
                establishConnectionFailed();
            }
        }
        // transfer data after connection success
        if (connectionSuccess) {
            ctx.fireChannelRead(msg);
        }
    }

    private void cancelTimeoutListener() {
        if (timeoutFuture != null) {
            timeoutFuture.cancel(false);
            timeoutFuture = null;
        }
    }

    private void establishConnectionFailed() {
        LOG.debug("establish connection failed to {}", socks5Proxy);
        connectionSuccess = false;
        cancelTimeoutListener();
        ctx.close().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                connectionFromClient.closeConnection();
            }
        });
    }

    private void sendPendingMessageFailed() {
        LOG.debug("send pending message failed");
        connectionSuccess = false;
        cancelTimeoutListener();
        ctx.close().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                connectionFromClient.closeConnection();
            }
        });
    }

    private void clientConnectionClosed() {
        LOG.debug("client connection has already close before connection to socks5 proxy established");
        ctx.close();
    }

    private void establishConnectionSuccess() {
        LOG.debug("establish connection success to {}", socks5Proxy);
        connectionSuccess = true;
        // cancel timeout handler
        cancelTimeoutListener();
        // connection from client has already closed
        if (connectionFromClient.isConnectionClosed()) {
            clientConnectionClosed();
            return;
        }
        // remove handler for socks5 proxy establish connection
        ctx.pipeline().remove(Constants.DECODER_NAME);
        ctx.pipeline().remove(Constants.ENCODER_NAME);
        // add handler for encode pending message
        ctx.pipeline().addLast(Constants.HTTP_REQUEST_ENCODER_NAME, new HttpRequestEncoder());
        // add raw data transfer handler
        ctx.pipeline().addLast(new DataTransferHandler(connectionFromClient));
        // build pipeline for client channel
        connectionFromClient.channel().pipeline().remove(Constants.MAIN_HANDLER);
        connectionFromClient.channel().pipeline().addAfter(Constants.HTTP_REQUEST_DECODER_NAME,
                Constants.HTTP_RESPONSE_ENCODER_NAME, new HttpResponseEncoder());
        connectionFromClient.channel().pipeline().addLast(new DataTransferHandler(connectionToProxy));
        sendPendingMessages();
    }

    private void sendPendingMessages() {
        ChannelFutureListener pendingMessageProcessFinishedListener = new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                LOG.debug("send pending messages finished with success: {}", channelFuture.isSuccess());
                if (channelFuture.isSuccess()) {
                    // remove all http handlers for tunneling
                    connectionFromClient.channel().pipeline().remove(Constants.HTTP_REQUEST_DECODER_NAME);
                    connectionFromClient.channel().pipeline().remove(Constants.HTTP_RESPONSE_ENCODER_NAME);
                    ctx.pipeline().remove(Constants.HTTP_REQUEST_ENCODER_NAME);
                    // open auto read
                    connectionFromClient.enableChannelAutoRead();
                } else {
                    sendPendingMessageFailed();
                }
            }
        };
        // process pending messages
        if (connectionFromClient.isTunneling()) {
            // skip pending messages for CONNECT method
            connectionFromClient.writeTunnelingEstablishedResponse().addListener(pendingMessageProcessFinishedListener);
        } else {
            ChannelFuture last = null;
            while (connectionFromClient.pendingMessages().size() > 0) {
                last = ctx.pipeline().writeAndFlush(connectionFromClient.pendingMessages().remove(0));
            }
            if (last != null) {
                last.addListener(pendingMessageProcessFinishedListener);
            }
        }
    }

    private void writeSocks5Message(SocksMessage message) {
        ctx.writeAndFlush(message);
    }

    private Socks5PasswordAuthRequest newAuthRequest() {
        return new DefaultSocks5PasswordAuthRequest(socks5Proxy.username() == null ? "" : socks5Proxy.username(),
                socks5Proxy.password() == null ? "" : socks5Proxy.password());
    }

    private Socks5CommandRequest newConnectRequest() {
        // prepare address
        Socks5AddressType type = Socks5AddressType.DOMAIN;
        if (InetAddresses.isInetAddress(connectionFromClient.targetHost())) {
            if (NetUtil.isValidIpV4Address(connectionFromClient.targetHost())) {
                type = Socks5AddressType.IPv4;
            }
            if (NetUtil.isValidIpV6Address(connectionFromClient.targetHost())) {
                type = Socks5AddressType.IPv6;
            }
        }
        return new DefaultSocks5CommandRequest(Socks5CommandType.CONNECT, type, connectionFromClient.targetHost(),
                connectionFromClient.targetPort());
    }

}
