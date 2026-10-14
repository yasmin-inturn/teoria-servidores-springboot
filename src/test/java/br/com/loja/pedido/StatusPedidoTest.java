package br.com.loja.pedido;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StatusPedidoTest {

    @Test
    @DisplayName("o caminho feliz avanca um passo por vez")
    void caminhoFeliz() {
        assertThat(StatusPedido.AGUARDANDO_PAGAMENTO.podeIrPara(StatusPedido.PAGO)).isTrue();
        assertThat(StatusPedido.PAGO.podeIrPara(StatusPedido.EM_SEPARACAO)).isTrue();
        assertThat(StatusPedido.EM_SEPARACAO.podeIrPara(StatusPedido.ENVIADO)).isTrue();
        assertThat(StatusPedido.ENVIADO.podeIrPara(StatusPedido.ENTREGUE)).isTrue();
    }

    @Test
    @DisplayName("nao da para pular etapas nem voltar no tempo")
    void transicoesProibidas() {
        assertThat(StatusPedido.AGUARDANDO_PAGAMENTO.podeIrPara(StatusPedido.ENVIADO)).isFalse();
        assertThat(StatusPedido.ENTREGUE.podeIrPara(StatusPedido.PAGO)).isFalse();
        assertThat(StatusPedido.ENVIADO.podeIrPara(StatusPedido.CANCELADO)).isFalse();
    }

    @Test
    @DisplayName("entregue e cancelado sao estados finais")
    void estadosFinais() {
        assertThat(StatusPedido.ENTREGUE.finalizado()).isTrue();
        assertThat(StatusPedido.CANCELADO.finalizado()).isTrue();
        assertThat(StatusPedido.ENTREGUE.proximosPermitidos()).isEmpty();
        assertThat(StatusPedido.CANCELADO.proximosPermitidos()).isEmpty();
    }

    @Test
    @DisplayName("cancelar devolve estoque so antes do envio")
    void devolucaoDeEstoque() {
        assertThat(StatusPedido.AGUARDANDO_PAGAMENTO.devolveEstoqueAoCancelar()).isTrue();
        assertThat(StatusPedido.PAGO.devolveEstoqueAoCancelar()).isTrue();
        assertThat(StatusPedido.EM_SEPARACAO.devolveEstoqueAoCancelar()).isTrue();
        assertThat(StatusPedido.ENVIADO.devolveEstoqueAoCancelar()).isFalse();
    }
}
