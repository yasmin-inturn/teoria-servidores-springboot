package br.com.loja.carrinho;

import java.math.BigDecimal;

import br.com.loja.catalogo.Produto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Item do carrinho. Diferente do item de pedido, aqui o preco NAO e congelado:
 * o carrinho sempre mostra o preco atual do catalogo. O congelamento acontece
 * no checkout, quando o pedido e criado.
 */
@Entity
@Table(name = "item_carrinho")
public class ItemCarrinho {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "produto_id")
    private Produto produto;

    @Column(nullable = false)
    private int quantidade;

    protected ItemCarrinho() {
        // exigido pelo JPA
    }

    public ItemCarrinho(Produto produto, int quantidade) {
        this.produto = produto;
        this.quantidade = quantidade;
    }

    void somarQuantidade(int delta) {
        this.quantidade += delta;
    }

    void definirQuantidade(int quantidade) {
        this.quantidade = quantidade;
    }

    public BigDecimal subtotal() {
        return produto.getPreco().multiply(BigDecimal.valueOf(quantidade));
    }

    public BigDecimal pesoTotal() {
        return produto.getPesoKg().multiply(BigDecimal.valueOf(quantidade));
    }

    public Long getId() {
        return id;
    }

    public Produto getProduto() {
        return produto;
    }

    public int getQuantidade() {
        return quantidade;
    }
}
