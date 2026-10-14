package br.com.loja.pedido;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import br.com.loja.pagamento.MetodoPagamento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class PedidoDtos {

    private PedidoDtos() {
    }

    /**
     * Corpo do POST /api/pedidos.
     *
     * O @Valid dentro da lista faz o Bean Validation descer nos itens tambem:
     * um item com quantidade 0 derruba a requisicao com 400 antes de qualquer
     * consulta ao banco.
     */
    public record PedidoRequest(
            @NotEmpty(message = "informe ao menos um item") @Valid List<ItemRequest> itens,

            @NotBlank @Pattern(regexp = "\\d{5}-?\\d{3}", message = "use o formato 00000-000")
            String cepEntrega,

            @NotNull(message = "informe o metodo de pagamento")
            MetodoPagamento metodoPagamento,

            @Size(max = 25) String numeroCartao,

            @Size(max = 30) String cupom) {
    }

    public record ItemRequest(
            @NotBlank String sku,
            @Min(value = 1, message = "quantidade minima e 1") int quantidade) {
    }

    /** Checkout usando o que ja esta no carrinho do usuario autenticado. */
    public record CheckoutCarrinhoRequest(
            @NotBlank @Pattern(regexp = "\\d{5}-?\\d{3}", message = "use o formato 00000-000")
            String cepEntrega,

            @NotNull MetodoPagamento metodoPagamento,

            @Size(max = 25) String numeroCartao,

            @Size(max = 30) String cupom) {
    }

    public record MudancaStatusRequest(@NotNull StatusPedido novoStatus) {
    }

    public record PedidoResponse(
            String numero,
            String emailCliente,
            StatusPedido status,
            List<StatusPedido> proximosStatusPossiveis,
            List<ItemResponse> itens,
            String cepEntrega,
            BigDecimal subtotal,
            BigDecimal desconto,
            BigDecimal frete,
            BigDecimal total,
            int prazoEntregaDias,
            String detalheCalculo,
            PagamentoResponse pagamento,
            Instant criadoEm,
            Instant atualizadoEm) {

        public static PedidoResponse de(Pedido pedido) {
            return new PedidoResponse(
                    pedido.getNumero(),
                    pedido.getEmailCliente(),
                    pedido.getStatus(),
                    List.copyOf(pedido.getStatus().proximosPermitidos()),
                    pedido.getItens().stream().map(ItemResponse::de).toList(),
                    pedido.getCepEntrega(),
                    pedido.getSubtotal(),
                    pedido.getDesconto(),
                    pedido.getFrete(),
                    pedido.getTotal(),
                    pedido.getPrazoEntregaDias(),
                    pedido.getDetalheCalculo(),
                    PagamentoResponse.de(pedido),
                    pedido.getCriadoEm(),
                    pedido.getAtualizadoEm());
        }
    }

    public record ItemResponse(String sku, String nome, int quantidade,
                               BigDecimal precoUnitario, BigDecimal subtotal) {

        public static ItemResponse de(ItemPedido item) {
            return new ItemResponse(item.getSku(), item.getNome(), item.getQuantidade(),
                    item.getPrecoUnitario(), item.subtotal());
        }
    }

    public record PagamentoResponse(String metodo, String status, String identificador, String mensagem) {

        public static PagamentoResponse de(Pedido pedido) {
            var pagamento = pedido.getPagamento();
            return new PagamentoResponse(pagamento.getMetodo().name(), pagamento.getStatus().name(),
                    pagamento.getIdentificadorExterno(), pagamento.getMensagem());
        }
    }
}
