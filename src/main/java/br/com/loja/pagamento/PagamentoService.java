package br.com.loja.pagamento;

import java.math.BigDecimal;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.com.loja.common.RegraDeNegocioException;

/**
 * Gateway de pagamento SIMULADO.
 *
 * Num sistema real esta chamada sairia para fora (operadora de cartao) e seria o
 * ponto mais delicado da transacao: chamada de rede dentro de transacao de banco
 * segura conexao e pode estourar timeout. O padrao seria registrar a intencao,
 * commitar, chamar o gateway e confirmar depois (saga / outbox).
 *
 * Aqui a simulacao e deterministica para a aula poder ser reproduzida:
 *   PIX            -> autorizado na hora
 *   BOLETO         -> pendente (pedido fica AGUARDANDO_PAGAMENTO)
 *   CARTAO_CREDITO -> autorizado, exceto cartao terminando em 0000 (recusado)
 */
@Service
public class PagamentoService {

    private static final Logger log = LoggerFactory.getLogger(PagamentoService.class);

    @Transactional(propagation = Propagation.MANDATORY)
    public Pagamento processar(MetodoPagamento metodo, BigDecimal valor, String numeroCartao) {
        String identificador = metodo.name() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Pagamento pagamento = switch (metodo) {
            case PIX -> new Pagamento(metodo, StatusPagamento.AUTORIZADO, valor, identificador,
                    "PIX confirmado automaticamente (simulacao)");
            case BOLETO -> new Pagamento(metodo, StatusPagamento.PENDENTE, valor, identificador,
                    "Boleto emitido, aguardando pagamento");
            case CARTAO_CREDITO -> processarCartao(valor, numeroCartao, identificador);
        };

        log.info("Pagamento {} valor={} status={}", identificador, valor, pagamento.getStatus());
        return pagamento;
    }

    private Pagamento processarCartao(BigDecimal valor, String numeroCartao, String identificador) {
        String digitos = numeroCartao == null ? "" : numeroCartao.replaceAll("\\D", "");
        if (digitos.length() < 13 || digitos.length() > 19) {
            throw new RegraDeNegocioException("Numero de cartao invalido para pagamento com CARTAO_CREDITO");
        }
        if (digitos.endsWith("0000")) {
            throw new PagamentoRecusadoException("cartao sem limite (simulacao: terminado em 0000)");
        }
        return new Pagamento(MetodoPagamento.CARTAO_CREDITO, StatusPagamento.AUTORIZADO, valor, identificador,
                "Autorizado para o cartao com final " + digitos.substring(digitos.length() - 4));
    }
}
