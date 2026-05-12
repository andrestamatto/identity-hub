package br.dev.andrestamatto.identityhub.domain.valueobjects;

public enum UsernameType {
    EMAIL {
        @Override
        public boolean validate(String value) {
            return value != null && value.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$");
        }
    },
    CPF {
        @Override
        public boolean validate(String value) {
            // TODO: validate the last two digits.
            return value != null && value.matches("\\d{11}");
        }
    },
    SSN {
        @Override
        public boolean validate(String value) {
            return value != null && value.matches("\\d{3}-\\d{2}-\\d{4}");
        }
    },
    ID {
        @Override
        public boolean validate(String value) {
            return value != null && !value.isBlank();
        }
    };

    // Abstract method that each enum instance must implement.
    public abstract boolean validate(String value);
}