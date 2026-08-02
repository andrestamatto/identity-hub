package br.dev.andrestamatto.identityhub.identity.application;

public interface GlobalAccountDisableOperationRepository {

    GlobalAccountDisableOperation findByIdempotencyKey(String key);

    void save(GlobalAccountDisableOperation operation);

    void lockGlobalAccountLifecycle();
}
