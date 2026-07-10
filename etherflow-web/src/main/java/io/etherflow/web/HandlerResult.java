package io.etherflow.web;

import io.etherflow.core.Mono;

public class HandlerResult {

    private final Object handler;
    private final Mono<?> returnValue;

    public HandlerResult(Object handler, Mono<?> returnValue) {
        this.handler = handler;
        this.returnValue = returnValue;
    }

    public Object handler() {
        return handler;
    }

    public Mono<?> returnValue() {
        return returnValue;
    }
}
