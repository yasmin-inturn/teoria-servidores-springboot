package br.com.loja.catalogo;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.loja.catalogo.ProdutoDtos.ProdutoRequest;
import br.com.loja.common.RecursoNaoEncontradoException;
import br.com.loja.common.RegraDeNegocioException;

/**
 * Regras do catalogo. A classe nao sabe nada de HTTP: recebe e devolve objetos
 * de dominio. E isso que permite testar as regras sem subir o servidor.
 */
@Service
public class ProdutoService {

    private final ProdutoRepository repositorio;

    // Injecao por construtor: o Spring passa a implementacao do repositorio ao
    // criar o bean. Como o campo e final, o objeto nunca existe pela metade.
    public ProdutoService(ProdutoRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional(readOnly = true)
    public List<Produto> listar(String categoria) {
        return (categoria == null || categoria.isBlank())
                ? repositorio.findByAtivoTrueOrderByNomeAsc()
                : repositorio.findByCategoriaIgnoreCaseAndAtivoTrueOrderByNomeAsc(categoria);
    }

    @Transactional(readOnly = true)
    public Produto buscarPorId(Long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Produto", id));
    }

    @Transactional(readOnly = true)
    public Produto buscarPorSku(String sku) {
        return repositorio.findBySku(sku)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Produto", sku));
    }

    @Transactional
    public Produto criar(ProdutoRequest requisicao) {
        if (repositorio.existsBySku(requisicao.sku())) {
            throw new RegraDeNegocioException("Ja existe produto com o SKU " + requisicao.sku());
        }
        Produto produto = new Produto(requisicao.sku(), requisicao.nome(), requisicao.descricao(),
                requisicao.categoria(), requisicao.preco(), requisicao.quantidadeEstoque(), requisicao.pesoKg());
        return repositorio.save(produto);
    }

    @Transactional
    public Produto atualizar(Long id, ProdutoRequest requisicao) {
        Produto produto = buscarPorId(id);
        produto.atualizar(requisicao.nome(), requisicao.descricao(), requisicao.categoria(),
                requisicao.preco(), requisicao.pesoKg());
        // Nao chamamos save(): dentro da transacao a entidade esta gerenciada pelo
        // Hibernate, que detecta a mudanca no flush (dirty checking).
        return produto;
    }

    @Transactional
    public Produto reporEstoque(Long id, int quantidade) {
        Produto produto = buscarPorId(id);
        produto.somarEstoque(quantidade);
        return produto;
    }

    @Transactional
    public void desativar(Long id) {
        buscarPorId(id).desativar();
    }
}
