package br.com.loja.doc;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Todo o conteúdo editorial da página de documentação vem do application.yml
 * (prefixo documentacao). Trocar um texto não exige mexer em Java nem em HTML.
 *
 * A estrutura dos campos segue à risca o que a atividade pede: título,
 * pontos fortes + caso de uso ideal, pré-requisitos e setup, e conclusão.
 */
@ConfigurationProperties(prefix = "documentacao")
public record DocumentacaoProperties(
        String titulo,
        String disciplina,
        String instituicao,
        String comandoExecucao,
        List<Participante> participantes,
        List<PontoForte> pontosFortes,
        String casoDeUsoIdeal,
        List<String> prerequisitos,
        List<String> setupComum,
        List<String> setupLinux,
        List<String> setupMacos,
        List<String> setupWindows,
        Conclusao conclusao) {

    // Thymeleaf bloqueia acesso a java.lang.System (T(...)) dentro do template,
    // entao o join de cada bloco de comandos e feito aqui, em Java puro.
    public String setupLinuxTexto() {
        return String.join("\n", setupLinux());
    }

    public String setupMacosTexto() {
        return String.join("\n", setupMacos());
    }

    public String setupWindowsTexto() {
        return String.join("\n", setupWindows());
    }

    public record Participante(String nome) {
    }

    public record PontoForte(String titulo, String texto) {
    }

    public record Conclusao(String dificuldade, String recomendacao) {
    }
}
