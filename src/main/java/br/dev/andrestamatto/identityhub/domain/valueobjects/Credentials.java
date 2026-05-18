package br.dev.andrestamatto.identityhub.domain.valueobjects;

/**
 * Credentials representa os dados de autenticação recebidos no login.
 * Exemplo: username "user@example.com" + rawPassword "123456".
 * Este VO existe apenas para entrada de autenticação; senha em claro não deve ser persistida.
 */
public record Credentials(
        Username username,
        RawPassword rawPassword
) {
    public Credentials {
        if (username == null || rawPassword == null) {
            throw new IllegalArgumentException("Username and rawPassword are required");
        }
    }

    public static Credentials create(String username,  String rawPassword) {
        return new Credentials(
                new Username(username),
                new RawPassword(rawPassword)
        );
    }

}
