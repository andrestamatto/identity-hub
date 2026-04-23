package br.dev.andrestamatto.identityhub.interfaces.rest.dto;

public record LoginResponse(
    String accessToken,
    String tokenType,
    long expiresIn
) implements AuthenticatableResponse {

    public LoginResponse(String accessToken, long expiresIn) {
        this(accessToken, "Bearer", expiresIn);
    }

}
