/*
 * Sweet Bites — comportamento das telas (JavaScript puro, sem dependências).
 * O carrinho fica no localStorage (US-07 RN#2: continua salvo se o app for fechado);
 * preços e estoque são sempre recalculados pelo servidor (/api/carrinho).
 */
(function () {
    'use strict';

    // Carrinho separado por usuário: quem entrar depois no mesmo navegador não herda o carrinho anterior.
    var metaUsuario = document.querySelector('meta[name="usuario-id"]');
    var CHAVE_CARRINHO = 'sweetbites:carrinho:' + (metaUsuario ? metaUsuario.content : 'anonimo');
    try { localStorage.removeItem('sweetbites:carrinho'); } catch (e) { /* chave antiga, compartilhada */ }
    var MAX_POR_ITEM = 20;
    var moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

    function $(seletor, raiz) { return (raiz || document).querySelector(seletor); }
    function $$(seletor, raiz) { return Array.prototype.slice.call((raiz || document).querySelectorAll(seletor)); }

    function csrfHeaders() {
        var token = $('meta[name="_csrf"]');
        var header = $('meta[name="_csrf_header"]');
        var h = {};
        if (token && header) h[header.content] = token.content;
        return h;
    }

    function toast(mensagem) {
        var t = document.createElement('div');
        t.className = 'toast';
        t.setAttribute('role', 'status');
        t.textContent = mensagem;
        document.body.appendChild(t);
        setTimeout(function () { t.remove(); }, 2600);
    }

    function escapar(texto) {
        var d = document.createElement('div');
        d.textContent = texto == null ? '' : String(texto);
        return d.innerHTML;
    }

    // ------------------------------------------------------------------ carrinho

    var Carrinho = {
        ler: function () {
            try {
                var itens = JSON.parse(localStorage.getItem(CHAVE_CARRINHO) || '[]');
                return Array.isArray(itens) ? itens.filter(function (i) { return i && i.id > 0 && i.qtd > 0; }) : [];
            } catch (e) {
                return [];
            }
        },
        salvar: function (itens) {
            try { localStorage.setItem(CHAVE_CARRINHO, JSON.stringify(itens)); } catch (e) { /* armazenamento indisponível */ }
            atualizarContador();
        },
        adicionar: function (id, qtd, estoque) {
            var itens = Carrinho.ler();
            var item = itens.find(function (i) { return i.id === id; });
            var atual = item ? item.qtd : 0;
            var limite = Math.min(MAX_POR_ITEM, estoque || MAX_POR_ITEM);
            if (atual + qtd > limite) {
                toast(limite === MAX_POR_ITEM
                    ? 'Máximo de ' + MAX_POR_ITEM + ' unidades por item.'
                    : 'Só temos ' + limite + ' unidade(s) em estoque.');
                return false;
            }
            if (item) item.qtd += qtd; else itens.push({ id: id, qtd: qtd });
            Carrinho.salvar(itens);
            return true;
        },
        definir: function (id, qtd) {
            var itens = Carrinho.ler().map(function (i) { return i.id === id ? { id: i.id, qtd: qtd } : i; })
                .filter(function (i) { return i.qtd > 0; });
            Carrinho.salvar(itens);
        },
        remover: function (id) { Carrinho.definir(id, 0); },
        limpar: function () { Carrinho.salvar([]); },
        serializar: function () {
            return Carrinho.ler().map(function (i) { return i.id + ':' + i.qtd; }).join(',');
        },
        total: function () { return Carrinho.ler().reduce(function (s, i) { return s + i.qtd; }, 0); }
    };

    function atualizarContador() {
        var total = Carrinho.total();
        $$('[data-contador-carrinho]').forEach(function (b) {
            b.textContent = total > 99 ? '99+' : total;
            b.hidden = total === 0;
        });
    }

    // Sessão expirada: a API responde 401 e o cliente é levado ao login com o aviso de sessão encerrada.
    function verificarSessao(r) {
        if (r.status === 401) {
            window.location.href = '/login?expirada';
            throw new Error('Sessão expirada');
        }
        return r;
    }

    function buscarResumo() {
        return fetch('/api/carrinho?itens=' + encodeURIComponent(Carrinho.serializar()), {
            headers: { 'Accept': 'application/json' }, credentials: 'same-origin'
        }).then(verificarSessao).then(function (r) {
            if (!r.ok) throw new Error('Falha ao carregar o carrinho');
            return r.json();
        });
    }

    // ------------------------------------------------------------------ vitrine

    function iniciarVitrine() {
        var grid = $('#grid-vitrine');
        if (!grid) return;
        var campoBusca = $('#campo-busca');
        var categoria = grid.dataset.categoria || '';
        var temporizador;

        function recarregar() {
            var params = new URLSearchParams();
            if (campoBusca.value.trim()) params.set('busca', campoBusca.value.trim());
            if (categoria) params.set('categoria', categoria);
            fetch('/vitrine/grid?' + params.toString(), { credentials: 'same-origin' })
                .then(function (r) { return r.text(); })
                .then(function (html) {
                    grid.innerHTML = html;
                    history.replaceState(null, '', params.toString() ? '/?' + params.toString() : '/');
                });
        }

        // US-05 CA#1: busca em tempo real (com debounce de 300ms).
        campoBusca.addEventListener('input', function () {
            clearTimeout(temporizador);
            temporizador = setTimeout(recarregar, 300);
        });
        $('#form-busca').addEventListener('submit', function (e) { e.preventDefault(); recarregar(); });

        $$('[data-categoria]').forEach(function (chip) {
            chip.addEventListener('click', function (e) {
                e.preventDefault();
                categoria = chip.dataset.categoria;
                $$('[data-categoria]').forEach(function (c) { c.classList.toggle('ativo', c === chip); });
                recarregar();
            });
        });
    }

    // Botões "Adicionar" (vitrine e detalhe) — delegação de eventos.
    document.addEventListener('click', function (e) {
        var botao = e.target.closest('[data-adicionar]');
        if (!botao) return;
        e.preventDefault();
        var id = Number(botao.dataset.adicionar);
        var estoque = Number(botao.dataset.estoque);
        var saida = $('[data-qtd-saida]');
        var qtd = botao.hasAttribute('data-usa-seletor') && saida ? Number(saida.value) : 1;
        if (Carrinho.adicionar(id, qtd, estoque)) {
            toast(qtd + 'x ' + botao.dataset.nome + ' no carrinho!');
        }
    });

    // ------------------------------------------------------------------ detalhe

    function iniciarDetalhe() {
        var saida = $('[data-qtd-saida]');
        if (!saida) return;
        var maximo = Math.min(MAX_POR_ITEM, Number(saida.dataset.max) || MAX_POR_ITEM);
        function definir(v) { saida.value = Math.max(1, Math.min(maximo, v)); saida.textContent = saida.value; }
        definir(1);
        $('[data-qtd-menos]').addEventListener('click', function () { definir(Number(saida.value) - 1); });
        $('[data-qtd-mais]').addEventListener('click', function () {
            if (Number(saida.value) >= maximo) toast('Quantidade máxima atingida.');
            definir(Number(saida.value) + 1);
        });
    }

    // ------------------------------------------------------------------ carrinho

    function iniciarCarrinho() {
        var lista = $('#itens-carrinho');
        if (!lista) return;
        var vazio = $('#carrinho-vazio');
        var conteudo = $('#carrinho-conteudo');
        var botaoFinalizar = $('#botao-finalizar');
        var avisos = $('#avisos-carrinho');

        function renderizar() {
            if (Carrinho.ler().length === 0) {
                vazio.hidden = false;
                conteudo.hidden = true;
                return;
            }
            buscarResumo().then(function (resumo) {
                // Remove do localStorage o que não existe mais e ajusta quantidades limitadas.
                Carrinho.salvar(resumo.itens.map(function (i) { return { id: i.cupcakeId, qtd: i.quantidade }; }));
                if (resumo.itens.length === 0) { vazio.hidden = false; conteudo.hidden = true; return; }
                vazio.hidden = true;
                conteudo.hidden = false;
                lista.innerHTML = resumo.itens.map(function (i) {
                    var limite = Math.min(MAX_POR_ITEM, i.estoque);
                    return '<li class="item-carrinho">' +
                        '<img src="/imagens/' + i.imagemId + '" alt="" loading="lazy">' +
                        '<div><div class="nome">' + escapar(i.nome) + '</div>' +
                        '<div class="preco">' + moeda.format(i.subtotal) + '</div>' +
                        '<div class="seletor-qtd">' +
                        '<button type="button" data-alterar="' + i.cupcakeId + '" data-delta="-1" aria-label="Diminuir">−</button>' +
                        '<output>' + i.quantidade + '</output>' +
                        '<button type="button" class="mais" data-alterar="' + i.cupcakeId + '" data-delta="1" data-limite="' + limite + '" aria-label="Aumentar">+</button>' +
                        '</div></div>' +
                        '<button type="button" class="remover" data-remover="' + i.cupcakeId + '" aria-label="Remover ' + escapar(i.nome) + '">' +
                        '<svg viewBox="0 0 24 24" width="22" height="22" aria-hidden="true"><path fill="currentColor" d="M9 3h6l1 2h4v2H4V5h4Zm-3 6h12l-1 12H7Z"/></svg></button>' +
                        (i.disponivel ? '' : '<div class="item-aviso">' + (i.estoque === 0 ? 'Indisponível no momento — remova para continuar.' : 'Só temos ' + i.estoque + ' em estoque.') + '</div>') +
                        '</li>';
                }).join('');
                $('#subtotal-carrinho').textContent = moeda.format(resumo.subtotal);
                var podeFinalizar = resumo.atingiuValorMinimo && resumo.itens.every(function (i) { return i.disponivel; });
                botaoFinalizar.classList.toggle('desabilitado', !podeFinalizar);
                botaoFinalizar.setAttribute('aria-disabled', String(!podeFinalizar));
                var mensagens = resumo.avisos.slice();
                if (!resumo.atingiuValorMinimo) {
                    mensagens.unshift('Pedido mínimo de ' + moeda.format(resumo.valorMinimo) + '. Faltam ' +
                        moeda.format(resumo.valorMinimo - resumo.subtotal) + '.');
                }
                avisos.innerHTML = mensagens.map(function (m) { return '<div class="alerta alerta-aviso">' + escapar(m) + '</div>'; }).join('');
            }).catch(function () {
                avisos.innerHTML = '<div class="alerta alerta-erro">Não foi possível carregar o carrinho. Tente novamente.</div>';
            });
        }

        lista.addEventListener('click', function (e) {
            var alterar = e.target.closest('[data-alterar]');
            var remover = e.target.closest('[data-remover]');
            if (alterar) {
                var id = Number(alterar.dataset.alterar);
                var item = Carrinho.ler().find(function (i) { return i.id === id; });
                var nova = (item ? item.qtd : 0) + Number(alterar.dataset.delta);
                var limite = Number(alterar.dataset.limite || MAX_POR_ITEM);
                if (nova > limite) { toast('Quantidade máxima atingida.'); return; }
                Carrinho.definir(id, nova);
                renderizar();
            } else if (remover) {
                Carrinho.remover(Number(remover.dataset.remover));
                renderizar();
            }
        });

        botaoFinalizar.addEventListener('click', function (e) {
            if (botaoFinalizar.getAttribute('aria-disabled') === 'true') {
                e.preventDefault();
                toast('Revise os avisos do carrinho para continuar.');
            }
        });

        renderizar();
    }

    // ------------------------------------------------------------------ checkout

    function iniciarCheckout() {
        var form = $('#form-checkout');
        if (!form) return;
        var campoItens = $('#itens');
        var resumoItens = $('#resumo-itens');
        var subtotal = 0;
        var frete = null;

        function atualizarTotais() {
            $('#resumo-subtotal').textContent = moeda.format(subtotal);
            $('#resumo-frete').textContent = frete === null ? 'Informe o CEP' : moeda.format(frete);
            $('#resumo-total').textContent = moeda.format(subtotal + (frete || 0));
        }

        campoItens.value = Carrinho.serializar();
        if (!campoItens.value) {
            window.location.href = '/carrinho';
            return;
        }
        buscarResumo().then(function (resumo) {
            subtotal = resumo.subtotal;
            resumoItens.innerHTML = resumo.itens.map(function (i) {
                return '<div class="resumo-linha"><span>' + i.quantidade + 'x ' + escapar(i.nome) + '</span><span>' + moeda.format(i.subtotal) + '</span></div>';
            }).join('');
            atualizarTotais();
        });

        // Passo 1 — endereço: consulta CEP + frete (UC-08 passos 5 e 6).
        var cep = $('#cep');
        var infoCep = $('#info-cep');
        var erroCep = $('#erro-cep');
        var ultimoCep = '';

        function consultarCep() {
            var digitos = cep.value.replace(/\D/g, '');
            if (digitos.length !== 8 || digitos === ultimoCep) return;
            ultimoCep = digitos;
            erroCep.textContent = '';
            infoCep.hidden = true;
            fetch('/api/frete?cep=' + digitos, { headers: { 'Accept': 'application/json' }, credentials: 'same-origin' })
                .then(verificarSessao)
                .then(function (r) { return r.json().then(function (corpo) { return { ok: r.ok, corpo: corpo }; }); })
                .then(function (res) {
                    if (!res.ok) {
                        frete = null;
                        erroCep.textContent = res.corpo.erro || 'CEP não encontrado. Verifique o endereço.';
                        atualizarTotais();
                        return;
                    }
                    var e = res.corpo.endereco;
                    frete = res.corpo.valor;
                    var logradouro = $('#logradouro');
                    var bairro = $('#bairro');
                    logradouro.value = e.logradouro || logradouro.value;
                    bairro.value = e.bairro || bairro.value;
                    logradouro.readOnly = !!e.logradouro;
                    bairro.readOnly = !!e.bairro;
                    infoCep.textContent = e.cidade + '/' + e.uf + ' · entrega estimada em ' + res.corpo.tempoEstimadoMin + ' min';
                    infoCep.hidden = false;
                    atualizarTotais();
                })
                .catch(function () {
                    erroCep.textContent = 'Não foi possível consultar o CEP agora.';
                    ultimoCep = '';
                });
        }
        cep.addEventListener('input', function () {
            var d = cep.value.replace(/\D/g, '').slice(0, 8);
            cep.value = d.length > 5 ? d.slice(0, 5) + '-' + d.slice(5) : d;
            consultarCep();
        });
        cep.addEventListener('blur', consultarCep);
        if (cep.value) consultarCep();

        // Passo 2 — pagamento: mostra os campos do cartão só quando necessário.
        var camposCartao = $('#campos-cartao');
        var blocoParcelas = $('#bloco-parcelas');
        function alternarPagamento() {
            var escolhido = $('input[name="formaPagamento"]:checked');
            var cartao = escolhido && escolhido.value === 'CARTAO';
            camposCartao.hidden = !cartao;
            $$('input, select', camposCartao).forEach(function (c) { c.disabled = !cartao; });
            if (blocoParcelas) blocoParcelas.hidden = !cartao;
        }
        $$('input[name="formaPagamento"]').forEach(function (r) { r.addEventListener('change', alternarPagamento); });
        alternarPagamento();

        var numeroCartao = $('#numeroCartao');
        numeroCartao.addEventListener('input', function () {
            numeroCartao.value = numeroCartao.value.replace(/\D/g, '').slice(0, 19).replace(/(\d{4})(?=\d)/g, '$1 ');
        });
        var validade = $('#validadeCartao');
        validade.addEventListener('input', function () {
            var d = validade.value.replace(/\D/g, '').slice(0, 4);
            validade.value = d.length > 2 ? d.slice(0, 2) + '/' + d.slice(2) : d;
        });

        form.addEventListener('submit', function () {
            campoItens.value = Carrinho.serializar();
            var botao = $('#botao-confirmar');
            botao.disabled = true;
            botao.textContent = 'Processando…';
        });
    }

    // ------------------------------------------------------------------ acompanhamento

    function iniciarAcompanhamento() {
        var pagina = $('[data-pedido-id]');
        if (!pagina) return;
        if (pagina.hasAttribute('data-limpar-carrinho')) Carrinho.limpar();
        var ativo = pagina.dataset.ativo === 'true';
        if (!ativo) return;
        var statusAtual = pagina.dataset.status;
        // US-10 CA#1: status em tempo real (consulta a cada 10 segundos).
        setInterval(function () {
            fetch('/api/pedidos/' + pagina.dataset.pedidoId + '/status', { headers: { 'Accept': 'application/json' }, credentials: 'same-origin' })
                .then(verificarSessao)
                .then(function (r) { return r.ok ? r.json() : null; })
                .then(function (s) {
                    if (s && s.status !== statusAtual) {
                        toast('Seu pedido agora está: ' + s.descricao);
                        setTimeout(function () { window.location.replace('/pedidos/' + pagina.dataset.pedidoId); }, 1500);
                    }
                });
        }, 10000);
    }

    // ------------------------------------------------------------------ utilidades

    document.addEventListener('click', function (e) {
        var copiar = e.target.closest('[data-copiar]');
        if (!copiar) return;
        var alvo = document.getElementById(copiar.dataset.copiar);
        if (navigator.clipboard) {
            navigator.clipboard.writeText(alvo.value).then(function () { toast('Código PIX copiado!'); });
        } else {
            alvo.select();
            document.execCommand('copy');
            toast('Código PIX copiado!');
        }
    });

    // Confirmação para ações destrutivas (remover, cancelar).
    document.addEventListener('submit', function (e) {
        var form = e.target;
        if (form.dataset.confirmar && !window.confirm(form.dataset.confirmar)) {
            e.preventDefault();
            return;
        }
        if (form.hasAttribute('data-limpar-carrinho-ao-enviar')) Carrinho.limpar();
    }, true);

    // Pré-visualização da imagem no formulário do admin.
    document.addEventListener('change', function (e) {
        var input = e.target;
        if (!input.matches('[data-preview]') || !input.files || !input.files[0]) return;
        var img = document.getElementById(input.dataset.preview);
        if (input.files[0].size > 5 * 1024 * 1024) {
            toast('A imagem deve ter no máximo 5MB.');
            input.value = '';
            return;
        }
        img.src = URL.createObjectURL(input.files[0]);
        img.hidden = false;
    });

    document.addEventListener('DOMContentLoaded', function () {
        atualizarContador();
        iniciarVitrine();
        iniciarDetalhe();
        iniciarCarrinho();
        iniciarCheckout();
        iniciarAcompanhamento();
    });

    window.SweetBites = { Carrinho: Carrinho, csrfHeaders: csrfHeaders };
})();
