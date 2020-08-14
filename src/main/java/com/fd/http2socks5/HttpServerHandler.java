package com.fd.http2socks5;

import com.fd.http2socks5.utils.ChannelUtils;
import com.fd.http2socks5.utils.ProxyUtils;
import com.google.common.base.Strings;
import com.google.common.net.HostAndPort;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * client request handler
 */
public class HttpServerHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(HttpServerHandler.class);

    // pending messages which need to send to proxy server after connection established
    private List<Object> pendingMessages = new LinkedList<>();
    // stop read before connection to proxy server established
    private boolean stopRead = false;
    private final Configuration configuration;
    private ConnectionToProxy connectionToProxy;

    public HttpServerHandler(Configuration configuration) {
        this.configuration = configuration;
    }

    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        tryToReadIfNeeded(ctx);
        ctx.fireChannelReadComplete();
    }

    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        tryToReadIfNeeded(ctx);
        ctx.fireChannelActive();
    }

    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        closeChannelConnection(ctx);
    }

    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        LOG.debug("http server received message: {}", msg);
        // first message must be HttpRequest else Error
        if (pendingMessages.size() == 0 && !(msg instanceof HttpRequest)) {
            LOG.error("client's first message must be a http request, but it's {}", msg);
            closeChannelConnection(ctx);
            return;
        }
        pendingMessages.add(msg);
        if (msg instanceof HttpRequest) {
            // stop read
            stopRead = true;
            HttpRequest request = (HttpRequest) msg;
            String host = extractHost(request);
            if (Strings.isNullOrEmpty(host)) {
                LOG.error("cannot find host from request: {}", request);
                closeChannelConnection(ctx);
                return;
            }
            boolean tunneling = ProxyUtils.isHttpsTunnelingRequest(request);
            HostAndPort hostAndPort = ProxyUtils.parseHostAndPortString(host,
                    tunneling ? Constants.DEFAULT_HTTPS_PORT : Constants.DEFAULT_HTTP_PORT);
            modifyHttpRequestBeforeSendToProxyServer(request);
            ConnectionFromClient cf = new ConnectionFromClient(ctx.channel(), hostAndPort.getHost(),
                    hostAndPort.getPort(), tunneling, pendingMessages, configuration);
            connectionToProxy = new ConnectionToProxy(cf, configuration);
            connectionToProxy.connect();
        }
    }

    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOG.error("http server handler occurs error", cause);
        closeChannelConnection(ctx);
    }

    private String extractHost(HttpRequest request) {
        String host = request.headers().get(HttpHeaderNames.HOST);
        // do not has HOST header, get it from uri
        if (Strings.isNullOrEmpty(host)) {
            host = ProxyUtils.parseHostAndPort(request.uri());
        }
        if (Strings.isNullOrEmpty(host) && request.method().equals(HttpMethod.CONNECT)) {
           host = request.uri();
        }
        return host;
    }

    private void modifyHttpRequestBeforeSendToProxyServer(HttpRequest httpRequest) {
        // remove proxy headers
        Set<String> headerNames = httpRequest.headers().names();
        for (String headerName : headerNames) {
            if (ProxyUtils.isShouldBeRemovedHeader(headerName)) {
                httpRequest.headers().remove(headerName);
            }
        }
        String uri = httpRequest.uri();
        // rebuild request uri
        httpRequest.setUri(ProxyUtils.parseUri(uri));
    }

    private void tryToReadIfNeeded(ChannelHandlerContext ctx) {
        if (!ctx.channel().config().isAutoRead() && !stopRead) {
            ctx.read();
        }
    }

    private void closeChannelConnection(ChannelHandlerContext ctx) {
        LOG.debug("close client connection: {}", ctx.channel().remoteAddress());
        ChannelUtils.closeOnFlush(ctx.channel());
        if (connectionToProxy != null) {
            connectionToProxy.closeConnection();
        }
    }

}
