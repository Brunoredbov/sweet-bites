package br.com.sweetbites.service;

import br.com.sweetbites.dto.EnderecoCep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.Optional;

/** RN10 (UC-08): validação de CEP pela API pública ViaCEP (https://viacep.com.br). */
@Service
public class ViaCepService implements CepService {

    private static final Logger log = LoggerFactory.getLogger(ViaCepService.class);

    record RespostaViaCep(String cep, String logradouro, String bairro, String localidade, String uf, Object erro) {
    }

    private final RestClient restClient;

    public ViaCepService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        // RNF (US-08): cálculo do frete em até 3 segundos.
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(2));
        this.restClient = RestClient.builder()
                .baseUrl("https://viacep.com.br/ws")
                .requestFactory(factory)
                .build();
    }

    @Override
    public Optional<EnderecoCep> consultar(String cep) {
        try {
            RespostaViaCep r = restClient.get().uri("/{cep}/json/", cep).retrieve().body(RespostaViaCep.class);
            if (r == null || r.erro() != null || r.uf() == null) {
                return Optional.empty();
            }
            return Optional.of(new EnderecoCep(cep, valor(r.logradouro()), valor(r.bairro()), r.localidade(), r.uf()));
        } catch (RestClientException e) {
            log.warn("[cep] falha ao consultar ViaCEP: {}", e.getMessage());
            throw new RegraNegocioException("Não foi possível consultar o CEP agora. Tente novamente em instantes.");
        }
    }

    private static String valor(String s) {
        return s == null ? "" : s;
    }
}
