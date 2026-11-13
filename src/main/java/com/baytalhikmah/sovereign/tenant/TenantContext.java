package com.baytalhikmah.sovereign.tenant;

import java.util.function.Supplier;

/** Which shop (tenant) the work on this thread belongs to. "public" is the platform itself. */
public final class TenantContext {

    public static final String PLATFORM = "public";

    private static final ThreadLocal<String> TENANT = ThreadLocal.withInitial(() -> PLATFORM);

    private TenantContext() {
    }

    public static String get() {
        return TENANT.get();
    }

    public static <T> T callAs(String tenant, Supplier<T> work) {
        String previous = TENANT.get();
        TENANT.set(tenant);
        try {
            return work.get();
        }
        finally {
            TENANT.set(previous);
        }
    }

    /** Sets the tenant until the returned scope is closed; use it in try-with-resources. */
    public static Scope enter(String tenant) {
        String previous = TENANT.get();
        TENANT.set(tenant);
        return () -> TENANT.set(previous);
    }

    /** A tenant setting that ends when it is closed. */
    public interface Scope extends AutoCloseable {

        @Override
        void close();

    }

    public static void runAs(String tenant, Runnable work) {
        callAs(tenant, () -> {
            work.run();
            return null;
        });
    }

}
