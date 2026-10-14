package br.com.loja.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracao de seguranca (prefixo loja.seguranca no application.yml).
 *
 * O segredo fica fora do codigo justamente para poder ser trocado por variavel
 * de ambiente em producao: LOJA_SEGURANCA_JWT_SEGREDO sobrescreve o valor do
 * arquivo, sem recompilar nada.
 */
@ConfigurationProperties(prefix = "loja.seguranca")
public record SegurancaProperties(
        /** Chave HMAC do JWT. Precisa de 32 bytes ou mais para HS256. */
        String jwtSegredo,
        long expiracaoMinutos,
        String emissor) {
}
