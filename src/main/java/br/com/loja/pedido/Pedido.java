package br.com.loja.pedido;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import br.com.loja.common.RegraDeNegocioException;
import br.com.loja.pagamento.Pagamento;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * O pedido e a raiz do agregado: itens e pagamento so existem dentro dele
 * (cascade = ALL). Salvar o pedido salva tudo junto, numa unica transacao.
 *
 * Os totais ficam GRAVADOS em colunas em vez de recalculados na leitura: o
 * valor cobrado precisa sobreviver a mudancas futuras de preco, frete ou regra
 * de desconto.
 */
@Entity
@Table(name = "pedido")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String numero;

    @Column(name = "email_cliente", nullable = false, length = 160)
    private String emailCliente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private StatusPedido status;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "pedido_id", nullable = false)
    private List<ItemPedido> itens = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL, optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "pagamento_id", nullable = false)
    private Pagamento pagamento;

    @Column(name = "cep_entrega", nullable = false, length = 9)
    private String cepEntrega;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal desconto;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal frete;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(name = "prazo_entrega_dias", nullable = false)
    private int prazoEntregaDias;

    @Column(name = "detalhe_calculo", length = 400)
    private String detalheCalculo;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm = Instant.now();

    protected Pedido() {
        // exigido pelo JPA
    }

    public Pedido(String numero, String emailCliente, StatusPedido status, List<ItemPedido> itens,
                  Pagamento pagamento, String cepEntrega, BigDecimal subtotal, BigDecimal desconto,
                  BigDecimal frete, BigDecimal total, int prazoEntregaDias, String detalheCalculo) {
        this.numero = numero;
        this.emailCliente = emailCliente;
        this.status = status;
        this.itens = new ArrayList<>(itens);
        this.pagamento = pagamento;
        this.cepEntrega = cepEntrega;
        this.subtotal = subtotal;
        this.desconto = desconto;
        this.frete = frete;
        this.total = total;
        this.prazoEntregaDias = prazoEntregaDias;
        this.detalheCalculo = detalheCalculo;
    }

    /**
     * Troca o status validando a transicao. A regra mora no enum
     * {@link StatusPedido}; aqui so aplicamos e marcamos a data.
     */
    public void mudarStatus(StatusPedido novo) {
        if (status == novo) {
            return;
        }
        if (!status.podeIrPara(novo)) {
            throw new RegraDeNegocioException(
                    "Transicao de status invalida: %s -> %s (permitidas: %s)"
                            .formatted(status, novo, status.proximosPermitidos()));
        }
        if (novo == StatusPedido.PAGO) {
            pagamento.confirmar();
        }
        this.status = novo;
        this.atualizadoEm = Instant.now();
    }

    public int quantidadeTotalItens() {
        return itens.stream().mapToInt(ItemPedido::getQuantidade).sum();
    }

    public Long getId() {
        return id;
    }

    public String getNumero() {
        return numero;
    }

    public String getEmailCliente() {
        return emailCliente;
    }

    public StatusPedido getStatus() {
        return status;
    }

    public List<ItemPedido> getItens() {
        return List.copyOf(itens);
    }

    public Pagamento getPagamento() {
        return pagamento;
    }

    public String getCepEntrega() {
        return cepEntrega;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getDesconto() {
        return desconto;
    }

    public BigDecimal getFrete() {
        return frete;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public int getPrazoEntregaDias() {
        return prazoEntregaDias;
    }

    public String getDetalheCalculo() {
        return detalheCalculo;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }
}
