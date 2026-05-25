package br.dev.andrestamatto.identityhub.interfaces.rest;


import br.dev.andrestamatto.identityhub.application.ports.input.command.ConfirmUserCommand;
import br.dev.andrestamatto.identityhub.application.ports.input.command.RegisterUserCommand;
import br.dev.andrestamatto.identityhub.application.usecase.ConfirmUser;
import br.dev.andrestamatto.identityhub.application.usecase.RegisterUser;
import br.dev.andrestamatto.identityhub.interfaces.rest.mapper.UserResponseMapper;
import br.dev.andrestamatto.identityhub.interfaces.rest.response.RegisteredUserResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/users")
public class UserController {

    private final RegisterUser registerUser;
    private final ConfirmUser confirmUser;
    private final UserResponseMapper userResponseMapper;

    public UserController(RegisterUser registerUser, ConfirmUser confirmUser, UserResponseMapper userResponseMapper) {
        this.registerUser = registerUser;
        this.confirmUser = confirmUser;
        this.userResponseMapper = userResponseMapper;
    }

    @PostMapping(value="/register", consumes = "application/json")
    public ResponseEntity<RegisteredUserResponse> register(@RequestBody RegisterUserCommand registerUserCommand) {

        var userResponse = Optional.of(
                userResponseMapper.registeredUserResponseFrom(
                    registerUser.execute(registerUserCommand)
                )
        ).orElseThrow();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userResponse);
    }

    @GetMapping(value="/confirm")
    public ResponseEntity<Void> confirm(@RequestParam String username, @RequestParam String code) {

        ConfirmUserCommand confirmUserCommand = new ConfirmUserCommand(username, code);

        confirmUser.execute(confirmUserCommand);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // TODO: Create PUT "/resend-code"

}
