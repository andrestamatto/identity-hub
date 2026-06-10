package br.dev.andrestamatto.identityhub.application.ports.input.command;

public record ConfirmUserCommand(
    String username,
    String verificationCode
){

    public ConfirmUserCommand {
        if (username == null || username.isBlank()) { throw new  IllegalArgumentException("Username cannot be null or blank"); }
        if (verificationCode == null || verificationCode.isBlank()) { throw new  IllegalArgumentException("Verification token cannot be null or blank"); }
    }

}
