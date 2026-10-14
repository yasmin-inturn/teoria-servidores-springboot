package br.com.loja.doc;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import br.com.loja.common.RecursoNaoEncontradoException;

/**
 * Le o proprio codigo-fonte do servidor para exibi-lo na pagina de documentacao.
 *
 * Como isso funciona: o pom.xml tem um maven-resources-plugin que copia
 * src/main/java, os templates e o pom para dentro de target/classes/fonte
 * durante o build. Resultado: os fontes viajam DENTRO do jar e a pagina
 * funciona igual em "mvn spring-boot:run" e em "java -jar", sem depender da
 * pasta do projeto existir na maquina.
 *
 * A leitura usa o PathMatchingResourcePatternResolver, o mecanismo do Spring
 * para varrer recursos do classpath com padrao (classpath*:fonte/**).
 */
@Service
public class CodigoFonteService {

    private static final String RAIZ = "fonte/";

    /** Cache: o fonte nao muda em tempo de execucao, le uma vez e pronto. */
    private final Map<String, ArquivoFonte> arquivos;

    public CodigoFonteService() {
        this.arquivos = carregar();
    }

    private Map<String, ArquivoFonte> carregar() {
        var resolver = new PathMatchingResourcePatternResolver();
        Map<String, ArquivoFonte> mapa = new LinkedHashMap<>();
        try {
            Resource[] recursos = resolver.getResources("classpath*:" + RAIZ + "**/*");
            List<ArquivoFonte> lidos = new ArrayList<>();

            for (Resource recurso : recursos) {
                if (!recurso.isReadable()) {
                    continue;
                }
                String url = recurso.getURL().toString();
                int corte = url.lastIndexOf(RAIZ);
                if (corte < 0) {
                    continue;
                }
                String caminho = url.substring(corte + RAIZ.length());
                if (caminho.isBlank() || caminho.endsWith("/")) {
                    continue;
                }
                String conteudo = new String(recurso.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                lidos.add(new ArquivoFonte(caminho, conteudo));
            }

            lidos.sort(Comparator.comparing(ArquivoFonte::caminho));
            lidos.forEach(arquivo -> mapa.put(arquivo.caminho(), arquivo));
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao ler o proprio codigo-fonte do classpath", e);
        }
        return Map.copyOf(mapa);
    }

    public List<ArquivoFonte> listar() {
        return arquivos.values().stream()
                .sorted(Comparator.comparing(ArquivoFonte::caminho))
                .toList();
    }

    /** Arquivos agrupados por pacote/pasta, para a arvore da pagina. */
    public Map<String, List<ArquivoFonte>> listarPorPasta() {
        Map<String, List<ArquivoFonte>> agrupado = new LinkedHashMap<>();
        for (ArquivoFonte arquivo : listar()) {
            agrupado.computeIfAbsent(arquivo.pasta(), chave -> new ArrayList<>()).add(arquivo);
        }
        return agrupado;
    }

    public ArquivoFonte buscar(String caminho) {
        ArquivoFonte arquivo = arquivos.get(caminho);
        if (arquivo == null) {
            throw RecursoNaoEncontradoException.de("Arquivo de codigo", caminho);
        }
        return arquivo;
    }

    public List<ArquivoFonte> buscarTodos(List<String> caminhos) {
        if (caminhos == null) {
            return List.of();
        }
        return caminhos.stream().filter(arquivos::containsKey).map(arquivos::get).toList();
    }

    public int totalArquivos() {
        return arquivos.size();
    }

    public int totalLinhas() {
        return arquivos.values().stream().mapToInt(ArquivoFonte::linhas).sum();
    }

    /** Um arquivo de codigo com os metadados que a pagina mostra. */
    public record ArquivoFonte(String caminho, String conteudo) {

        public String nome() {
            int barra = caminho.lastIndexOf('/');
            return barra < 0 ? caminho : caminho.substring(barra + 1);
        }

        public String pasta() {
            int barra = caminho.lastIndexOf('/');
            return barra < 0 ? "(raiz)" : caminho.substring(0, barra);
        }

        public int linhas() {
            return conteudo.isEmpty() ? 0 : conteudo.split("\r?\n", -1).length;
        }

        public String extensao() {
            int ponto = nome().lastIndexOf('.');
            return ponto < 0 ? "" : nome().substring(ponto + 1);
        }
    }
}
