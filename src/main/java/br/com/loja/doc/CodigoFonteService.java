package br.com.loja.doc;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

/**
 * Lê o próprio código-fonte do servidor para exibi-lo na página de documentação.
 *
 * Como isso funciona: o pom.xml tem um maven-resources-plugin que copia
 * src/main/java para dentro de target/classes/fonte durante o build. Resultado:
 * os fontes viajam DENTRO do jar e a página funciona igual em
 * "mvn spring-boot:run" e em "java -jar", sem depender da pasta do projeto
 * existir na máquina.
 */
@Service
public class CodigoFonteService {

    private static final String RAIZ = "fonte/";

    /** Cache: o fonte não muda em tempo de execução, lê uma vez e pronto. */
    private final Map<String, ArquivoFonte> arquivos;

    public CodigoFonteService() {
        this.arquivos = carregar();
    }

    private Map<String, ArquivoFonte> carregar() {
        var resolver = new PathMatchingResourcePatternResolver();
        List<ArquivoFonte> lidos = new ArrayList<>();
        try {
            for (Resource recurso : resolver.getResources("classpath*:" + RAIZ + "**/*")) {
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
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao ler o próprio código-fonte do classpath", e);
        }

        lidos.sort(Comparator.comparing(ArquivoFonte::caminho));
        Map<String, ArquivoFonte> mapa = new LinkedHashMap<>();
        lidos.forEach(arquivo -> mapa.put(arquivo.caminho(), arquivo));
        return Map.copyOf(mapa);
    }

    public List<ArquivoFonte> listar() {
        return arquivos.values().stream().sorted(Comparator.comparing(ArquivoFonte::caminho)).toList();
    }

    public ArquivoFonte buscar(String caminho) {
        ArquivoFonte arquivo = arquivos.get(caminho);
        if (arquivo == null) {
            throw new NoSuchElementException("Arquivo de código não encontrado: " + caminho);
        }
        return arquivo;
    }

    public int totalLinhas() {
        return arquivos.values().stream().mapToInt(ArquivoFonte::linhas).sum();
    }

    /** Um arquivo de código com os metadados que a página mostra. */
    public record ArquivoFonte(String caminho, String conteudo) {

        public int linhas() {
            return conteudo.isEmpty() ? 0 : conteudo.split("\r?\n", -1).length;
        }
    }
}
