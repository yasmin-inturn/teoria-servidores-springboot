package br.com.loja.carrinho;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public final class CarrinhoDtos {

    private CarrinhoDtos() {
    }

    public record AdicionarItemRequest(
            @NotBlank String sku,
            @Min(value = 1, message = "quantidade minima e 1") int quantidade) {
    }

    public record QuantidadeRequest(@Min(0) int quantidade) {
    }

    public record CarrinhoResponse(
            String emailUsuario,
            List<ItemResponse> itens,
            int quantidadeTotal,
            BigDecimal subtotal,
            BigDecimal pesoTotalKg,
            Instant atualizadoEm) {

        public static CarrinhoResponse de(Carrinho carrinho) {
            return new CarrinhoResponse(
                    carrinho.getEmailUsuario(),
                    carrinho.getItens().stream().map(ItemResponse::de).toList(),
                    carrinho.quantidadeTotal(),
                    carrinho.subtotal(),
                    carrinho.pesoTotal(),
                    carrinho.getAtualizadoEm());
        }
    }

    public record ItemResponse(String sku, String nome, int quantidade,
                               BigDecimal precoUnitario, BigDecimal subtotal, int estoqueDisponivel) {

        public static ItemResponse de(ItemCarrinho item) {
            return new ItemResponse(
                    item.getProduto().getSku(),
                    item.getProduto().getNome(),
                    item.getQuantidade(),
                    item.getProduto().getPreco(),
                    item.subtotal(),
                    item.getProduto().getQuantidadeEstoque());
        }
    }

    /**
     * Previa do checkout: roda as mesmas regras de desconto e frete do pedido,
     * sem gravar nada. O front-end usa isso para mostrar o total antes do
     * cliente confirmar.
     */
    public record SimulacaoResponse(
            BigDecimal subtotal,
            BigDecimal desconto,
            String descricaoDesconto,
            BigDecimal frete,
            String regiaoFrete,
            int prazoEntregaDias,
            BigDecimal total) {
    }
}
