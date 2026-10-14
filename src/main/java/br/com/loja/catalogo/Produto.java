package br.com.loja.catalogo;

import java.math.BigDecimal;

/**
 * Um produto do catálogo.
 *
 * É um record: a tipagem forte do Java garante, em tempo de compilação, que
 * todo produto tem nome, preço e estoque - nada de campo esquecido só
 * descoberto em produção.
 */
public record Produto(Long id, String nome, BigDecimal preco, int estoque) {
}
