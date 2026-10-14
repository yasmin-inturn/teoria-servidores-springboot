package br.com.loja.auth;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * Emissao do token JWT.
 *
 * O token carrega quem e o usuario (subject) e o que ele pode fazer (claim
 * "roles"), assinado com HMAC-SHA256. Por isso o servidor nao guarda sessao: a
 * cada requisicao ele valida a assinatura e remonta a identidade do zero. E o
 * que permite rodar varias instancias atras de um load balancer sem sessao
 * compartilhada - ponto central quando o projeto virar microsservicos.
 */
@Service
public class JwtService {

    private final JwtEncoder encoder;
    private final SegurancaProperties parametros;

    public JwtService(JwtEncoder encoder, SegurancaProperties parametros) {
        this.encoder = encoder;
        this.parametros = parametros;
    }

    public TokenEmitido gerarPara(Usuario usuario) {
        Instant agora = Instant.now();
        Instant expiraEm = agora.plus(parametros.expiracaoMinutos(), ChronoUnit.MINUTES);

        List<String> perfis = usuario.getPerfis().stream().map(Perfil::name).sorted().toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(parametros.emissor())
                .issuedAt(agora)
                .expiresAt(expiraEm)
                .subject(usuario.getEmail())
                .claim("nome", usuario.getNome())
                .claim("roles", perfis)
                .build();

        JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();
        String valor = encoder.encode(JwtEncoderParameters.from(cabecalho, claims)).getTokenValue();

        return new TokenEmitido(valor, expiraEm, perfis);
    }

    public record TokenEmitido(String token, Instant expiraEm, List<String> perfis) {
    }
}
