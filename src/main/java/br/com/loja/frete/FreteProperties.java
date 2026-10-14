package br.com.loja.frete;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Parametros de frete lidos do application.yml (prefixo loja.frete).
 *
 * Trocar uma regra comercial (ex.: o valor minimo para frete gratis) passa a ser
 * mudanca de configuracao, nao de codigo. O @ConfigurationProperties ainda da
 * tipagem forte: se alguem escrever "duzentos" no lugar de um numero, a
 * aplicacao nao sobe.
 */
@ConfigurationProperties(prefix = "loja.frete")
public record FreteProperties(
        BigDecimal valorMinimoFreteGratis,
        BigDecimal custoPorKg,
        BigDecimal taxaManuseio,
        int prazoBaseDias) {
}
