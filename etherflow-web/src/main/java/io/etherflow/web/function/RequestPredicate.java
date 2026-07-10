package io.etherflow.web.function;

import io.etherflow.http.HttpMethod;

import java.util.Objects;

@FunctionalInterface
public interface RequestPredicate {

    boolean test(ServerRequest request);

    default RequestPredicate and(RequestPredicate other) {
        Objects.requireNonNull(other);
        return request -> this.test(request) && other.test(request);
    }

    default RequestPredicate or(RequestPredicate other) {
        Objects.requireNonNull(other);
        return request -> this.test(request) || other.test(request);
    }

    default RequestPredicate negate() {
        return request -> !this.test(request);
    }

    static RequestPredicate method(HttpMethod method) {
        return request -> request.method() == method;
    }

    static RequestPredicate path(String pattern) {
        return request -> {
            String requestPath = request.path();
            if (pattern.endsWith("/**")) {
                String prefix = pattern.substring(0, pattern.length() - 3);
                return requestPath.startsWith(prefix);
            }
            if (pattern.contains("{") && pattern.contains("}")) {
                return matchPath(pattern, requestPath);
            }
            return requestPath.equals(pattern);
        };
    }

    static RequestPredicate GET(String pattern) {
        return method(HttpMethod.GET).and(path(pattern));
    }

    static RequestPredicate POST(String pattern) {
        return method(HttpMethod.POST).and(path(pattern));
    }

    static RequestPredicate PUT(String pattern) {
        return method(HttpMethod.PUT).and(path(pattern));
    }

    static RequestPredicate DELETE(String pattern) {
        return method(HttpMethod.DELETE).and(path(pattern));
    }

    static RequestPredicate PATCH(String pattern) {
        return method(HttpMethod.PATCH).and(path(pattern));
    }

    static RequestPredicate all() {
        return request -> true;
    }

    static RequestPredicate accept(String mediaType) {
        return request -> {
            String accept = request.headers().getFirst("accept");
            return accept != null && accept.contains(mediaType);
        };
    }

    static RequestPredicate contentType(String mediaType) {
        return request -> {
            String ct = request.headers().getFirst("content-type");
            return ct != null && ct.contains(mediaType);
        };
    }

    private static boolean matchPath(String pattern, String requestPath) {
        String[] patternParts = pattern.split("/");
        String[] pathParts = requestPath.split("/");

        if (patternParts.length != pathParts.length) {
            return false;
        }

        for (int i = 0; i < patternParts.length; i++) {
            if (patternParts[i].startsWith("{") && patternParts[i].endsWith("}")) {
                continue;
            }
            if (!patternParts[i].equals(pathParts[i])) {
                return false;
            }
        }
        return true;
    }
}
