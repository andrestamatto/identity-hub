package br.dev.andrestamatto.identityhub.domain.valueobjects;

public record LoginData(
        Username username,
        RawPassword rawPassword
) {
    public LoginData {
        if (username == null || rawPassword == null) {
            throw new IllegalArgumentException("Username and rawPassword are required");
        }
    }
}
