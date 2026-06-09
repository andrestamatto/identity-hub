package br.dev.andrestamatto.identityhub.domain.valueobjects;

import br.dev.andrestamatto.identityhub.domain.exceptions.VerificationTokenException;

import java.time.Instant;

public record VerificationToken(
    String code,
    NotificationMethod method,
    Instant expiresAt
) {

    public VerificationToken {
        if (code == null || code.isBlank()) { throw new IllegalArgumentException("code must not be null or blank"); }
        if (method == null) { throw new IllegalArgumentException("method must not be null"); }
        if (expiresAt == null) { throw new IllegalArgumentException("expiresAt must not be null"); }
    }

    public boolean isExpiredAt(Instant now) {
        if (now == null) throw new IllegalArgumentException("now is required");
        return now.isAfter(expiresAt);
    }

    public boolean matches(String givenCode) {
        return code.equals(givenCode);
    }

    public static boolean validateCode(VerificationToken userVerificationToken, String givenCode, Instant instant) {

        if ( userVerificationToken.isExpiredAt(instant) ) {
            throw new VerificationTokenException("Verification token has expired");
        }

        if ( !userVerificationToken.matches(givenCode) ) {
            throw new VerificationTokenException("Verification code mismatch");
        }

        return true;
    }

}
