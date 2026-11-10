package br.com.sweetbites.service;

import br.com.sweetbites.dto.EnderecoCep;

import java.util.Optional;

public interface CepService {
    /** Consulta o CEP (8 dígitos). Vazio se o CEP não existir. */
    Optional<EnderecoCep> consultar(String cep);
}
