package io.etherflow.server.netty;

import io.etherflow.core.Mono;
import io.etherflow.http.HttpHandler;
import io.etherflow.http.ServerWebExchange;
import io.etherflow.web.DispatcherHandler;
import io.etherflow.web.function.RouterFunction;
import io.etherflow.web.function.RouterFunctionMapping;
import io.etherflow.web.function.ServerResponse;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpServerCodec;

public class NettyServer {

    private final int port;
    private final HttpHandler handler;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel channel;

    public NettyServer(int port, HttpHandler handler) {
        this.port = port;
        this.handler = handler;
    }

    public void start() throws InterruptedException {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
         .channel(NioServerSocketChannel.class)
         .childHandler(new ChannelInitializer<SocketChannel>() {
             @Override
             protected void initChannel(SocketChannel ch) {
                 ch.pipeline().addLast(new HttpServerCodec());
                 ch.pipeline().addLast(new SimpleChannelInboundHandler<FullHttpRequest>() {
                     @Override
                     protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest nettyRequest) {
                         NettyServerHttpRequest request = new NettyServerHttpRequest(nettyRequest);
                         NettyServerHttpResponse response = new NettyServerHttpResponse(ctx);
                         ServerWebExchange exchange = new ServerWebExchange(request, response);
                         handler.handle(exchange).subscribe(
                                 v -> {},
                                 err -> err.printStackTrace());
                     }
                 });
             }
         });

        ChannelFuture f = b.bind(port).sync();
        channel = f.channel();
    }

    public void stop() {
        if (channel != null) channel.close();
        if (workerGroup != null) workerGroup.shutdownGracefully();
        if (bossGroup != null) bossGroup.shutdownGracefully();
    }

    public void await() throws InterruptedException {
        if (channel != null) channel.closeFuture().sync();
    }

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;

        RouterFunction routes = RouterFunction.route()
                .GET("/hello", req -> Mono.just(ServerResponse.ok("Hello EtherFlow!")))
                .build();

        DispatcherHandler dispatcher = new DispatcherHandler();
        dispatcher.addHandlerMapping(new RouterFunctionMapping(routes));
        dispatcher.addHandlerAdapter(new RouterFunctionMapping(routes));

        NettyServer server = new NettyServer(port, dispatcher);
        server.start();
        System.out.println("EtherFlow server started on port " + port);
        server.await();
    }
}
