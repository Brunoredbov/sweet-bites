package br.com.sweetbites.service;

import br.com.sweetbites.dto.CategoriaForm;
import br.com.sweetbites.dto.CupcakeForm;
import br.com.sweetbites.model.Categoria;
import br.com.sweetbites.model.Cupcake;
import br.com.sweetbites.model.ImagemProduto;
import br.com.sweetbites.model.StatusPedido;
import br.com.sweetbites.repository.CategoriaRepository;
import br.com.sweetbites.repository.CupcakeRepository;
import br.com.sweetbites.repository.ImagemProdutoRepository;
import br.com.sweetbites.repository.PedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Locale;

/** Vitrine (US-04/05/06) e gestão de produtos, estoque e categorias (US-14/15). */
@Service
public class CatalogoService {

    private final CupcakeRepository cupcakeRepository;
    private final CategoriaRepository categoriaRepository;
    private final ImagemProdutoRepository imagemRepository;
    private final PedidoRepository pedidoRepository;

    public CatalogoService(CupcakeRepository cupcakeRepository, CategoriaRepository categoriaRepository,
                           ImagemProdutoRepository imagemRepository, PedidoRepository pedidoRepository) {
        this.cupcakeRepository = cupcakeRepository;
        this.categoriaRepository = categoriaRepository;
        this.imagemRepository = imagemRepository;
        this.pedidoRepository = pedidoRepository;
    }

    // ---------- Vitrine ----------

    /** Busca case-insensitive por nome ou ingrediente, com filtro opcional de categoria. */
    @Transactional(readOnly = true)
    public List<Cupcake> vitrine(String busca, Long categoriaId) {
        String termo = (busca == null || busca.isBlank()) ? "%"
                : "%" + busca.trim().toLowerCase(Locale.ROOT).replace("%", "").replace("_", "") + "%";
        return cupcakeRepository.buscarVitrine(categoriaId == null, categoriaId == null ? 0L : categoriaId, termo);
    }

    @Transactional(readOnly = true)
    public Cupcake detalhe(Long id) {
        return cupcakeRepository.findAtivoById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cupcake não encontrado"));
    }

    @Transactional(readOnly = true)
    public List<Categoria> categorias() {
        return categoriaRepository.findAllByOrderByNomeAsc();
    }

    @Transactional(readOnly = true)
    public ImagemProduto imagem(Long id) {
        return imagemRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Imagem não encontrada"));
    }

    // ---------- Admin: produtos (US-14) ----------

    @Transactional(readOnly = true)
    public List<Cupcake> listarAtivos() {
        return cupcakeRepository.findAtivosComCategoria();
    }

    @Transactional
    public Cupcake criar(CupcakeForm form, MultipartFile imagem) {
        if (imagem == null || imagem.isEmpty()) {
            throw new RegraNegocioException("Envie uma foto do cupcake (JPG ou PNG, até 5MB).");
        }
        Cupcake c = new Cupcake(form.getNome().trim(), form.getDescricao().trim(), form.getIngredientes().trim(),
                form.getPreco(), form.getEstoque(), buscarCategoria(form.getCategoriaId()));
        c.setImagem(lerImagem(imagem));
        return cupcakeRepository.save(c);
    }

    @Transactional
    public Cupcake atualizar(Long id, CupcakeForm form, MultipartFile imagem) {
        Cupcake c = detalhe(id);
        c.setNome(form.getNome().trim());
        c.setDescricao(form.getDescricao().trim());
        c.setIngredientes(form.getIngredientes().trim());
        c.setPreco(form.getPreco());
        c.setEstoque(form.getEstoque());
        c.setCategoria(buscarCategoria(form.getCategoriaId()));
        if (imagem != null && !imagem.isEmpty()) {
            c.setImagem(lerImagem(imagem));
        }
        return c;
    }

    /**
     * RN (US-14): cupcake em pedido ativo não pode ser excluído. A exclusão é lógica
     * (ativo = false) para preservar o histórico dos pedidos já finalizados.
     */
    @Transactional
    public void remover(Long id) {
        Cupcake c = detalhe(id);
        if (pedidoRepository.existeCupcakeEmPedidoAtivo(id, StatusPedido.ATIVOS)) {
            throw new RegraNegocioException("\"" + c.getNome() + "\" está em pedidos em andamento e não pode ser removido agora.");
        }
        c.setAtivo(false);
    }

    // ---------- Admin: estoque (US-15) ----------

    @Transactional
    public void atualizarEstoque(Long id, int quantidade) {
        if (quantidade < 0 || quantidade > 10000) {
            throw new RegraNegocioException("Informe uma quantidade entre 0 e 10000.");
        }
        detalhe(id).setEstoque(quantidade);
    }

    @Transactional(readOnly = true)
    public List<Cupcake> estoqueBaixo() {
        return cupcakeRepository.findEstoqueBaixo(Cupcake.LIMITE_ESTOQUE_BAIXO);
    }

    // ---------- Admin: categorias ----------

    @Transactional
    public Categoria criarCategoria(CategoriaForm form) {
        String nome = form.getNome().trim();
        if (categoriaRepository.existsByNomeIgnoreCase(nome)) {
            throw new RegraNegocioException("Já existe uma categoria chamada \"" + nome + "\".");
        }
        return categoriaRepository.save(new Categoria(nome, form.getDescricao()));
    }

    @Transactional
    public void removerCategoria(Long id) {
        Categoria categoria = buscarCategoria(id);
        if (cupcakeRepository.countByCategoriaId(id) > 0) {
            throw new RegraNegocioException("A categoria \"" + categoria.getNome() + "\" possui cupcakes e não pode ser removida.");
        }
        categoriaRepository.delete(categoria);
    }

    private Categoria buscarCategoria(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new RegraNegocioException("Categoria inválida."));
    }

    private static ImagemProduto lerImagem(MultipartFile arquivo) {
        String tipo = ImagemValidator.validar(arquivo);
        try {
            return new ImagemProduto(tipo, arquivo.getBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
