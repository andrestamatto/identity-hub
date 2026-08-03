package br.dev.andrestamatto.identityhub.clientapplication.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public record MachineSettings(List<MachineClientScope> scopes)
        implements ApplicationClientSettings {

    public MachineSettings {
        scopes = List.copyOf(Objects.requireNonNull(scopes));
        if (new HashSet<>(scopes).size() != scopes.size()) {
            throw new IllegalArgumentException("Machine scopes must be unique");
        }
    }

    public MachineSettings() {
        this(List.of());
    }

    @Override
    public ApplicationClientType type() {
        return ApplicationClientType.MACHINE;
    }
}
