package br.com.loja.common;

/**
 * Erro de regra de negocio: a requisicao esta bem formada, mas o estado atual do
 * sistema nao permite a operacao (ex.: transicao de status invalida, cupom expirado).
 * Mapeado para HTTP 422 pelo {@link ApiExceptionHandler}.
 */
public class RegraDeNegocioException extends RuntimeException {

    public RegraDeNegocioException(String mensagem) {
        super(mensagem);
    }
}
