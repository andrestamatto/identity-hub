package br.dev.andrestamatto.identityhub.domain.valueobjects;

public record EncodedPassword(
        String value
) {

    private static final int MINIMUM_VALUE_SIZE_ALLOWED = 20;

    public EncodedPassword {
        if ( value == null || value.isBlank() ) { throw new IllegalArgumentException("value for EncodedPassword is required."); }
        if ( value.length() < MINIMUM_VALUE_SIZE_ALLOWED ) { throw new IllegalArgumentException("Invalid value for EncodedPassword."); }
    }

}
