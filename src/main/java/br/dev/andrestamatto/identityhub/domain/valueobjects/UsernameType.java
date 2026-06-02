package br.dev.andrestamatto.identityhub.domain.valueobjects;

import java.util.Optional;

public enum UsernameType {
    EMAIL {
        @Override
        public boolean validate(String value) {
            return Optional.ofNullable(value)
                    .map((nonNullValue) -> nonNullValue.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$"))
                    .orElse(false);
        }
    },
    PHONE {
        @Override
        public boolean validate(String value) {
            return Optional.ofNullable(value)
                    .map((nonNullValue) -> nonNullValue.matches("^\\+?[1-9]\\d{7,14}$"))
                    .orElse(false);
        }
    },
    EMAIL_OR_PHONE {
        @Override
        public boolean validate(String value) {
            return Optional.ofNullable(value)
                    .map((nonNullValue) -> (nonNullValue.matches("^\\+?[1-9]\\d{7,14}$") || nonNullValue.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")))
                    .orElse(false);
        }
    },
    EXTERNAL_ID {
        @Override
        public boolean validate(String value) {
            return value != null && !value.isBlank();
        }
    },
    UNKNOWN {
        @Override
        public boolean validate(String value) {
            return true;
        }
    };

    public static UsernameType create(String value) {
        return UsernameType.valueOf(value.toUpperCase());
    }

    // Abstract method that each enum instance must implement.
    public abstract boolean validate(String value);
}
