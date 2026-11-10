package br.com.sweetbites.dto;

public record ResultadoPagamento(boolean aprovado, String cartaoFinal, String motivo) {
}
