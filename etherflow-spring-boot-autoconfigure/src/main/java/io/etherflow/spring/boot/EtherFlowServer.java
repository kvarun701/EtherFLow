package io.etherflow.spring.boot;

import io.etherflow.server.netty.NettyServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

public class EtherFlowServer implements InitializingBean, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(EtherFlowServer.class);

    private final NettyServer nettyServer;
    private final int port;

    public EtherFlowServer(NettyServer nettyServer, int port) {
        this.nettyServer = nettyServer;
        this.port = port;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        nettyServer.start();
        log.info("EtherFlow server started on port {}", port);
    }

    @Override
    public void destroy() {
        nettyServer.stop();
        log.info("EtherFlow server stopped");
    }
}
