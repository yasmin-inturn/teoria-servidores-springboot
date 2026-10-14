package br.com.loja.common;

/** Email inexistente ou senha incorreta. Mapeada para HTTP 401. */
public class CredenciaisInvalidasException extends RuntimeException {

    public CredenciaisInvalidasException() {
        super("Email ou senha invalidos");
    }
}
