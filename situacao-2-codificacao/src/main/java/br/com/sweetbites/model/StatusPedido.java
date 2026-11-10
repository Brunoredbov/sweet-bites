package br.com.sweetbites.model;

import java.util.EnumSet;
import java.util.Set;

/** A ORDEM das constantes é a linha do tempo exibida ao cliente (usa ordinal()). */
public enum StatusPedido {
    AGUARDANDO_PAGAMENTO("Aguardando pagamento"),
    EM_PREPARACAO("Em preparação"),
    SAIU_PARA_ENTREGA("Saiu para entrega"),
    ENTREGUE("Entregue"),
    CANCELADO("Cancelado");

    /** Status de pedidos "em andamento" (allow-list explícita). */
    public static final Set<StatusPedido> ATIVOS =
            EnumSet.of(AGUARDANDO_PAGAMENTO, EM_PREPARACAO, SAIU_PARA_ENTREGA);

    private final String descricao;

    StatusPedido(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    /** Próximo status no fluxo normal de preparação/entrega, ou null se não houver. */
    public StatusPedido proximo() {
        return switch (this) {
            case EM_PREPARACAO -> SAIU_PARA_ENTREGA;
            case SAIU_PARA_ENTREGA -> ENTREGUE;
            default -> null;
        };
    }

    /** RN (US-16): pedidos entregues (ou já cancelados) não podem ser cancelados. */
    public boolean permiteCancelamento() {
        return ATIVOS.contains(this);
    }
}
