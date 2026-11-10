package br.com.sweetbites.model;

public enum FormaPagamento {
    CARTAO("Cartão de crédito"),
    PIX("PIX"),
    DINHEIRO("Dinheiro na entrega");

    private final String descricao;

    FormaPagamento(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
