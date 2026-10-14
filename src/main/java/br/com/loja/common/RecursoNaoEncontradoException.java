package br.com.loja.common;

/** Recurso inexistente. Mapeado para HTTP 404 pelo {@link ApiExceptionHandler}. */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }

    public static RecursoNaoEncontradoException de(String tipo, Object identificador) {
        return new RecursoNaoEncontradoException("%s nao encontrado: %s".formatted(tipo, identificador));
    }
}
