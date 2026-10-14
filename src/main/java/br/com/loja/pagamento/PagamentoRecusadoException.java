package br.com.loja.pagamento;

import br.com.loja.common.RegraDeNegocioException;

/**
 * Pagamento negado pela operadora.
 *
 * Lancar a excecao (em vez de devolver um objeto de erro) e o que dispara o
 * rollback da transacao do pedido: o estoque ja baixado volta sozinho, porque
 * nada foi confirmado no banco.
 */
public class PagamentoRecusadoException extends RegraDeNegocioException {

    public PagamentoRecusadoException(String motivo) {
        super("Pagamento recusado: " + motivo);
    }
}
