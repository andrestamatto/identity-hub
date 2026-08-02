package br.dev.andrestamatto.identityhub.clientapplication.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public record MachineSettings(List<MachineScope> scopes) implements ApplicationClientSettings {

    public MachineSettings {
        scopes = List.copyOf(Objects.requireNonNull(scopes));
        if (new HashSet<>(scopes).size() != scopes.size()) {
            throw new IllegalArgumentException("Duplicate machine scope");
        }
    }

    public MachineSettings() {
        this(List.of());
    }

    public static MachineSettings create(List<String> scopes) {
        Objects.requireNonNull(scopes);
        return new MachineSettings(scopes.stream().map(MachineScope::from).toList());
    }

    public List<String> scopeValues() {
        return scopes.stream().map(MachineScope::value).toList();
    }

    @Override
    public ApplicationClientType type() {
        return ApplicationClientType.MACHINE;
    }
}
