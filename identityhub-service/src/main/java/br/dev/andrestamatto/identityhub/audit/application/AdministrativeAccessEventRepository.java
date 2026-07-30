package br.dev.andrestamatto.identityhub.audit.application;

public interface AdministrativeAccessEventRepository {

    void append(AdministrativeAccessEvent event);
}
