package br.com.loja.pagamento;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Registro do pagamento de um pedido. Gravado na MESMA transacao do pedido e da
 * baixa de estoque: ou os tres existem, ou nenhum existe.
 */
@Entity
@Table(name = "pagamento")
public class Pagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MetodoPagamento metodo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusPagamento status;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Column(name = "identificador_externo", nullable = false, length = 60)
    private String identificadorExterno;

    @Column(length = 200)
    private String mensagem;

    @Column(name = "registrado_em", nullable = false)
    private Instant registradoEm = Instant.now();

    protected Pagamento() {
        // exigido pelo JPA
    }

    public Pagamento(MetodoPagamento metodo, StatusPagamento status, BigDecimal valor,
                     String identificadorExterno, String mensagem) {
        this.metodo = metodo;
        this.status = status;
        this.valor = valor;
        this.identificadorExterno = identificadorExterno;
        this.mensagem = mensagem;
    }

    public Long getId() {
        return id;
    }

    public MetodoPagamento getMetodo() {
        return metodo;
    }

    public StatusPagamento getStatus() {
        return status;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public String getIdentificadorExterno() {
        return identificadorExterno;
    }

    public String getMensagem() {
        return mensagem;
    }

    public Instant getRegistradoEm() {
        return registradoEm;
    }

    public void confirmar() {
        this.status = StatusPagamento.AUTORIZADO;
        this.mensagem = "Pagamento confirmado";
    }
}
