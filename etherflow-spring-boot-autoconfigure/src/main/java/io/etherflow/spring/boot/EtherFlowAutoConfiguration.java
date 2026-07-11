package io.etherflow.spring.boot;

import io.etherflow.http.HttpHandler;
import io.etherflow.server.netty.NettyServer;
import io.etherflow.web.DispatcherHandler;
import io.etherflow.web.function.RouterFunction;
import io.etherflow.web.function.RouterFunctionMapping;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.List;

@AutoConfiguration
@EnableConfigurationProperties(EtherFlowProperties.class)
public class EtherFlowAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(HttpHandler.class)
    public DispatcherHandler dispatcherHandler(List<RouterFunction> routerFunctions) {
        DispatcherHandler dispatcher = new DispatcherHandler();
        for (RouterFunction routes : routerFunctions) {
            RouterFunctionMapping mapping = new RouterFunctionMapping(routes);
            dispatcher.addHandlerMapping(mapping);
            dispatcher.addHandlerAdapter(mapping);
        }
        return dispatcher;
    }

    @Bean
    @ConditionalOnMissingBean(NettyServer.class)
    public NettyServer nettyServer(HttpHandler httpHandler, EtherFlowProperties properties) {
        return new NettyServer(properties.getPort(), httpHandler);
    }

    @Bean
    @ConditionalOnMissingBean(EtherFlowServer.class)
    public EtherFlowServer etherFlowServer(NettyServer nettyServer, EtherFlowProperties properties) {
        return new EtherFlowServer(nettyServer, properties.getPort());
    }
}
