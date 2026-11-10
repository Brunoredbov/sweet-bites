package br.com.sweetbites.dto;

import br.com.sweetbites.model.FormaPagamento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Dados da TELA-ENDERECO + TELA-PAGAMENTO (UC-08). Os dados do cartão só existem durante a
 * requisição: nunca são persistidos (apenas os 4 últimos dígitos).
 */
public class CheckoutForm {

    /** Itens do carrinho no formato "idCupcake:quantidade,idCupcake:quantidade". */
    @NotBlank(message = "Seu carrinho está vazio.")
    private String itens;

    @NotBlank(message = "Informe o CEP.")
    @Pattern(regexp = "^\\d{5}-?\\d{3}$", message = "CEP inválido. Use o formato 00000-000.")
    private String cep;

    /** Preenchidos pelo CEP; editáveis só quando o CEP não traz rua/bairro (CEP geral de cidade). */
    @Size(max = 160, message = "Logradouro muito longo.")
    private String logradouro;

    @Size(max = 80, message = "Bairro muito longo.")
    private String bairro;

    @NotBlank(message = "Informe o número.")
    @Size(max = 10, message = "Número muito longo.")
    private String numero;

    @Size(max = 80, message = "Complemento muito longo.")
    private String complemento;

    @NotNull(message = "Escolha a forma de pagamento.")
    private FormaPagamento formaPagamento;

    private String numeroCartao;
    private String nomeCartao;
    private String validadeCartao;
    private String cvv;
    private Integer parcelas = 1;

    public String getItens() { return itens; }
    public void setItens(String itens) { this.itens = itens; }
    public String getCep() { return cep; }
    public void setCep(String cep) { this.cep = cep; }
    public String getLogradouro() { return logradouro; }
    public void setLogradouro(String logradouro) { this.logradouro = logradouro; }
    public String getBairro() { return bairro; }
    public void setBairro(String bairro) { this.bairro = bairro; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public String getComplemento() { return complemento; }
    public void setComplemento(String complemento) { this.complemento = complemento; }
    public FormaPagamento getFormaPagamento() { return formaPagamento; }
    public void setFormaPagamento(FormaPagamento formaPagamento) { this.formaPagamento = formaPagamento; }
    public String getNumeroCartao() { return numeroCartao; }
    public void setNumeroCartao(String numeroCartao) { this.numeroCartao = numeroCartao; }
    public String getNomeCartao() { return nomeCartao; }
    public void setNomeCartao(String nomeCartao) { this.nomeCartao = nomeCartao; }
    public String getValidadeCartao() { return validadeCartao; }
    public void setValidadeCartao(String validadeCartao) { this.validadeCartao = validadeCartao; }
    public String getCvv() { return cvv; }
    public void setCvv(String cvv) { this.cvv = cvv; }
    public Integer getParcelas() { return parcelas; }
    public void setParcelas(Integer parcelas) { this.parcelas = parcelas; }

    /** Limpa os dados sensíveis antes de devolver o formulário para a tela. */
    public void limparDadosCartao() {
        this.numeroCartao = null;
        this.cvv = null;
    }
}
