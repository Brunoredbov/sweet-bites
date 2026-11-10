package br.com.sweetbites.dto;

import java.math.BigDecimal;
import java.util.List;

public record ResumoCarrinho(List<LinhaCarrinho> itens, BigDecimal subtotal, BigDecimal valorMinimo,
                             boolean atingiuValorMinimo, List<String> avisos) {
}
