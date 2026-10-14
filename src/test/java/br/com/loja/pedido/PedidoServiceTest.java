package br.com.loja.pedido;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import br.com.loja.catalogo.ProdutoRepository;
import br.com.loja.common.EstoqueInsuficienteException;
import br.com.loja.pagamento.MetodoPagamento;
import br.com.loja.pagamento.PagamentoRecusadoException;
import br.com.loja.pagamento.StatusPagamento;
import br.com.loja.pedido.PedidoDtos.ItemRequest;
import br.com.loja.pedido.PedidoDtos.PedidoRequest;

/**
 * Teste de integracao do checkout: sobe o contexto inteiro (Tomcat de fora,
 * banco H2 de verdade) para poder observar o efeito das TRANSACOES.
 *
 * Importante: a classe NAO e @Transactional. Se fosse, o Spring abriria uma
 * transacao em volta de cada teste e faria rollback no fim - e aqui queremos
 * justamente verificar o que o servico comita e o que ele desfaz.
 */
@SpringBootTest
class PedidoServiceTest {

    private static final String CLIENTE = "cliente@loja.com";
    private static final String CEP_SP = "01310-100";

    @Autowired
    private PedidoService pedidos;

    @Autowired
    private ProdutoRepository produtos;

    @Test
    @DisplayName("pedido pago com PIX baixa estoque e nasce PAGO")
    void checkoutFeliz() {
        int estoqueAntes = estoqueDe("MOU-LOG-02");

        Pedido pedido = pedidos.criar(CLIENTE, new PedidoRequest(
                List.of(new ItemRequest("MOU-LOG-02", 2)), CEP_SP, MetodoPagamento.PIX, null, null));

        assertThat(pedido.getNumero()).startsWith("PED-");
        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.PAGO);
        assertThat(pedido.getPagamento().getStatus()).isEqualTo(StatusPagamento.AUTORIZADO);
        assertThat(pedido.getItens()).hasSize(1);

        // subtotal = 2 * 159.90 = 319.80 -> acima de 299, frete gratis
        assertThat(pedido.getSubtotal()).isEqualByComparingTo("319.80");
        assertThat(pedido.getFrete()).isEqualByComparingTo("0.00");
        assertThat(pedido.getTotal()).isEqualByComparingTo("319.80");

