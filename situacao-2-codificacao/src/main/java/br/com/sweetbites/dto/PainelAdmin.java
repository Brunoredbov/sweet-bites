package br.com.sweetbites.dto;

import br.com.sweetbites.model.Cupcake;
import br.com.sweetbites.model.StatusPedido;

import java.util.List;
import java.util.Map;

public record PainelAdmin(Map<StatusPedido, Long> pedidosPorStatus, List<Cupcake> estoqueBaixo) {
}
