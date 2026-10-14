package br.com.loja.carrinho;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import br.com.loja.catalogo.Produto;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * Carrinho persistido, um por usuario.
 *
 * Poderia viver so em memoria (ou em Redis), mas gravar no banco tem um efeito
 * didatico: o carrinho sobrevive ao restart do servidor e deixa claro que a API
 * nao guarda estado de sessao - o estado esta no banco, nao na instancia.
 */
@Entity
@Table(name = "carrinho")
public class Carrinho {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email_usuario", nullable = false, unique = true, length = 160)
    private String emailUsuario;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "carrinho_id", nullable = false)
    private List<ItemCarrinho> itens = new ArrayList<>();

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm = Instant.now();

    protected Carrinho() {
        // exigido pelo JPA
    }

    public Carrinho(String emailUsuario) {
        this.emailUsuario = emailUsuario.toLowerCase();
    }

    public void adicionar(Produto produto, int quantidade) {
        buscarItem(produto.getSku())
                .ifPresentOrElse(
                        item -> item.somarQuantidade(quantidade),
                        () -> itens.add(new ItemCarrinho(produto, quantidade)));
        tocar();
    }

    public void definirQuantidade(String sku, int quantidade) {
        if (quantidade <= 0) {
            remover(sku);
            return;
        }
        buscarItem(sku).ifPresent(item -> item.definirQuantidade(quantidade));
        tocar();
    }

    public void remover(String sku) {
        itens.removeIf(item -> item.getProduto().getSku().equalsIgnoreCase(sku));
        tocar();
    }

    public void limpar() {
        itens.clear();
        tocar();
    }

    private Optional<ItemCarrinho> buscarItem(String sku) {
        return itens.stream()
                .filter(item -> item.getProduto().getSku().equalsIgnoreCase(sku))
                .findFirst();
    }

    private void tocar() {
        this.atualizadoEm = Instant.now();
    }

    public BigDecimal subtotal() {
        return itens.stream()
                .map(ItemCarrinho::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal pesoTotal() {
        return itens.stream()
                .map(ItemCarrinho::pesoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public int quantidadeTotal() {
        return itens.stream().mapToInt(ItemCarrinho::getQuantidade).sum();
    }

    public boolean vazio() {
        return itens.isEmpty();
    }

    public Long getId() {
        return id;
    }

    public String getEmailUsuario() {
        return emailUsuario;
    }

    public List<ItemCarrinho> getItens() {
        return List.copyOf(itens);
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }
}
