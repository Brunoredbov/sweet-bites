package br.com.sweetbites.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;

/** RNF (US-14): upload aceita apenas JPG e PNG com até 5MB — conferindo os bytes, não só a extensão. */
public final class ImagemValidator {

    public static final long TAMANHO_MAXIMO = 5L * 1024 * 1024;

    private ImagemValidator() {
    }

    /** Retorna o content-type real (image/jpeg ou image/png) ou lança RegraNegocioException. */
    public static String validar(MultipartFile arquivo) {
        if (arquivo.getSize() > TAMANHO_MAXIMO) {
            throw new RegraNegocioException("A imagem deve ter no máximo 5MB.");
        }
        byte[] cabecalho;
        try {
            cabecalho = arquivo.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (ehPng(cabecalho)) return "image/png";
        if (ehJpeg(cabecalho)) return "image/jpeg";
        throw new RegraNegocioException("Formato de imagem não suportado. Envie um arquivo JPG ou PNG.");
    }

    static boolean ehPng(byte[] b) {
        return b.length > 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G';
    }

    static boolean ehJpeg(byte[] b) {
        return b.length > 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF;
    }
}
