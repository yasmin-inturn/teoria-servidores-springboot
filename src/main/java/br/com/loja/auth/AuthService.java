package br.com.loja.auth;

import java.util.EnumSet;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.loja.common.CredenciaisInvalidasException;
import br.com.loja.common.RecursoNaoEncontradoException;
import br.com.loja.common.RegraDeNegocioException;

/**
 * Cadastro e login.
 *
 * A comparacao de senha passa pelo PasswordEncoder (BCrypt): o hash guardado
 * inclui o salt e o custo, e matches() faz a comparacao em tempo constante.
 * Nunca ha igualdade de String entre senhas.
 */
@Service
public class AuthService {

    private final UsuarioRepository repositorio;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository repositorio, PasswordEncoder encoder, JwtService jwtService) {
        this.repositorio = repositorio;
        this.encoder = encoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public Usuario cadastrar(AuthDtos.CadastroRequest requisicao) {
        if (repositorio.existsByEmailIgnoreCase(requisicao.email())) {
            throw new RegraDeNegocioException("Email ja cadastrado: " + requisicao.email());
        }
        Usuario usuario = new Usuario(requisicao.email(), encoder.encode(requisicao.senha()),
                requisicao.nome(), EnumSet.of(Perfil.CLIENTE));
        return repositorio.save(usuario);
    }

    @Transactional(readOnly = true)
    public JwtService.TokenEmitido autenticar(AuthDtos.LoginRequest requisicao) {
        Usuario usuario = repositorio.findByEmailIgnoreCase(requisicao.email())
                .orElseThrow(CredenciaisInvalidasException::new);

        // Mensagem generica de proposito: dizer "senha errada" revelaria que o
        // email existe, o que ajuda ataques de enumeracao de usuarios.
        if (!usuario.isAtivo() || !encoder.matches(requisicao.senha(), usuario.getSenhaHash())) {
            throw new CredenciaisInvalidasException();
        }
        return jwtService.gerarPara(usuario);
    }

    @Transactional(readOnly = true)
    public Usuario porEmail(String email) {
        return repositorio.findByEmailIgnoreCase(email)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Usuario", email));
    }
}
