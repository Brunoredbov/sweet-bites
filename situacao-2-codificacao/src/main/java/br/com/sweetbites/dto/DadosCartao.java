package br.com.sweetbites.dto;

public record DadosCartao(String numero, String nomeTitular, String validade, String cvv, int parcelas) {
}