        assertThat(estoqueDe("MOU-LOG-02")).isEqualTo(estoqueAntes - 2);
    }

    @Test
    @DisplayName("boleto deixa o pedido aguardando pagamento, mas ja reserva o estoque")
    void checkoutComBoleto() {
        int estoqueAntes = estoqueDe("CAB-USBC-2M");

        Pedido pedido = pedidos.criar(CLIENTE, new PedidoRequest(
                List.of(new ItemRequest("CAB-USBC-2M", 1)), CEP_SP, MetodoPagamento.BOLETO, null, null));

        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.AGUARDANDO_PAGAMENTO);
        assertThat(pedido.getPagamento().getStatus()).isEqualTo(StatusPagamento.PENDENTE);
        assertThat(estoqueDe("CAB-USBC-2M")).isEqualTo(estoqueAntes - 1);
    }

    @Test
    @DisplayName("SKUs repetidos sao somados antes de validar o estoque")
    void agrupaSkusRepetidos() {
        int estoqueAntes = estoqueDe("CAB-USBC-2M");

        Pedido pedido = pedidos.criar(CLIENTE, new PedidoRequest(
                List.of(new ItemRequest("CAB-USBC-2M", 2), new ItemRequest("CAB-USBC-2M", 3)),
                CEP_SP, MetodoPagamento.PIX, null, null));

        assertThat(pedido.getItens()).hasSize(1);
        assertThat(pedido.getItens().getFirst().getQuantidade()).isEqualTo(5);
        assertThat(estoqueDe("CAB-USBC-2M")).isEqualTo(estoqueAntes - 5);
    }

    @Test
    @DisplayName("estoque insuficiente: 409 e nada e gravado")
    void estoqueInsuficiente() {
        long pedidosAntes = contarPedidos();
        int estoqueAntes = estoqueDe("WEB-FHD-03"); // so 1 unidade na carga inicial

        assertThatThrownBy(() -> pedidos.criar(CLIENTE, new PedidoRequest(
                List.of(new ItemRequest("WEB-FHD-03", 5)), CEP_SP, MetodoPagamento.PIX, null, null)))
                .isInstanceOf(EstoqueInsuficienteException.class);

        assertThat(estoqueDe("WEB-FHD-03")).isEqualTo(estoqueAntes);
        assertThat(contarPedidos()).isEqualTo(pedidosAntes);
    }

    @Test
    @DisplayName("pagamento recusado desfaz a baixa de estoque (rollback)")
    void pagamentoRecusadoFazRollback() {
        long pedidosAntes = contarPedidos();
        int estoqueAntes = estoqueDe("SSD-1TB-NV");

        assertThatThrownBy(() -> pedidos.criar(CLIENTE, new PedidoRequest(
                List.of(new ItemRequest("SSD-1TB-NV", 1)), CEP_SP,
                MetodoPagamento.CARTAO_CREDITO, "4111111111110000", null)))
                .isInstanceOf(PagamentoRecusadoException.class);

        // Este e o teste que mostra a transacao funcionando: o estoque JA havia
        // sido baixado quando o pagamento falhou, e voltou sozinho.
        assertThat(estoqueDe("SSD-1TB-NV")).isEqualTo(estoqueAntes);
        assertThat(contarPedidos()).isEqualTo(pedidosAntes);
    }

    @Test
    @DisplayName("cancelar antes do envio devolve o estoque")
    void cancelamentoDevolveEstoque() {
        int estoqueAntes = estoqueDe("HEA-BT-05");

        Pedido pedido = pedidos.criar(CLIENTE, new PedidoRequest(
                List.of(new ItemRequest("HEA-BT-05", 2)), CEP_SP, MetodoPagamento.PIX, null, null));
        assertThat(estoqueDe("HEA-BT-05")).isEqualTo(estoqueAntes - 2);

        pedidos.mudarStatus(pedido.getNumero(), StatusPedido.CANCELADO);

        assertThat(estoqueDe("HEA-BT-05")).isEqualTo(estoqueAntes);
        assertThat(pedidos.buscar(pedido.getNumero(), CLIENTE, false).getStatus())
                .isEqualTo(StatusPedido.CANCELADO);
    }

    @Test
    @DisplayName("transicao de status invalida e rejeitada")
    void transicaoInvalida() {
        Pedido pedido = pedidos.criar(CLIENTE, new PedidoRequest(
                List.of(new ItemRequest("CAB-USBC-2M", 1)), CEP_SP, MetodoPagamento.PIX, null, null));

        // PAGO -> ENTREGUE pula separacao e envio.
        assertThatThrownBy(() -> pedidos.mudarStatus(pedido.getNumero(), StatusPedido.ENTREGUE))
                .isInstanceOf(br.com.loja.common.RegraDeNegocioException.class)
                .hasMessageContaining("Transicao de status invalida");
    }

    @Test
    @DisplayName("cliente nao enxerga pedido de outro cliente")
    void isolamentoEntreClientes() {
        Pedido pedido = pedidos.criar(CLIENTE, new PedidoRequest(
                List.of(new ItemRequest("CAB-USBC-2M", 1)), CEP_SP, MetodoPagamento.PIX, null, null));

        assertThatThrownBy(() -> pedidos.buscar(pedido.getNumero(), "outro@loja.com", false))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);

        // ...mas o administrador enxerga.
        assertThat(pedidos.buscar(pedido.getNumero(), "admin@loja.com", true)).isNotNull();
    }

    private int estoqueDe(String sku) {
        return produtos.findBySku(sku).orElseThrow().getQuantidadeEstoque();
    }

    private long contarPedidos() {
        return pedidos.listar("admin@loja.com", true).size();
    }
}
