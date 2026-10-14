package br.com.loja.frete;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;

import br.com.loja.common.RegraDeNegocioException;

/**
 * Calculo de frete: uma regra de negocio pura, sem banco e sem HTTP.
 *
 * Formula: taxa da regiao (derivada do primeiro digito do CEP) + peso * custo
 * por kg + taxa de manuseio. Acima do valor minimo configurado, ou com cupom de
 * frete gratis, o valor zera mas o prazo continua valendo.
 */
@Service
public class FreteService {

    private final FreteProperties parametros;

    public FreteService(FreteProperties parametros) {
        this.parametros = parametros;
    }

    public Frete calcular(String cep, BigDecimal pesoTotalKg, BigDecimal subtotal, boolean cupomDeFreteGratis) {
        String digitos = normalizarCep(cep);
        Regiao regiao = Regiao.porCep(digitos);

        BigDecimal custoPeso = pesoTotalKg.max(BigDecimal.ZERO)
                .multiply(parametros.custoPorKg())
                .multiply(regiao.multiplicadorPeso());

        BigDecimal valor = regiao.taxaBase()
                .add(custoPeso)
                .add(parametros.taxaManuseio())
                .setScale(2, RoundingMode.HALF_UP);

        int prazo = parametros.prazoBaseDias() + regiao.prazoAdicionalDias();

        boolean gratisPorValor = subtotal.compareTo(parametros.valorMinimoFreteGratis()) >= 0;
        if (gratisPorValor || cupomDeFreteGratis) {
            String motivo = cupomDeFreteGratis
                    ? "frete gratis por cupom"
                    : "frete gratis acima de R$ " + parametros.valorMinimoFreteGratis();
            return new Frete(BigDecimal.ZERO.setScale(2), prazo, regiao.name(), true, motivo);
        }

        return new Frete(valor, prazo, regiao.name(), false,
                "%s: taxa R$ %s + %s kg".formatted(regiao.descricao(), regiao.taxaBase(), pesoTotalKg));
    }

    private String normalizarCep(String cep) {
        String digitos = cep == null ? "" : cep.replaceAll("\\D", "");
        if (digitos.length() != 8) {
            throw new RegraDeNegocioException("CEP invalido: informe 8 digitos (recebido: " + cep + ")");
        }
        return digitos;
    }

    /** Resultado do calculo, imutavel - um record encaixa melhor que uma classe. */
    public record Frete(BigDecimal valor, int prazoDias, String regiao, boolean gratis, String descricao) {
    }

    /**
     * Faixas de CEP simplificadas (o CEP brasileiro comeca pela regiao).
     * Em producao isso viria de uma integracao com a transportadora; aqui o
     * objetivo e ter uma regra deterministica e facil de testar.
     */
    enum Regiao {

        SUDESTE("Sudeste", new BigDecimal("12.90"), BigDecimal.ONE, 0),
        SUL("Sul", new BigDecimal("16.90"), new BigDecimal("1.1"), 1),
        CENTRO_OESTE("Centro-Oeste", new BigDecimal("21.90"), new BigDecimal("1.2"), 3),
        NORDESTE("Nordeste", new BigDecimal("26.90"), new BigDecimal("1.3"), 5),
        NORTE("Norte", new BigDecimal("32.90"), new BigDecimal("1.5"), 8);

        private final String descricao;
        private final BigDecimal taxaBase;
        private final BigDecimal multiplicadorPeso;
        private final int prazoAdicionalDias;

        Regiao(String descricao, BigDecimal taxaBase, BigDecimal multiplicadorPeso, int prazoAdicionalDias) {
            this.descricao = descricao;
            this.taxaBase = taxaBase;
            this.multiplicadorPeso = multiplicadorPeso;
            this.prazoAdicionalDias = prazoAdicionalDias;
        }

        static Regiao porCep(String cepComOitoDigitos) {
            return switch (cepComOitoDigitos.charAt(0)) {
                case '0', '1', '2', '3' -> SUDESTE;
                case '8', '9' -> SUL;
                case '7' -> CENTRO_OESTE;
                case '4', '5' -> NORDESTE;
                case '6' -> NORTE;
                default -> SUDESTE;
            };
        }

        String descricao() {
            return descricao;
        }

        BigDecimal taxaBase() {
            return taxaBase;
        }

        BigDecimal multiplicadorPeso() {
            return multiplicadorPeso;
        }

        int prazoAdicionalDias() {
            return prazoAdicionalDias;
        }
    }
}
