package io.etherflow.web;

import io.etherflow.http.*;
import io.etherflow.core.Mono;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class DispatcherHandler implements HttpHandler {

    private final List<HandlerMapping> handlerMappings = new CopyOnWriteArrayList<>();
    private final List<HandlerAdapter> handlerAdapters = new CopyOnWriteArrayList<>();
    private final List<WebFilter> filters = new CopyOnWriteArrayList<>();
    private final List<WebExceptionHandler> exceptionHandlers = new CopyOnWriteArrayList<>();

    public DispatcherHandler addHandlerMapping(HandlerMapping mapping) {
        handlerMappings.add(mapping);
        return this;
    }

    public DispatcherHandler addHandlerAdapter(HandlerAdapter adapter) {
        handlerAdapters.add(adapter);
        return this;
    }

    public DispatcherHandler addFilter(WebFilter filter) {
        filters.add(filter);
        return this;
    }

    public DispatcherHandler addExceptionHandler(WebExceptionHandler handler) {
        exceptionHandlers.add(handler);
        return this;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange) {
        WebFilter.WebFilterChain chain = createFilterChain();
        return chain.filter(exchange);
    }

    private WebFilter.WebFilterChain createFilterChain() {
        WebFilter.WebFilterChain chain = this::handleInternal;

        List<WebFilter> reversed = new ArrayList<>(filters);
        Collections.reverse(reversed);

        for (WebFilter filter : reversed) {
            WebFilter.WebFilterChain current = chain;
            chain = exchange -> filter.filter(exchange, current);
        }
        return chain;
    }

    private Mono<Void> handleInternal(ServerWebExchange exchange) {
        return findHandlerAdapter(exchange)
                .flatMap(adapter -> adapter.handle(exchange, exchange.request()))
                .then();
    }

    private Mono<HandlerAdapter> findHandlerAdapter(ServerWebExchange exchange) {
        return findHandler(exchange)
                .flatMap(handler -> {
                    for (HandlerAdapter adapter : handlerAdapters) {
                        if (adapter.supports(handler)) {
                            return Mono.just(adapter);
                        }
                    }
                    return Mono.error(new IllegalStateException("No HandlerAdapter for handler: " + handler));
                });
    }

    private Mono<Object> findHandler(ServerWebExchange exchange) {
        for (HandlerMapping mapping : handlerMappings) {
            try {
                Mono<Object> handler = mapping.getHandler(exchange);
                Object result = handler.block();
                if (result != null) {
                    return Mono.just(result);
                }
            } catch (Exception ignored) {}
        }
        return Mono.error(new IllegalStateException("No handler found for " + exchange.request().path()));
    }
}
