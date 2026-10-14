package br.com.loja.common;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduz excecoes de dominio em respostas HTTP. Centralizar isso aqui evita
 * try/catch espalhado pelos controllers: cada camada lanca a excecao que faz
 * sentido para ela e o protocolo HTTP e decidido em um unico lugar.
 *
 * O corpo segue o formato "Problem Detail" (RFC 9457), padrao desde o Spring 6.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ProblemDetail naoEncontrado(RecursoNaoEncontradoException ex) {
        return problema(HttpStatus.NOT_FOUND, "Recurso nao encontrado", ex.getMessage(), "recurso-nao-encontrado");
    }

    @ExceptionHandler(EstoqueInsuficienteException.class)
    public ProblemDetail estoqueInsuficiente(EstoqueInsuficienteException ex) {
        ProblemDetail detalhe = problema(HttpStatus.CONFLICT, "Estoque insuficiente", ex.getMessage(),
                "estoque-insuficiente");
        detalhe.setProperty("sku", ex.getSku());
        detalhe.setProperty("solicitado", ex.getSolicitado());
        detalhe.setProperty("disponivel", ex.getDisponivel());
        return detalhe;
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    public ProblemDetail regraDeNegocio(RegraDeNegocioException ex) {
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, "Regra de negocio violada", ex.getMessage(),
                "regra-de-negocio");
    }

    /**
     * Disparada pelo Bean Validation quando o JSON de entrada e invalido
     * (@Valid no controller). Devolve campo a campo para o front-end exibir.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validacao(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(erro -> campos.putIfAbsent(erro.getField(), erro.getDefaultMessage()));
        ex.getBindingResult().getGlobalErrors()
                .forEach(erro -> campos.putIfAbsent(erro.getObjectName(), erro.getDefaultMessage()));

        ProblemDetail detalhe = problema(HttpStatus.BAD_REQUEST, "Dados invalidos",
                "A requisicao nao passou na validacao", "dados-invalidos");
        detalhe.setProperty("campos", campos);
        return detalhe;
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ProblemDetail credenciaisInvalidas(CredenciaisInvalidasException ex) {
        return problema(HttpStatus.UNAUTHORIZED, "Nao autenticado", ex.getMessage(), "credenciais-invalidas");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail acessoNegado(AccessDeniedException ex) {
        return problema(HttpStatus.FORBIDDEN, "Acesso negado",
                "Seu perfil nao permite esta operacao", "acesso-negado");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail inesperado(Exception ex) {
        log.error("Erro nao tratado", ex);
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno",
                "Falha inesperada no servidor", "erro-interno");
    }

    private ProblemDetail problema(HttpStatus status, String titulo, String detalhe, String tipo) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setTitle(titulo);
        problema.setType(URI.create("https://loja.exemplo/erros/" + tipo));
        return problema;
    }
}
