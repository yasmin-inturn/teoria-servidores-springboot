package br.com.loja.catalogo;

/** Produto inexistente. O ApiExceptionHandler do controller traduz isto para HTTP 404. */
public class ProdutoNaoEncontradoException extends RuntimeException {

    public ProdutoNaoEncontradoException(Long id) {
        super("Produto não encontrado: " + id);
    }
}
