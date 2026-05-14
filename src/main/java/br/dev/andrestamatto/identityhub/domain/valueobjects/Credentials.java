package br.dev.andrestamatto.identityhub.domain.valueobjects;

public record Credentials(
        Username username,
        RawPassword rawPassword
) {
    public Credentials {
        if (username == null || rawPassword == null) {
            throw new IllegalArgumentException("Username and rawPassword are required");
        }
    }
}
