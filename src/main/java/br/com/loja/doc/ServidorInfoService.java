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
import org.springframework.context.ApplicationContext;
import org.springframework.core.SpringVersion;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import br.com.loja.doc.DocumentacaoProperties.Participante;

/**
 * Introspeccao do servidor em execucao.
 *
 * A lista de rotas da pagina NAO e escrita a mao: ela e lida do
 * RequestMappingHandlerMapping, a estrutura que o Spring MVC monta na
 * inicializacao para saber qual metodo chamar para cada URL. Entao a
 * documentacao nunca fica desatualizada em relacao ao codigo - se alguem
 * apagar um endpoint, ele desaparece da pagina.
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
            Set<String> caminhos = caminhosDe(info);
            String verbos = info.getMethodsCondition().getMethods().isEmpty()
                    ? "ANY"
                    : info.getMethodsCondition().getMethods().stream()
                            .map(Enum::name).sorted().reduce((a, b) -> a + ", " + b).orElse("ANY");

            for (String caminho : caminhos) {
                rotas.add(new Rota(
                        verbos,
                        caminho,
                        metodo.getBeanType().getSimpleName(),
                        metodo.getMethod().getName()));
            }
        });

        rotas.sort(Comparator.comparing(Rota::caminho).thenComparing(Rota::verbos));
        return rotas;
    }

    /** Rotas da API de negocio (ignora /actuator, /error e a propria doc). */
    public List<Rota> rotasDaApi() {
        return rotas().stream()
                .filter(rota -> rota.caminho().startsWith("/api/"))
                .toList();
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
        long memoriaUsadaMb = (java.lang.Runtime.getRuntime().totalMemory()
                - java.lang.Runtime.getRuntime().freeMemory()) / (1024 * 1024);

        return new Runtime(
                System.getProperty("java.version"),
                System.getProperty("java.vm.name"),
                SpringBootVersion.getVersion(),
                SpringVersion.getVersion(),
                servidorEmbarcado(),
                porta(),
                contexto.getBeanDefinitionCount(),
                "%dh %02dmin %02ds".formatted(ligado.toHours(), ligado.toMinutesPart(), ligado.toSecondsPart()),
                memoriaUsadaMb,
                java.lang.Runtime.getRuntime().availableProcessors());
    }

    private String servidorEmbarcado() {
        if (contexto instanceof org.springframework.boot.web.server.context.WebServerApplicationContext web
                && web.getWebServer() != null) {
            return web.getWebServer().getClass().getSimpleName().replace("WebServer", "");
        }
        return "desconhecido";
    }

    private int porta() {
        if (contexto instanceof org.springframework.boot.web.server.context.WebServerApplicationContext web
                && web.getWebServer() != null) {
            return web.getWebServer().getPort();
        }
        return -1;
    }

    public boolean contextoWeb() {
        return contexto instanceof WebApplicationContext;
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
            String vm,
            String versaoSpringBoot,
            String versaoSpringFramework,
            String servidorEmbarcado,
            int porta,
            int beansRegistrados,
            String tempoLigado,
            long memoriaUsadaMb,
            int processadores) {
    }
}
