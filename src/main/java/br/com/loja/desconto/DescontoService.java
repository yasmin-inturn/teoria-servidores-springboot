package br.com.loja.desconto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import br.com.loja.common.RegraDeNegocioException;

/**
 * Descontos: cupom (opcional) somado ao desconto progressivo por volume, com
 * teto de 25% do subtotal para o total acumulado.
 *
 * Toda decisao sai de BigDecimal, nunca de double: dinheiro em ponto flutuante
 * binario acumula erro de centavo (0.1 + 0.2 != 0.3). Esse e um dos detalhes que
 * a tipagem forte do Java deixa impossivel de esquecer.
 */
@Service
public class DescontoService {

    private static final BigDecimal TETO_PERCENTUAL = new BigDecimal("0.25");

    private static final Map<String, Cupom> CUPONS = Map.of(
            "PRIMEIRACOMPRA", new Cupom("PRIMEIRACOMPRA", new BigDecimal("0.10"), new BigDecimal("50.00"), false),
            "BLACK20", new Cupom("BLACK20", new BigDecimal("0.20"), new BigDecimal("200.00"), false),
            "FRETEGRATIS", new Cupom("FRETEGRATIS", BigDecimal.ZERO, BigDecimal.ZERO, true));

    public Desconto calcular(BigDecimal subtotal, int quantidadeTotalItens, String codigoCupom) {
        List<String> aplicados = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        boolean freteGratis = false;

        if (codigoCupom != null && !codigoCupom.isBlank()) {
            Cupom cupom = CUPONS.get(codigoCupom.trim().toUpperCase());
            if (cupom == null) {
                throw new RegraDeNegocioException("Cupom invalido ou expirado: " + codigoCupom);
            }
            if (cupom.freteGratis()) {
                freteGratis = true;
                aplicados.add(cupom.codigo() + " (frete gratis)");
            } else {
                BigDecimal valor = percentual(subtotal, cupom.percentual()).min(cupom.tetoAbsoluto());
                total = total.add(valor);
                aplicados.add("%s (-R$ %s)".formatted(cupom.codigo(), valor));
            }
        }

        // Desconto progressivo: incentiva carrinho maior sem precisar de cupom.
        BigDecimal percentualVolume = switch (faixaDeVolume(quantidadeTotalItens)) {
            case 1 -> new BigDecimal("0.03");
            case 2 -> new BigDecimal("0.05");
            case 3 -> new BigDecimal("0.08");
            default -> BigDecimal.ZERO;
        };
        if (percentualVolume.signum() > 0) {
            BigDecimal valor = percentual(subtotal, percentualVolume);
            total = total.add(valor);
            aplicados.add("volume %d itens (-R$ %s)".formatted(quantidadeTotalItens, valor));
        }

        BigDecimal teto = percentual(subtotal, TETO_PERCENTUAL);
        if (total.compareTo(teto) > 0) {
            total = teto;
            aplicados.add("teto de 25% aplicado");
        }

        return new Desconto(total.setScale(2, RoundingMode.HALF_UP), freteGratis,
                aplicados.isEmpty() ? "sem desconto" : String.join(", ", aplicados));
    }

    private int faixaDeVolume(int itens) {
        if (itens >= 20) {
            return 3;
        }
        if (itens >= 10) {
            return 2;
        }
        if (itens >= 5) {
            return 1;
        }
        return 0;
    }

    private BigDecimal percentual(BigDecimal base, BigDecimal percentual) {
        return base.multiply(percentual).setScale(2, RoundingMode.HALF_UP);
    }

    public record Desconto(BigDecimal valor, boolean freteGratis, String descricao) {
    }

    private record Cupom(String codigo, BigDecimal percentual, BigDecimal tetoAbsoluto, boolean freteGratis) {
    }
}
