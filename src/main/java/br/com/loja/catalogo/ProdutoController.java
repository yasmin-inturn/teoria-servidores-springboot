package br.com.loja.catalogo;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.loja.catalogo.ProdutoDtos.ProdutoRequest;
import br.com.loja.catalogo.ProdutoDtos.ProdutoResponse;
import br.com.loja.catalogo.ProdutoDtos.ReposicaoRequest;
import jakarta.validation.Valid;

/**
 * Camada de borda: converte HTTP em chamadas de servico e o resultado de volta
 * em HTTP. Leitura e publica; escrita exige perfil ADMIN.
 */
@RestController
@RequestMapping("/api/produtos")
public class ProdutoController {

    private final ProdutoService servico;

    public ProdutoController(ProdutoService servico) {
        this.servico = servico;
    }

    @GetMapping
    public List<ProdutoResponse> listar(@RequestParam(required = false) String categoria) {
        return servico.listar(categoria).stream().map(ProdutoResponse::de).toList();
    }

    @GetMapping("/{id}")
    public ProdutoResponse porId(@PathVariable Long id) {
        return ProdutoResponse.de(servico.buscarPorId(id));
    }

    @GetMapping("/sku/{sku}")
    public ProdutoResponse porSku(@PathVariable String sku) {
        return ProdutoResponse.de(servico.buscarPorSku(sku));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProdutoResponse> criar(@Valid @RequestBody ProdutoRequest requisicao) {
        Produto criado = servico.criar(requisicao);
        // 201 + cabecalho Location: o cliente descobre a URL do recurso novo.
        return ResponseEntity.created(URI.create("/api/produtos/" + criado.getId()))
                .body(ProdutoResponse.de(criado));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ProdutoResponse atualizar(@PathVariable Long id, @Valid @RequestBody ProdutoRequest requisicao) {
        return ProdutoResponse.de(servico.atualizar(id, requisicao));
    }

    @PostMapping("/{id}/estoque")
    @PreAuthorize("hasRole('ADMIN')")
    public ProdutoResponse repor(@PathVariable Long id, @Valid @RequestBody ReposicaoRequest requisicao) {
        return ProdutoResponse.de(servico.reporEstoque(id, requisicao.quantidade()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> desativar(@PathVariable Long id) {
        servico.desativar(id);
        return ResponseEntity.noContent().build();
    }
}
