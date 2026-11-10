package br.com.sweetbites.service;

/** Violação de regra de negócio; a mensagem é exibida ao usuário. */
public class RegraNegocioException extends RuntimeException {
    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
