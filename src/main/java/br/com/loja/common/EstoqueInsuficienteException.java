package br.com.loja.common;

/**
 * Lancada quando a quantidade pedida supera o estoque disponivel. Por ser uma
 * {@code RuntimeException}, o rollback da transacao do pedido e automatico:
 * nada do que ja foi gravado (itens, baixa de estoque) e confirmado.
 * Mapeada para HTTP 409 (conflito) pelo {@link ApiExceptionHandler}.
 */
public class EstoqueInsuficienteException extends RegraDeNegocioException {

    private final String sku;
    private final int solicitado;
    private final int disponivel;

    public EstoqueInsuficienteException(String sku, int solicitado, int disponivel) {
        super("Estoque insuficiente para o SKU %s: solicitado %d, disponivel %d"
                .formatted(sku, solicitado, disponivel));
        this.sku = sku;
        this.solicitado = solicitado;
        this.disponivel = disponivel;
    }

    public String getSku() {
        return sku;
    }

    public int getSolicitado() {
        return solicitado;
    }

    public int getDisponivel() {
        return disponivel;
    }
}
