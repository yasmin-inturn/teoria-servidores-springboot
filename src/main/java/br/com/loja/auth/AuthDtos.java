package br.com.loja.auth;

import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String senha) {
    }

    public record CadastroRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 72, message = "a senha precisa de 8 a 72 caracteres") String senha,
            @NotBlank @Size(max = 120) String nome) {
    }

    public record TokenResponse(
            String tipo,
            String token,
            Instant expiraEm,
            List<String> perfis) {

        public static TokenResponse de(JwtService.TokenEmitido emitido) {
            return new TokenResponse("Bearer", emitido.token(), emitido.expiraEm(), emitido.perfis());
        }
    }

    public record UsuarioResponse(Long id, String email, String nome, List<String> perfis) {

        public static UsuarioResponse de(Usuario usuario) {
            return new UsuarioResponse(usuario.getId(), usuario.getEmail(), usuario.getNome(),
                    usuario.getPerfis().stream().map(Perfil::name).sorted().toList());
        }
    }
}
