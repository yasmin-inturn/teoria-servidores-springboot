package br.com.loja.catalogo;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * API REST de exemplo: é esta rota que prova que o servidor faz mais do que
 * um "Hello World" - um @RestController, injeção de dependência, validação
 * de entrada e códigos de status HTTP corretos (200, 201, 404).
 */
@RestController
@RequestMapping("/api/produtos")
public class ProdutoController {

    private final ProdutoService servico;

    public ProdutoController(ProdutoService servico) {
        this.servico = servico;
    }

    @GetMapping
    public List<Produto> listar() {
        return servico.listar();
    }

    @GetMapping("/{id}")
    public Produto buscar(@PathVariable Long id) {
        return servico.buscar(id);
    }

    @PostMapping
    public ResponseEntity<Produto> criar(@Valid @RequestBody NovoProdutoRequest requisicao) {
        Produto criado = servico.criar(requisicao.nome(), requisicao.preco(), requisicao.estoque());
        return ResponseEntity.created(URI.create("/api/produtos/" + criado.id())).body(criado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        servico.remover(id);
        return ResponseEntity.noContent().build();
    }
}
