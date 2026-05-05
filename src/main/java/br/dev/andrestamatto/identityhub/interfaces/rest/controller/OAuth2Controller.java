package br.dev.andrestamatto.identityhub.interfaces.rest.controller;

import br.dev.andrestamatto.identityhub.application.usecase.port.in.SocialLoginUseCasePort;
import br.dev.andrestamatto.identityhub.interfaces.rest.dto.LoginResponse;
import br.dev.andrestamatto.identityhub.interfaces.rest.mapper.AuthResponseMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value="/oauth2")
public class OAuth2Controller {

    private final SocialLoginUseCasePort socialLoginUseCase;
    private final AuthResponseMapper authResponseMapper;

    public OAuth2Controller(SocialLoginUseCasePort socialLogin, AuthResponseMapper authResponseMapper) {
        this.socialLoginUseCase = socialLogin;
        this.authResponseMapper = authResponseMapper;
    }

    @GetMapping(value="/authorize/{provider}")
    public ResponseEntity<Void> authorize(@PathVariable String provider) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @GetMapping(value="/callback/{provider}")
    public ResponseEntity<LoginResponse> callback(
            @PathVariable String provider,
            @RequestParam(value = "code") String code,
            @RequestParam(value="redirectUri", required = false) String redirectUri
    ){
        var socialLoginResult = socialLoginUseCase.execute(provider, code, redirectUri);

        return ResponseEntity.ok().body(
                authResponseMapper.toLoginResponse(socialLoginResult)
        );
    }

}
