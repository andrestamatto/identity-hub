package br.dev.andrestamatto.identityhub.clientapplication.domain;

import java.util.Arrays;

public enum MachineClientScope {
    MEMBERSHIP_WRITE("membership:write");

    private final String value;

    MachineClientScope(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static MachineClientScope from(String value) {
        return Arrays.stream(values())
                .filter(scope -> scope.value.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported machine scope"));
    }
}
