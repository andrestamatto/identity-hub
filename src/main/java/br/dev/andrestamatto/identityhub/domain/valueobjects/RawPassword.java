package br.dev.andrestamatto.identityhub.domain.valueobjects;

public record RawPassword(
        String value
) {

    private static final int MINIMUM_VALUE_SIZE_ALLOWED = 6;

    public RawPassword {
        if ( value == null || value.isBlank() ) { throw new IllegalArgumentException("value for RawPassword is required."); }
        if ( value.length() < MINIMUM_VALUE_SIZE_ALLOWED ) { throw new IllegalArgumentException("Invalid value for RawPassword."); }
    }

}
