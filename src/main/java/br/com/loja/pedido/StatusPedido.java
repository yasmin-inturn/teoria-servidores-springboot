package br.com.loja.pedido;

import java.util.Set;

/**
 * Fluxo de status do pedido, com as transicoes permitidas declaradas no proprio
 * enum. Isso evita o classico "if gigante" no servico e deixa a regra em um
 * lugar so: um pedido ENTREGUE nunca volta para PAGO, e um CANCELADO e final.
 *
 *   AGUARDANDO_PAGAMENTO -> PAGO | CANCELADO
 *   PAGO                 -> EM_SEPARACAO | CANCELADO
 *   EM_SEPARACAO         -> ENVIADO | CANCELADO
 *   ENVIADO              -> ENTREGUE
 *   ENTREGUE             -> (final)
 *   CANCELADO            -> (final)
 */
public enum StatusPedido {

    AGUARDANDO_PAGAMENTO,
    PAGO,
    EM_SEPARACAO,
    ENVIADO,
    ENTREGUE,
    CANCELADO;

    public Set<StatusPedido> proximosPermitidos() {
        return switch (this) {
            case AGUARDANDO_PAGAMENTO -> Set.of(PAGO, CANCELADO);
            case PAGO -> Set.of(EM_SEPARACAO, CANCELADO);
            case EM_SEPARACAO -> Set.of(ENVIADO, CANCELADO);
            case ENVIADO -> Set.of(ENTREGUE);
            case ENTREGUE, CANCELADO -> Set.of();
        };
    }

    public boolean podeIrPara(StatusPedido destino) {
        return proximosPermitidos().contains(destino);
    }

    public boolean finalizado() {
        return this == ENTREGUE || this == CANCELADO;
    }

    /** Antes do envio, cancelar devolve os itens ao estoque. */
    public boolean devolveEstoqueAoCancelar() {
        return this == AGUARDANDO_PAGAMENTO || this == PAGO || this == EM_SEPARACAO;
    }
}
