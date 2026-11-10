package br.com.sweetbites.dto;

import java.math.BigDecimal;

/** Item do carrinho com preço e disponibilidade atuais, calculados no servidor. */
public record LinhaCarrinho(Long cupcakeId, String nome, BigDecimal precoUnitario, int quantidade,
                            BigDecimal subtotal, int estoque, boolean disponivel, Long imagemId) {
}
