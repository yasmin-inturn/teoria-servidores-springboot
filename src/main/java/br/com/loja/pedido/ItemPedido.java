package br.com.loja.pedido;

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
 * Item de um pedido.
 *
 * Repare que nome, SKU e preco sao COPIADOS do produto no momento da compra
 * (snapshot). Se amanha o preco do produto mudar, o pedido antigo continua
 * valendo o que o cliente pagou - uma nota fiscal nao pode mudar sozinha.
 */
@Entity
@Table(name = "item_pedido")
public class ItemPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id")
    private Produto produto;

    @Column(nullable = false, length = 40)
    private String sku;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false)
    private int quantidade;

    @Column(name = "preco_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal precoUnitario;

    protected ItemPedido() {
        // exigido pelo JPA
    }

    public ItemPedido(Produto produto, int quantidade) {
        this.produto = produto;
        this.sku = produto.getSku();
        this.nome = produto.getNome();
        this.quantidade = quantidade;
        this.precoUnitario = produto.getPreco();
    }

    public BigDecimal subtotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
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

    public String getSku() {
        return sku;
    }

    public String getNome() {
        return nome;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }
}
