package br.com.loja.pagamento;

/**
 * Situacao do pagamento. PENDENTE existe por causa do boleto: o pedido fica
 * aguardando confirmacao sem travar o estoque indefinidamente.
 */
public enum StatusPagamento {

    AUTORIZADO,
    PENDENTE,
    RECUSADO
}
