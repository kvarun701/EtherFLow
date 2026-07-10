package io.etherflow.web.function;

import io.etherflow.core.Mono;

import java.util.*;

@FunctionalInterface
public interface RouterFunction {

    Mono<HandlerFunction> route(ServerRequest request);

    default RouterFunction and(RouterFunction other) {
        Objects.requireNonNull(other);
        return request -> this.route(request)
                .switchIfEmpty(() -> other.route(request));
    }

    default RouterFunction andRoute(RequestPredicate predicate, HandlerFunction handler) {
        return and(new DefaultRouterFunction(predicate, handler));
    }

    static Builder route() {
        return new Builder();
    }

    static RouterFunction of(RequestPredicate predicate, HandlerFunction handler) {
        return new DefaultRouterFunction(predicate, handler);
    }

    class Builder {
        private final List<RouterFunction> functions = new ArrayList<>();

        public Builder GET(String pattern, HandlerFunction handler) {
            functions.add(of(RequestPredicate.GET(pattern), handler));
            return this;
        }

        public Builder POST(String pattern, HandlerFunction handler) {
            functions.add(of(RequestPredicate.POST(pattern), handler));
            return this;
        }

        public Builder PUT(String pattern, HandlerFunction handler) {
            functions.add(of(RequestPredicate.PUT(pattern), handler));
            return this;
        }

        public Builder DELETE(String pattern, HandlerFunction handler) {
            functions.add(of(RequestPredicate.DELETE(pattern), handler));
            return this;
        }

        public Builder PATCH(String pattern, HandlerFunction handler) {
            functions.add(of(RequestPredicate.PATCH(pattern), handler));
            return this;
        }

        public Builder add(RequestPredicate predicate, HandlerFunction handler) {
            functions.add(of(predicate, handler));
            return this;
        }

        public RouterFunction build() {
            RouterFunction result = request -> Mono.empty();
            for (int i = functions.size() - 1; i >= 0; i--) {
                result = functions.get(i).and(result);
            }
            return result;
        }
    }

    class DefaultRouterFunction implements RouterFunction {
        private final RequestPredicate predicate;
        private final HandlerFunction handler;

        public DefaultRouterFunction(RequestPredicate predicate, HandlerFunction handler) {
            this.predicate = predicate;
            this.handler = handler;
        }

        @Override
        public Mono<HandlerFunction> route(ServerRequest request) {
            if (predicate.test(request)) {
                extractPathVariables(request);
                return Mono.just(handler);
            }
            return Mono.empty();
        }

        private void extractPathVariables(ServerRequest request) {
            String pattern = predicate.toString();
        }
    }
}
