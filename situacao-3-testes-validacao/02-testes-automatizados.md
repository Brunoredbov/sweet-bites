# 2. Testes automatizados (verificação)

**Resultado da execução em 10/11/2026:** 47 testes, **47 aprovados**, 0 falhas, 0 erros (`./mvnw test`, cerca de 15 s).

```
[INFO] Tests run: 47, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

Para rodar:
```bash
cd situacao-2-codificacao
./mvnw test
```

## Rastreabilidade: teste → requisito

### Testes unitários (37)

| Classe de teste | Teste | Requisito verificado |
|---|---|---|
| `UsuarioTest` | bloqueiaContaNaQuintaTentativaErrada | US-02 RN#1 |
| | loginComSucessoZeraTentativas | US-02 |
| | redefinirSenhaDesbloqueiaConta | US-18 / US-02 RN#2 |
| `StatusPedidoTest` | fluxoNormalDePreparacaoEEntrega | US-16 RN#2 |
| | pedidoEntregueOuCanceladoNaoPodeSerCancelado | US-16 RN#1 |
| | ordemDasConstantesEhALinhaDoTempoExibidaAoCliente | US-10 CA#1 |
| `PagamentoServiceTest` | aprovaCartaoValidoEGuardaSoOsUltimos4Digitos | US-09 RN#3 |
| | recusaCartaoTerminadoEm0002 | UC-08 curso Beta |
| | rejeitaNumeroQueNaoPassaNoLuhn | US-09 |
| | rejeitaCartaoVencido / aceitaCartaoQueVenceNoMesCorrente | US-09 |
| | parcelamentoMaximoDe3xSemJuros | US-09 RN#1 |
| | codigoPixIdentificaOPedido | US-09 CA#2 |
| `FreteServiceTest` | mesmaCidadeDaLojaIgnorandoAcentos | US-08 RN#3 |
| | outraCidadeDoMesmoEstado | US-08 RN#3 |
| | outroEstadoNaoTemEntrega | US-08 RN#3 |
| | cepInexistenteOuMalformado | UC-08 curso Alfa |
| `CarrinhoServiceTest` | interpretaTextoDoNavegadorIgnorandoEntradasInvalidas | US-07 (robustez) |
| | recalculaPrecosNoServidorELimitaA20PorItem | US-07 RN#1 e RN#3 |
| | sinalizaPedidoAbaixoDoMinimoEItemSemEstoque | US-08 RN#2, US-04 RN#1 |
| | removeItemQueSaiuDoCardapio | US-14 RN#2 |
| `ImagemValidatorTest` | reconhecePngEJpegPelosBytes | US-14 RNF#1 |
| | rejeitaArquivoDisfarcadoDeImagem | US-14 RNF#1 (segurança) |
| | rejeitaImagemMaiorQue5MB | US-14 RNF#1 |
| `PedidoServiceTest` | pedidoComCartaoAprovadoVaiParaPreparacaoEBaixaEstoque | US-08, US-15 RN#1 |
| | cartaoRecusadoNaoGravaPedidoNemMexeNoEstoque | UC-08 curso Beta |
| | pixFicaAguardandoPagamentoComValidadeDe30Minutos | US-09 RN#2 |
| | pedidoMinimoDe15Reais | US-08 RN#2 |
| | naoVendeMaisDoQueOEstoque | UC-08 curso Gama |
| | maximoDe20UnidadesPorItem | US-07 RN#1 |
| | carrinhoVazio | UC-08 pré-condição |
| | adminAvancaStatusSomenteNoFluxoENotificaCliente | US-16 CA#2 e RN#2 |
| | entregaEmDinheiroAprovaOPagamento | US-16 RN#3 |
| | pedidoEntregueNaoPodeSerCancelado | US-16 RN#1 |
| | cancelarDevolveEstoqueEEstornaPagamentoAprovado | US-19 RN#1, US-15 |
| | pixVencidoCancelaPedidoEDevolveEstoque | US-09 RN#2 |
| | clienteNaoAcessaPedidoDeOutroCliente | RNF-G3 (segurança) |

### Testes de integração (10): `FluxosWebTest`

Sobem a aplicação inteira (controllers, segurança, banco com Flyway). Só a API de CEP é substituída por um dublê de teste.

| Teste | O que verifica | Requisito |
|---|---|---|
| visitanteEhRedirecionadoParaOLogin | Páginas protegidas exigem login | US-04 |
| cadastroValidaSenhaEEmailUnico | Senha fraca recusada; e-mail duplicado (mesmo com maiúsculas) recusado | US-01 CA#1 e CA#2 |
| contaBloqueadaAposCincoTentativasErradas | 5ª tentativa errada bloqueia; senha certa não entra depois | US-02 RN#1 |
| recuperarSenhaDesbloqueiaEPermiteNovoLogin | Fluxo completo de redefinição; token não pode ser reutilizado | US-18 |
| emailInexistenteRecebeAMesmaRespostaNaRecuperacao | Não revela quem tem conta | US-18 RN#2 |
| clienteNaoAcessaPainelAdmin | Cliente recebe 403 em `/admin`; admin acessa e filtra pedidos | US-14 a US-16 |
| buscaNaVitrineIgnoraMaiusculasEProcuraIngredientes | "MORANGO" encontra por nome e ingrediente; mostra "Produto não encontrado"; mostra "Indisponível" | US-05, US-04 RN#1 |
| pedidoCompletoComPixEOwnership | Compra via PIX, baixa de estoque, QR Code, outro cliente recebe 404, pagamento simulado | US-08, US-09, US-10, RNF-G3 |
| checkoutRejeitaPedidoAbaixoDoMinimoMostrandoAMensagem | Mensagem do pedido mínimo na tela | US-08 RN#2 |
| apiDeFreteDevolveErroAmigavelParaCepInvalido | CEP inválido → erro amigável; CEP de SP → R$ 7,00 / 45 min | UC-08 Alfa, US-08 RN#3 |

## Testes manuais de sistema (desenvolvedor)

Executados no navegador com tela de celular (375×812). As evidências estão em [`evidencias/`](evidencias/).

| # | Cenário | Resultado | Evidência |
|---|---|---|---|
| M01 | Cadastro com campos vazios ou senha fraca | ✅ Mensagens por campo | [bug05-corrigido-uma-mensagem.jpg](evidencias/bug05-corrigido-uma-mensagem.jpg) |
| M02 | Login e vitrine com saudação | ✅ | — |
| M03 | Busca "MORANGO" em tempo real | ✅ 2 resultados (nome e ingrediente) | — |
| M04 | Carrinho com 2 itens e subtotal | ✅ | — |
| M05 | Checkout com CEP 01310-100 (Av. Paulista) | ✅ após a correção do BUG-01 | [bug01-corrigido-frete-45min.jpg](evidencias/bug01-corrigido-frete-45min.jpg) |
| M06 | Cartão 4000 0000 0000 0002 | ✅ Recusado, nada gravado, endereço mantido | [teste-cartao-recusado.jpg](evidencias/teste-cartao-recusado.jpg) |
| M07 | Pedido via PIX + simular pagamento | ✅ "Em preparação" | [teste-pix-pago-em-preparacao.jpg](evidencias/teste-pix-pago-em-preparacao.jpg) |
| M08 | Admin muda o status; a tela do cliente atualiza sozinha | ✅ em menos de 10 s, com notificação | [teste-status-tempo-real.jpg](evidencias/teste-status-tempo-real.jpg) |
| M09 | Pedido com cartão 4111… | ✅ Confirmado, carrinho esvaziado | [bug02-03-corrigido-confirmacao.jpg](evidencias/bug02-03-corrigido-confirmacao.jpg) |
| M10 | Upload de arquivo falso como imagem | ✅ Recusado: "Formato de imagem não suportado" | — |
| M11 | Admin tenta voltar o status de "Saiu para entrega" para "Em preparação" | ✅ Recusado com mensagem | — |
| M12 | Acesso a `/admin` sem login | ✅ Redireciona para o login | — |
| M13 | CEP do Rio de Janeiro | ✅ "Ainda não entregamos em Rio de Janeiro/RJ" | — |
