package br.com.loja.config;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;

import br.com.loja.auth.SegurancaProperties;

/**
 * Configuracao do Spring Security.
 *
 * O ponto importante para entender o servidor: a seguranca aqui e uma CADEIA DE
 * FILTROS (servlet filters) que roda ANTES de qualquer @RestController. Quando
 * uma requisicao chega com "Authorization: Bearer ...", o filtro do resource
 * server valida a assinatura do JWT, monta um Authentication e o guarda no
 * SecurityContext da thread. So depois disso o Spring MVC escolhe o metodo do
 * controller. Se o token for invalido, o controller nunca e chamado.
 *
 * A API e stateless: nenhuma sessao em memoria, nenhum cookie de sessao. Isso e
 * o que permite subir N instancias do servidor sem sessao compartilhada.
 */
@Configuration
@EnableMethodSecurity // habilita @PreAuthorize nos controllers
public class SecurityConfig {

    private final SegurancaProperties parametros;

    public SecurityConfig(SegurancaProperties parametros) {
        this.parametros = parametros;
    }

    @Bean
    SecurityFilterChain filtros(HttpSecurity http) throws Exception {
        return http
                // Sem CSRF: nao usamos cookie de sessao, o token vai no cabecalho.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // O H2 console roda dentro de um frame; liberado so em dev.
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .authorizeHttpRequests(rotas -> rotas
                        // Documentacao e estaticos: publicos.
                        .requestMatchers("/", "/doc/**", "/css/**", "/js/**", "/favicon.ico").permitAll()
                        // Entrada no sistema.
                        .requestMatchers("/api/auth/login", "/api/auth/registrar").permitAll()
                        // Vitrine: qualquer um ve o catalogo...
                        .requestMatchers(HttpMethod.GET, "/api/produtos/**").permitAll()
                        // ...mas alterar catalogo exige ADMIN (reforcado por @PreAuthorize).
                        .requestMatchers("/api/produtos/**").hasRole("ADMIN")
                        // Saude para o orquestrador (Docker/Kubernetes), metricas para o admin.
                        .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        .requestMatchers("/h2-console/**").permitAll()
                        // Carrinho, pedidos e /api/auth/eu: precisa de token valido.
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(conversorDeToken())))
                .build();
    }

    /** BCrypt com custo 10 (padrao): hash lento de proposito, para frear ataque de forca bruta. */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private SecretKey chaveHmac() {
        byte[] bytes = parametros.jwtSegredo().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException(
                    "loja.seguranca.jwt-segredo precisa de pelo menos 32 bytes para HS256");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    /** Assina os tokens emitidos no login. */
    @Bean
    JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(chaveHmac()));
    }

    /** Valida assinatura e expiracao dos tokens que chegam nas requisicoes. */
    @Bean
    JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(chaveHmac())
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    /**
     * Traduz o claim "roles" do token em authorities do Spring Security.
     * Sem isto, o Spring esperaria o claim "scope"/"scp" e o hasRole('ADMIN')
     * nunca bateria.
     */
    private JwtAuthenticationConverter conversorDeToken() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(authorities);
        return conversor;
    }
}
