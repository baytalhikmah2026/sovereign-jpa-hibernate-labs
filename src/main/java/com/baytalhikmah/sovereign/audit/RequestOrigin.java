package com.baytalhikmah.sovereign.audit;

/** The network address the current request came from, for this thread only. Empty outside a request. */
public final class RequestOrigin {

    private static final ThreadLocal<String> CLIENT_IP = new ThreadLocal<>();

    private RequestOrigin() {
    }

    public static String clientIp() {
        return CLIENT_IP.get();
    }

    /** Sets the address until the returned scope is closed, then restores the previous one. */
    public static Scope enter(String clientIp) {
        String previous = CLIENT_IP.get();
        CLIENT_IP.set(clientIp);
        return () -> {
            if (previous == null) {
                CLIENT_IP.remove();
            }
            else {
                CLIENT_IP.set(previous);
            }
        };
    }

    @FunctionalInterface
    public interface Scope extends AutoCloseable {

        @Override
        void close();

    }

}
