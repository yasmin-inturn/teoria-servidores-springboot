package br.com.loja.doc;

import java.nio.charset.StandardCharsets;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Serve a documentação do trabalho na rota principal.
 *
 * É @Controller, não @RestController: o retorno "documentacao" não é um corpo
 * de resposta, é o NOME DA VIEW. O ViewResolver do Thymeleaf procura
 * templates/documentacao.html, renderiza com os dados do Model e devolve HTML.
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
        model.addAttribute("rotas", servidor.rotas());
        model.addAttribute("arquivos", fontes.listar());
        model.addAttribute("totalLinhas", fontes.totalLinhas());
        return "documentacao";
    }

    /** Qualquer arquivo do próprio servidor, em texto puro - ex.: /doc/fonte/java/br/com/loja/catalogo/ProdutoController.java */
    @GetMapping("/doc/fonte/{*caminho}")
    @ResponseBody
    public ResponseEntity<String> fonte(@PathVariable String caminho) {
        var arquivo = fontes.buscar(caminho.startsWith("/") ? caminho.substring(1) : caminho);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                .body(arquivo.conteudo());
    }
}
