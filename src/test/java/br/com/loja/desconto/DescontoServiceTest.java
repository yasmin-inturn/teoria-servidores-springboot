package br.com.loja.desconto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.loja.common.RegraDeNegocioException;

class DescontoServiceTest {

    private final DescontoService servico = new DescontoService();

    @Test
    @DisplayName("sem cupom e com poucos itens nao ha desconto")
    void semDesconto() {
        var desconto = servico.calcular(new BigDecimal("200.00"), 2, null);

        assertThat(desconto.valor()).isEqualByComparingTo("0.00");
        assertThat(desconto.freteGratis()).isFalse();
    }

    @Test
    @DisplayName("cupom percentual respeita o teto absoluto do cupom")
    void cupomComTeto() {
        // BLACK20 = 20% limitado a R$ 200: 20% de 5000 seria 1000.
        var desconto = servico.calcular(new BigDecimal("5000.00"), 1, "BLACK20");

        assertThat(desconto.valor()).isEqualByComparingTo("200.00");
    }

    @Test
    @DisplayName("cupom em minusculas e com espaco tambem vale")
    void cupomNormalizado() {
        var desconto = servico.calcular(new BigDecimal("100.00"), 1, "  primeiracompra ");

        assertThat(desconto.valor()).isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("FRETEGRATIS nao desconta valor, so libera o frete")
    void cupomDeFrete() {
        var desconto = servico.calcular(new BigDecimal("100.00"), 1, "FRETEGRATIS");

        assertThat(desconto.valor()).isEqualByComparingTo("0.00");
        assertThat(desconto.freteGratis()).isTrue();
    }

    @Test
    @DisplayName("desconto por volume soma com o cupom")
    void volumeMaisCupom() {
        // 10 itens -> 5%; PRIMEIRACOMPRA -> 10% limitado a R$ 50.
        var desconto = servico.calcular(new BigDecimal("400.00"), 10, "PRIMEIRACOMPRA");

        assertThat(desconto.valor()).isEqualByComparingTo("60.00");
        assertThat(desconto.descricao()).contains("PRIMEIRACOMPRA").contains("volume");
    }

    @Test
    @DisplayName("o acumulado nunca passa de 25% do subtotal")
    void tetoGeral() {
        // 20 itens -> 8% (32) + BLACK20 -> 20% (80) = 112; teto = 25% de 400 = 100.
        var noTeto = servico.calcular(new BigDecimal("400.00"), 20, "BLACK20");
        assertThat(noTeto.valor()).isEqualByComparingTo("100.00");
        assertThat(noTeto.descricao()).contains("teto");

        // 10 itens -> 5% (20) + PRIMEIRACOMPRA -> 10% (40) = 60; teto = 100, nao morde.
        var abaixoDoTeto = servico.calcular(new BigDecimal("400.00"), 10, "PRIMEIRACOMPRA");
        assertThat(abaixoDoTeto.valor()).isEqualByComparingTo("60.00");
        assertThat(abaixoDoTeto.descricao()).doesNotContain("teto");
    }

    @Test
    @DisplayName("cupom inexistente derruba a requisicao")
    void cupomInvalido() {
        assertThatThrownBy(() -> servico.calcular(new BigDecimal("100.00"), 1, "NAOEXISTE"))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Cupom invalido");
    }
}
