package br.com.sweetbites.service;

public interface EmailService {
    void enviar(String destinatario, String assunto, String corpo);
}
