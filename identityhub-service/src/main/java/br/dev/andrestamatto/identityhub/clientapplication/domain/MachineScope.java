package br.dev.andrestamatto.identityhub.clientapplication.domain;

import java.util.Arrays;

public enum MachineScope {
    ONBOARDING_WRITE("onboarding:write");

    private final String value;

    MachineScope(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static MachineScope from(String value) {
        return Arrays.stream(values())
                .filter(scope -> scope.value.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported machine scope"));
    }
}
