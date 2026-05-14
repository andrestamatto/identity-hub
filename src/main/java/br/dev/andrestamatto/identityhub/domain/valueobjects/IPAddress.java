package br.dev.andrestamatto.identityhub.domain.valueobjects;

public record IPAddress(
        RawIPAddress rawIPAddress,
        IPVersionType ipVersionType,
        String hostName
) {
}
