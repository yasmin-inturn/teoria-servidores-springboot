package br.com.loja.catalogo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

/**
 * Catálogo de produtos em memória - sem banco de dados.
 *
 * O objetivo aqui não é simular um e-commerce completo, e sim mostrar, de
 * forma pequena e didática, como o Spring organiza uma camada de serviço:
 * o @Service é um bean gerenciado pelo container, injetado por construtor
 * no controller, e guarda o estado da aplicação (aqui, um Map em memória).
 */
@Service
public class ProdutoService {

    private final Map<Long, Produto> produtos = new ConcurrentHashMap<>();
    private final AtomicLong proximoId = new AtomicLong(1);

    public ProdutoService() {
        criar("Teclado mecânico", new BigDecimal("349.90"), 12);
        criar("Mouse sem fio", new BigDecimal("129.90"), 30);
        criar("Monitor 27\" 4K", new BigDecimal("1899.00"), 5);
    }

    public List<Produto> listar() {
        return produtos.values().stream()
                .sorted((a, b) -> a.id().compareTo(b.id()))
                .toList();
    }

    public Produto buscar(Long id) {
        Produto produto = produtos.get(id);
        if (produto == null) {
            throw new ProdutoNaoEncontradoException(id);
        }
        return produto;
    }

    public Produto criar(String nome, BigDecimal preco, int estoque) {
        Long id = proximoId.getAndIncrement();
        Produto produto = new Produto(id, nome, preco, estoque);
        produtos.put(id, produto);
        return produto;
    }

    public void remover(Long id) {
        if (produtos.remove(id) == null) {
            throw new ProdutoNaoEncontradoException(id);
        }
    }
}
