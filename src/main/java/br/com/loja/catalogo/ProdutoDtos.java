package br.com.loja.catalogo;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTOs do catalogo. Usar records separados da entidade evita expor o modelo de
 * banco na API: o cliente nunca ve o campo de versao nem consegue alterar o
 * estoque por um PUT de produto.
 */
public final class ProdutoDtos {

    private ProdutoDtos() {
    }

    /** Entrada validada pelo Bean Validation antes de chegar ao servico. */
    public record ProdutoRequest(
            @NotBlank @Pattern(regexp = "[A-Z0-9-]{3,40}", message = "use letras maiusculas, numeros e hifen")
            String sku,

            @NotBlank @Size(max = 120)
            String nome,

            @Size(max = 400)
            String descricao,

            @NotBlank @Size(max = 40)
            String categoria,

            @NotNull @DecimalMin(value = "0.01", message = "preco deve ser maior que zero")
            BigDecimal preco,

            @Min(value = 0, message = "estoque nao pode ser negativo")
            int quantidadeEstoque,

            @NotNull @DecimalMin(value = "0.001", message = "peso deve ser maior que zero")
            BigDecimal pesoKg) {
    }

    public record ProdutoResponse(
            Long id,
            String sku,
            String nome,
            String descricao,
            String categoria,
            BigDecimal preco,
            int quantidadeEstoque,
            BigDecimal pesoKg,
            boolean disponivel) {

        public static ProdutoResponse de(Produto produto) {
            return new ProdutoResponse(produto.getId(), produto.getSku(), produto.getNome(),
                    produto.getDescricao(), produto.getCategoria(), produto.getPreco(),
                    produto.getQuantidadeEstoque(), produto.getPesoKg(),
                    produto.isAtivo() && produto.getQuantidadeEstoque() > 0);
        }
    }

    public record ReposicaoRequest(@Min(1) int quantidade) {
    }
}
