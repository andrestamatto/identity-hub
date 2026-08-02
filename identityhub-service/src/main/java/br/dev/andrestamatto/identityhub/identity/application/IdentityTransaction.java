package br.dev.andrestamatto.identityhub.identity.application;

import java.util.function.Supplier;
import java.util.concurrent.atomic.AtomicReference;

public interface IdentityTransaction {

    void execute(Runnable work);

    default <T> T execute(Supplier<T> work) {
        var result = new AtomicReference<T>();
        execute(() -> result.set(work.get()));
        return result.get();
    }
}
