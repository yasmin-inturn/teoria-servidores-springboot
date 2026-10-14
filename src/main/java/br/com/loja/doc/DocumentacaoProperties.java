package br.com.loja.doc;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Todo o conteudo editorial da pagina de documentacao vem do application.yml
 * (prefixo documentacao). Trocar os nomes do grupo, os pontos fortes ou o texto
 * da conclusao nao exige mexer em Java nem em HTML.
 */
@ConfigurationProperties(prefix = "documentacao")
public record DocumentacaoProperties(
        String titulo,
        String tecnologia,
        String subtitulo,
        String disciplina,
        String instituicao,
        String comandoExecucao,
        String comandoAlternativo,
        List<Participante> participantes,
        List<PontoForte> pontosFortes,
        Conclusao conclusao,
        List<String> arquivosDestaque) {

    public record Participante(String nome, String papel) {
    }

    public record PontoForte(String titulo, String texto) {
    }

    public record Conclusao(String dificuldade, String comoResolvemos, String recomendacao) {
    }
}
