package br.dev.andrestamatto.identityhub.domain.valueobjects;

public enum UsernameType {
    EMAIL {
        @Override
        public boolean validate(String value) {
            return value != null && value.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$");
        }
    },
    PHONE {
        @Override
        public boolean validate(String value) {
            return value != null && value.matches("^\\+?[1-9]\\d{7,14}$");
        }
    },
    EXTERNAL_ID {
        @Override
        public boolean validate(String value) {
            return value != null && !value.isBlank();
        }
    };

    // Abstract method that each enum instance must implement.
    public abstract boolean validate(String value);
}
