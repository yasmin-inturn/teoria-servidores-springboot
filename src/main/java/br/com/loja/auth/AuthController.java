package br.com.loja.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.loja.auth.AuthDtos.CadastroRequest;
import br.com.loja.auth.AuthDtos.LoginRequest;
import br.com.loja.auth.AuthDtos.TokenResponse;
import br.com.loja.auth.AuthDtos.UsuarioResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService servico;

    public AuthController(AuthService servico) {
        this.servico = servico;
    }

    @PostMapping("/registrar")
    public ResponseEntity<UsuarioResponse> registrar(@Valid @RequestBody CadastroRequest requisicao) {
        return ResponseEntity.status(201).body(UsuarioResponse.de(servico.cadastrar(requisicao)));
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest requisicao) {
        return TokenResponse.de(servico.autenticar(requisicao));
    }

    /**
     * Quem sou eu, segundo o token enviado. O parametro Jwt e preenchido pelo
     * Spring Security depois de validar a assinatura: se chegou aqui, o token e
     * autentico e esta no prazo.
     */
    @GetMapping("/eu")
    public UsuarioResponse eu(@AuthenticationPrincipal Jwt jwt) {
        return UsuarioResponse.de(servico.porEmail(jwt.getSubject()));
    }
}
