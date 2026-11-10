package br.com.sweetbites.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImagemValidatorTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0};

    @Test
    void reconhecePngEJpegPelosBytes() {
        assertThat(ImagemValidator.validar(new MockMultipartFile("imagem", "x.png", "image/png", PNG))).isEqualTo("image/png");
        assertThat(ImagemValidator.validar(new MockMultipartFile("imagem", "x.jpg", "image/jpeg", JPEG))).isEqualTo("image/jpeg");
    }

    @Test
    void rejeitaArquivoDisfarcadoDeImagem() {
        MockMultipartFile falso = new MockMultipartFile("imagem", "x.png", "image/png", "<script>".getBytes());
        assertThatThrownBy(() -> ImagemValidator.validar(falso)).isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void rejeitaImagemMaiorQue5MB() {
        byte[] grande = new byte[(int) ImagemValidator.TAMANHO_MAXIMO + 1];
        System.arraycopy(PNG, 0, grande, 0, PNG.length);
        assertThatThrownBy(() -> ImagemValidator.validar(new MockMultipartFile("imagem", "x.png", "image/png", grande)))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("5MB");
    }
}
