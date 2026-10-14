package br.com.loja.catalogo;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Produto do catalogo. A anotacao @Entity faz o Hibernate mapear esta classe
 * para uma tabela; cada campo vira coluna.
 *
 * O campo versao, anotado com @Version, liga o travamento otimista: se duas
 * requisicoes lerem o mesmo produto e ambas tentarem baixar estoque, a segunda
 * falha com OptimisticLockingFailureException em vez de sobrescrever a primeira.
 * E o mecanismo mais simples de evitar vender estoque que nao existe.
 */
@Entity
@Table(name = "produto")
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String sku;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(length = 400)
    private String descricao;

    @Column(nullable = false, length = 40)
    private String categoria;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal preco;

    @Column(name = "quantidade_estoque", nullable = false)
    private int quantidadeEstoque;

    @Column(name = "peso_kg", nullable = false, precision = 8, scale = 3)
    private BigDecimal pesoKg;

    @Column(nullable = false)
    private boolean ativo = true;

    @Version
    private Long versao;

    protected Produto() {
        // construtor sem argumentos exigido pelo JPA
    }

    public Produto(String sku, String nome, String descricao, String categoria,
                   BigDecimal preco, int quantidadeEstoque, BigDecimal pesoKg) {
        this.sku = sku;
        this.nome = nome;
        this.descricao = descricao;
        this.categoria = categoria;
        this.preco = preco;
        this.quantidadeEstoque = quantidadeEstoque;
        this.pesoKg = pesoKg;
    }

    public boolean temEstoquePara(int quantidade) {
        return ativo && quantidadeEstoque >= quantidade;
    }

    /**
     * Soma a quantidade ao estoque (use negativo para baixar). Quem decide se a
     * baixa e permitida e o EstoqueService, nao a entidade.
     */
    public void somarEstoque(int quantidade) {
        this.quantidadeEstoque += quantidade;
    }

    public void atualizar(String nome, String descricao, String categoria, BigDecimal preco, BigDecimal pesoKg) {
        this.nome = nome;
        this.descricao = descricao;
        this.categoria = categoria;
        this.preco = preco;
        this.pesoKg = pesoKg;
    }

    public void desativar() {
        this.ativo = false;
    }

    public Long getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getNome() {
        return nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getCategoria() {
        return categoria;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public int getQuantidadeEstoque() {
        return quantidadeEstoque;
    }

    public BigDecimal getPesoKg() {
        return pesoKg;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public Long getVersao() {
        return versao;
    }
}
