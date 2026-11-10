package br.com.sweetbites.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Envio de e-mail SIMULADO: a mensagem vai para o log da aplicação. Trocar por uma
 * implementação real (SMTP/Resend) não exige mudar quem usa a interface EmailService.
 */
@Service
public class EmailSimuladoService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailSimuladoService.class);

    @Override
    public void enviar(String destinatario, String assunto, String corpo) {
        log.info("[e-mail simulado] para={} assunto=\"{}\"", mascarar(destinatario), assunto);
    }

    private static String mascarar(String email) {
        int arroba = email.indexOf('@');
        return arroba <= 1 ? "***" : email.charAt(0) + "***" + email.substring(arroba);
    }
}
