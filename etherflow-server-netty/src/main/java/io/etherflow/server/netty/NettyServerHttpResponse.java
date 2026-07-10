package io.etherflow.server.netty;

import io.etherflow.codec.DataBuffer;
import io.etherflow.core.Mono;
import io.etherflow.http.HttpHeaders;
import io.etherflow.http.ServerHttpResponse;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import java.util.concurrent.Callable;

class NettyServerHttpResponse implements ServerHttpResponse {

    private final ChannelHandlerContext ctx;
    private final HttpHeaders headers = new HttpHeaders();
    private int statusCode = 200;

    NettyServerHttpResponse(ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public HttpHeaders headers() {
        return headers;
    }

    @Override
    public int statusCode() {
        return statusCode;
    }

    @Override
    public ServerHttpResponse statusCode(int statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    @Override
    public Mono<Void> writeWith(DataBuffer buffer) {
        Callable<Void> task = () -> {
            DefaultFullHttpResponse nettyResponse = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.valueOf(statusCode),
                    Unpooled.wrappedBuffer(buffer.asByteArray()));

            for (String name : headers.names()) {
                for (String value : headers.get(name)) {
                    nettyResponse.headers().add(name, value);
                }
            }

            if (!nettyResponse.headers().contains("content-length")) {
                nettyResponse.headers().set("content-length", buffer.readableByteCount());
            }

            ctx.writeAndFlush(nettyResponse).addListener(ChannelFutureListener.CLOSE);
            return null;
        };
        return Mono.fromCallable(task);
    }

    @Override
    public Mono<Void> writeAndFlushWith(DataBuffer buffer) {
        return writeWith(buffer);
    }
}
