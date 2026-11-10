package br.com.sweetbites.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

/** Gera o QR Code do PIX (US-09 CA#2) como imagem PNG. */
@Service
public class QrCodeService {

    public byte[] gerarPng(String conteudo, int tamanho) {
        try {
            BitMatrix matriz = new QRCodeWriter().encode(conteudo, BarcodeFormat.QR_CODE, tamanho, tamanho,
                    Map.of(EncodeHintType.MARGIN, 1));
            BufferedImage imagem = new BufferedImage(tamanho, tamanho, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < tamanho; x++) {
                for (int y = 0; y < tamanho; y++) {
                    imagem.setRGB(x, y, matriz.get(x, y) ? 0x000000 : 0xFFFFFF);
                }
            }
            ByteArrayOutputStream saida = new ByteArrayOutputStream();
            ImageIO.write(imagem, "png", saida);
            return saida.toByteArray();
        } catch (WriterException e) {
            throw new IllegalStateException("Falha ao gerar QR Code", e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
