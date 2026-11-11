package com.baytalhikmah.sovereign.audit;

import java.util.function.Supplier;

/** Who is doing the work on this thread: a user, a job or a service. "system" when nobody said. */
public final class CurrentActor {

    private static final ThreadLocal<String> ACTOR = ThreadLocal.withInitial(() -> "system");

    private CurrentActor() {
    }

    public static String get() {
        return ACTOR.get();
    }

    public static <T> T callAs(String actor, Supplier<T> work) {
        String previous = ACTOR.get();
        ACTOR.set(actor);
        try {
            return work.get();
        } finally {
            ACTOR.set(previous);
        }
    }

    public static void runAs(String actor, Runnable work) {
        callAs(actor, () -> {
            work.run();
            return null;
        });
    }

}
