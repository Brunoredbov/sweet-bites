package br.com.sweetbites.controller;

import br.com.sweetbites.service.RecursoNaoEncontradoException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** Erros tratados com páginas amigáveis — sem stack trace para o usuário. */
@ControllerAdvice
public class ErroAdvice {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String naoEncontrado() {
        return "erro/404";
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String arquivoGrande(RedirectAttributes redirect) {
        redirect.addFlashAttribute("erro", "A imagem deve ter no máximo 5MB.");
        String origem = ServletUriComponentsBuilder.fromCurrentRequest().build().getPath();
        return "redirect:" + (origem != null && origem.startsWith("/admin/") ? origem : "/admin/cupcakes");
    }
}
