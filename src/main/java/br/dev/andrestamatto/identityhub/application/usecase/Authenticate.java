package br.dev.andrestamatto.identityhub.application.usecase;

public interface Authenticate {
    String execute(String email, String password);
}
