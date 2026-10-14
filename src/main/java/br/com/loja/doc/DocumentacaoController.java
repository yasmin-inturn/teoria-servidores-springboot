package br.com.loja.doc;

import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import br.com.loja.doc.CodigoFonteService.ArquivoFonte;

/**
 * Serve a documentacao do trabalho na raiz do servidor.
 *
 * Note que a classe e @Controller (nao @RestController): o retorno "documentacao"
 * nao e um corpo de resposta, e o NOME DA VIEW. O ViewResolver do Thymeleaf
 * procura templates/documentacao.html, renderiza com os dados do Model e o
 * resultado sai como text/html. Os endpoints de /api/ fazem o contrario: devolvem
 * objetos que o Jackson serializa em JSON.
 */
@Controller
public class DocumentacaoController {

    private final DocumentacaoProperties conteudo;
    private final CodigoFonteService fontes;
    private final ServidorInfoService servidor;

    public DocumentacaoController(DocumentacaoProperties conteudo, CodigoFonteService fontes,
                                  ServidorInfoService servidor) {
        this.conteudo = conteudo;
        this.fontes = fontes;
        this.servidor = servidor;
    }

    @GetMapping("/")
    public String documentacao(Model model) {
        model.addAttribute("doc", conteudo);
        model.addAttribute("participantesEmLinha", servidor.participantesEmLinha(conteudo.participantes()));
        model.addAttribute("runtime", servidor.informacoes());
        model.addAttribute("rotas", servidor.rotasDaApi());
        model.addAttribute("totalRotas", servidor.rotas().size());
        model.addAttribute("destaques", fontes.buscarTodos(conteudo.arquivosDestaque()));
        model.addAttribute("arvore", fontes.listarPorPasta());
        model.addAttribute("totalArquivos", fontes.totalArquivos());
        model.addAttribute("totalLinhas", fontes.totalLinhas());
        return "documentacao";
    }

    /**
     * Qualquer arquivo do projeto em texto puro, ex.:
     * /doc/fonte/java/br/com/loja/pedido/PedidoService.java
     *
     * O {*caminho} e um padrao do Spring MVC que captura o resto da URL
     * inteiro, barras incluidas.
     */
    @GetMapping("/doc/fonte/{*caminho}")
    @ResponseBody
    public ResponseEntity<String> fonte(@PathVariable String caminho) {
        ArquivoFonte arquivo = fontes.buscar(caminho.startsWith("/") ? caminho.substring(1) : caminho);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "plain", java.nio.charset.StandardCharsets.UTF_8))
                .body(arquivo.conteudo());
    }

    /** Indice dos arquivos disponiveis, em JSON - usado pelo JS da pagina. */
    @GetMapping("/doc/arquivos")
    @ResponseBody
    public Map<String, Object> arquivos() {
        List<Map<String, Object>> lista = fontes.listar().stream()
                .map(arquivo -> Map.<String, Object>of(
                        "caminho", arquivo.caminho(),
                        "nome", arquivo.nome(),
                        "pasta", arquivo.pasta(),
                        "linhas", arquivo.linhas()))
                .toList();
        return Map.of("total", lista.size(), "arquivos", lista);
    }

    /** As rotas que o servidor realmente registrou, em JSON. */
    @GetMapping("/doc/rotas")
    @ResponseBody
    public List<ServidorInfoService.Rota> rotas() {
        return servidor.rotas();
    }
}
