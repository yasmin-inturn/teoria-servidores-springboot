package br.com.loja.carrinho;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.loja.catalogo.Produto;
import br.com.loja.catalogo.ProdutoService;
import br.com.loja.common.EstoqueInsuficienteException;

/**
 * Operacoes do carrinho. A validacao de estoque aqui e apenas um aviso
 * antecipado para o cliente: a garantia real acontece no checkout, dentro da
 * transacao do pedido. Entre colocar no carrinho e finalizar, outra pessoa pode
 * comprar a ultima unidade.
 */
@Service
public class CarrinhoService {

    private final CarrinhoRepository repositorio;
    private final ProdutoService produtos;

    public CarrinhoService(CarrinhoRepository repositorio, ProdutoService produtos) {
        this.repositorio = repositorio;
        this.produtos = produtos;
    }

    @Transactional
    public Carrinho adicionar(String email, String sku, int quantidade) {
        Produto produto = produtos.buscarPorSku(sku);
        Carrinho carrinho = obterOuCriar(email);

        int jaNoCarrinho = carrinho.getItens().stream()
                .filter(item -> item.getProduto().getSku().equalsIgnoreCase(sku))
                .mapToInt(ItemCarrinho::getQuantidade)
                .sum();

        if (!produto.temEstoquePara(jaNoCarrinho + quantidade)) {
            throw new EstoqueInsuficienteException(produto.getSku(),
                    jaNoCarrinho + quantidade, produto.getQuantidadeEstoque());
        }

        carrinho.adicionar(produto, quantidade);
        return carrinho;
    }

    @Transactional
    public Carrinho alterarQuantidade(String email, String sku, int quantidade) {
        Carrinho carrinho = obterOuCriar(email);
        carrinho.definirQuantidade(sku, quantidade);
        return carrinho;
    }

    @Transactional
    public Carrinho remover(String email, String sku) {
        Carrinho carrinho = obterOuCriar(email);
        carrinho.remover(sku);
        return carrinho;
    }

    @Transactional
    public Carrinho limpar(String email) {
        Carrinho carrinho = obterOuCriar(email);
        carrinho.limpar();
        return carrinho;
    }

    @Transactional(readOnly = true)
    public Carrinho ver(String email) {
        return repositorio.findByEmailUsuarioIgnoreCase(email).orElseGet(() -> new Carrinho(email));
    }

    @Transactional
    public Carrinho obterOuCriar(String email) {
        return repositorio.findByEmailUsuarioIgnoreCase(email)
                .orElseGet(() -> repositorio.save(new Carrinho(email)));
    }
}
