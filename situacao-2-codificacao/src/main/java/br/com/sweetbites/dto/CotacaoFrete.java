package br.com.sweetbites.dto;

import java.math.BigDecimal;

public record CotacaoFrete(EnderecoCep endereco, BigDecimal valor, int tempoEstimadoMin) {
}
