package br.com.loja.frete;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.loja.common.RegraDeNegocioException;

/**
 * Teste de regra de negocio pura: nenhum contexto do Spring sobe aqui.
 *
 * Isso so e possivel porque o servico recebe a configuracao pelo construtor.
 * Resultado: estes testes rodam em milissegundos, enquanto um @SpringBootTest
 * leva segundos para subir o contexto inteiro.
 */
class FreteServiceTest {

    private final FreteService servico = new FreteService(new FreteProperties(
            new BigDecimal("299.00"),  // frete gratis acima disso
            new BigDecimal("2.50"),    // por kg
            new BigDecimal("4.90"),    // manuseio
            3));                       // prazo base

    @Test
    @DisplayName("CEP do Sudeste cobra taxa base menor e prazo base")
    void sudeste() {
        var frete = servico.calcular("01310-100", new BigDecimal("2"), new BigDecimal("100.00"), false);

        // 12.90 (base SP) + 2kg * 2.50 * 1.0 + 4.90 = 22.80
        assertThat(frete.valor()).isEqualByComparingTo("22.80");
        assertThat(frete.prazoDias()).isEqualTo(3);
        assertThat(frete.gratis()).isFalse();
        assertThat(frete.regiao()).isEqualTo("SUDESTE");
    }

    @Test
    @DisplayName("CEP do Norte cobra mais e demora mais")
    void norte() {
        var frete = servico.calcular("69900-000", new BigDecimal("2"), new BigDecimal("100.00"), false);

        // 32.90 + 2kg * 2.50 * 1.5 + 4.90 = 45.30
        assertThat(frete.valor()).isEqualByComparingTo("45.30");
        assertThat(frete.prazoDias()).isEqualTo(11);
    }

    @Test
    @DisplayName("acima do valor minimo o frete zera, mas o prazo continua")
    void freteGratisPorValor() {
        var frete = servico.calcular("69900-000", new BigDecimal("5"), new BigDecimal("300.00"), false);

        assertThat(frete.valor()).isEqualByComparingTo("0.00");
        assertThat(frete.gratis()).isTrue();
        assertThat(frete.prazoDias()).isEqualTo(11);
    }

    @Test
    @DisplayName("cupom de frete gratis zera mesmo com subtotal baixo")
    void freteGratisPorCupom() {
        var frete = servico.calcular("88000-000", new BigDecimal("1"), new BigDecimal("50.00"), true);

        assertThat(frete.valor()).isEqualByComparingTo("0.00");
        assertThat(frete.gratis()).isTrue();
    }

    @Test
    @DisplayName("CEP malformado e erro de regra de negocio, nao NullPointer")
    void cepInvalido() {
        assertThatThrownBy(() -> servico.calcular("123", BigDecimal.ONE, BigDecimal.TEN, false))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("CEP invalido");
    }
}
