package br.com.loja.doc;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.core.SpringVersion;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import br.com.loja.doc.DocumentacaoProperties.Participante;

/**
 * Introspecção do servidor em execução.
 *
 * A lista de rotas da página NÃO é escrita à mão: é lida do
 * RequestMappingHandlerMapping, a mesma estrutura que o Spring MVC usa para
 * despachar cada requisição. Se alguém apagar um endpoint, ele some da página
 * sozinho - a documentação nunca fica desatualizada em relação ao código.
 */
@Service
public class ServidorInfoService {

    private final ApplicationContext contexto;
    private final RequestMappingHandlerMapping mapeamentos;

    public ServidorInfoService(ApplicationContext contexto,
                               @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping mapeamentos) {
        this.contexto = contexto;
        this.mapeamentos = mapeamentos;
    }

    public List<Rota> rotas() {
        List<Rota> rotas = new ArrayList<>();

        mapeamentos.getHandlerMethods().forEach((info, metodo) -> {
            String verbos = info.getMethodsCondition().getMethods().isEmpty()
                    ? "ANY"
                    : info.getMethodsCondition().getMethods().stream()
                            .map(Enum::name).sorted().reduce((a, b) -> a + ", " + b).orElse("ANY");

            for (String caminho : caminhosDe(info)) {
                // "/error" e registrado automaticamente pelo Spring Boot (BasicErrorController)
                // e nao interessa nesta tabela - ela e sobre as rotas que o grupo escreveu.
                if (caminho.equals("/error")) {
                    continue;
                }
                rotas.add(new Rota(verbos, caminho, metodo.getBeanType().getSimpleName(),
                        metodo.getMethod().getName()));
            }
        });

        rotas.sort(Comparator.comparing(Rota::caminho).thenComparing(Rota::verbos));
        return rotas;
    }

    private Set<String> caminhosDe(RequestMappingInfo info) {
        Set<String> caminhos = new TreeSet<>();
        if (info.getPathPatternsCondition() != null) {
            info.getPathPatternsCondition().getPatternValues().forEach(caminhos::add);
        }
        return caminhos;
    }

    public Runtime informacoes() {
        var rt = ManagementFactory.getRuntimeMXBean();
        Duration ligado = Duration.ofMillis(rt.getUptime());

        return new Runtime(
                System.getProperty("java.version"),
                SpringBootVersion.getVersion(),
                SpringVersion.getVersion(),
                servidorEmbarcado(),
                porta(),
                "%dh %02dmin %02ds".formatted(ligado.toHours(), ligado.toMinutesPart(), ligado.toSecondsPart()));
    }

    private String servidorEmbarcado() {
        if (contexto instanceof WebServerApplicationContext web && web.getWebServer() != null) {
            return web.getWebServer().getClass().getSimpleName().replace("WebServer", "");
        }
        return "desconhecido";
    }

    private int porta() {
        if (contexto instanceof WebServerApplicationContext web && web.getWebServer() != null) {
            return web.getWebServer().getPort();
        }
        return -1;
    }

    public String participantesEmLinha(List<Participante> participantes) {
        if (participantes == null || participantes.isEmpty()) {
            return "(defina documentacao.participantes no application.yml)";
        }
        return participantes.stream().map(Participante::nome).reduce((a, b) -> a + " | " + b).orElse("");
    }

    public record Rota(String verbos, String caminho, String controller, String metodo) {
    }

    public record Runtime(
            String versaoJava,
            String versaoSpringBoot,
            String versaoSpringFramework,
            String servidorEmbarcado,
            int porta,
            String tempoLigado) {
    }
}
