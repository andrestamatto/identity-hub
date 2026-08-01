package br.dev.andrestamatto.identityhub.identity.adapter.in.http;

public interface PublicResponseTiming {

    Scope begin();

    static PublicResponseTiming none() {
        return () -> () -> { };
    }

    interface Scope extends AutoCloseable {
        @Override
        void close();
    }
}
