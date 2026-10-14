package br.com.loja.catalogo;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Corpo do POST /api/produtos, validado pelo Bean Validation antes de chegar ao serviço. */
public record NovoProdutoRequest(
        @NotBlank String nome,
        @NotNull @DecimalMin(value = "0.01", message = "preço deve ser maior que zero") BigDecimal preco,
        @Min(value = 0, message = "estoque não pode ser negativo") int estoque) {
}
