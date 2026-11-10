# 6. Laudo de qualidade

| Item | Informação |
|---|---|
| Sistema | Sweet Bites: loja virtual de cupcakes gourmet |
| Versão avaliada | Branch `main` do repositório [Brunoredbov/sweet-bites](https://github.com/Brunoredbov/sweet-bites) |
| Data | 10/11/2026 |
| Responsável | Bruno Villanova da Silva |
| Ambiente | Java 17 + Spring Boot 4, PostgreSQL 16 (Docker); navegador com tela de celular 375×812 e de desktop 1280×800 |

## 1. Resumo

| Indicador | Resultado |
|---|---|
| Testes automatizados | **47 de 47 aprovados** (37 unitários + 10 de integração) |
| Histórias das Sprints 1 a 3 | 16 de 16 implementadas (4 com ajustes documentados) |
| Cenários manuais executados | 13 de 13 aprovados, após as correções |
| Defeitos encontrados | **5** (3 altos, 1 médio, 1 baixo) |
| Defeitos corrigidos e retestados | **5 de 5** |
| Defeitos em aberto | 0 |

**Parecer:** o sistema está **apto para os testes com usuários (validação)**. Os pontos da seção 4 são limitações conhecidas e documentadas, não defeitos.

## 2. Defeitos encontrados e correções

### BUG-01: Frete da cidade de São Paulo calculado como "outra cidade" · Severidade **Alta**
- **Como foi encontrado:** teste manual M05, checkout com o CEP 01310-100 (Av. Paulista).
- **Comportamento:** o sistema mostrava "entrega estimada em 120 min" e cobrava **R$ 15,00**, quando o certo seria 45 min e R$ 7,00. Ou seja, cobrava a mais de todo cliente da capital.
- **Causa:** o arquivo `application.properties` é lido pelo Java na codificação ISO-8859-1. O nome `São Paulo`, salvo em UTF-8, virou `SÃ£o Paulo` e nunca era igual à cidade devolvida pelo ViaCEP.
- **Correção:** o valor foi escrito com escape Unicode (`São Paulo`), e o teste unitário passou a cobrir a comparação com e sem acento.
- **Reteste:** ✅ R$ 7,00 / 45 min. Também coberto pelos testes `FreteServiceTest.mesmaCidadeDaLojaIgnorandoAcentos` e `FluxosWebTest.apiDeFreteDevolveErroAmigavelParaCepInvalido`.
- **Evidências:** antes, [bug01-frete-sao-paulo-120min.jpg](evidencias/bug01-frete-sao-paulo-120min.jpg); depois, [bug01-corrigido-frete-45min.jpg](evidencias/bug01-corrigido-frete-45min.jpg).

### BUG-02: Carrinho não era esvaziado depois do pedido confirmado · Severidade **Média**
- **Como foi encontrado:** teste manual M07. Depois de confirmar o pedido, o ícone do carrinho continuava mostrando "2".
- **Risco:** o cliente poderia comprar os mesmos itens de novo sem perceber.
- **Causa:** a tela marcava o pedido recém-confirmado com o atributo `data-limpar-carrinho=""`, mas o Thymeleaf **remove atributos com valor vazio**, então o JavaScript nunca recebia o aviso.
- **Correção:** o atributo passou a ter o valor `"true"`.
- **Reteste:** ✅ o carrinho zera na tela de confirmação.
- **Evidências:** antes, [bug02-carrinho-nao-esvaziou.jpg](evidencias/bug02-carrinho-nao-esvaziou.jpg); depois, [bug02-03-corrigido-confirmacao.jpg](evidencias/bug02-03-corrigido-confirmacao.jpg).

### BUG-03: Erro 500 na tela de acompanhamento do pedido · Severidade **Alta**
- **Como foi encontrado:** teste manual M07. Logo depois de concluir um pedido via PIX, aparecia a página "Algo deu errado".
- **Causa:** a linha do tempo usava `#lists.indexOf(...)`, uma função que **não existe** no Thymeleaf (*SpelEvaluationException: Method indexOf cannot be found*).
- **Correção:** a etapa atual passou a ser obtida pela ordem do próprio enum (`pedido.status.ordinal()`). Um teste (`StatusPedidoTest.ordemDasConstantesEhALinhaDoTempoExibidaAoCliente`) garante que a ordem não seja alterada por engano.
- **Reteste:** ✅ a tela abre com a linha do tempo correta.
- **Evidências:** antes, [bug03-erro-500-linha-do-tempo.jpg](evidencias/bug03-erro-500-linha-do-tempo.jpg); depois, [bug02-03-corrigido-confirmacao.jpg](evidencias/bug02-03-corrigido-confirmacao.jpg).

### BUG-04: Erro 500 na lista de pedidos do administrador · Severidade **Alta**
- **Como foi encontrado:** varredura das telas do admin (`/admin/pedidos`), com e sem filtros.
- **Causa:** a consulta usava parâmetros opcionais no formato `(:inicio IS NULL OR ...)`. O PostgreSQL não consegue deduzir o tipo de um parâmetro nulo (*ERROR: could not determine data type of parameter $3*). Os testes automatizados não pegaram o erro porque rodam no H2, que aceita essa construção.
- **Correção:** os parâmetros passaram a ser sempre preenchidos (lista de status e período padrão de 2000 a 2999). A mesma correção foi aplicada **preventivamente** à consulta da vitrine, que tinha a mesma construção.
- **Reteste:** ✅ `/admin/pedidos` sem filtro, com status e com período: todos com HTTP 200 no PostgreSQL real.
- **Lição aprendida:** testar também contra o banco de produção (PostgreSQL), não só contra o H2.

### BUG-05: Duas mensagens de erro no mesmo campo do cadastro · Severidade **Baixa**
- **Como foi encontrado:** durante a captura das telas para a documentação. Com o campo telefone vazio, apareciam "Informe um telefone válido…" **e** "Informe seu telefone."; com a senha vazia, o mesmo acontecia.
- **Causa:** duas validações (`@NotBlank` e `@Pattern`) falhavam juntas e a tela mostrava todas.
- **Correção:** todas as telas de formulário passaram a mostrar **apenas a primeira mensagem** de cada campo.
- **Reteste:** ✅ uma mensagem por campo.
- **Evidências:** antes, [bug05-mensagens-duplicadas.jpg](evidencias/bug05-mensagens-duplicadas.jpg); depois, [bug05-corrigido-uma-mensagem.jpg](evidencias/bug05-corrigido-uma-mensagem.jpg).

## 3. Regras de negócio verificadas com sucesso

| Regra | Evidência |
|---|---|
| Cartão recusado não grava pedido nem altera estoque (UC-08 curso Beta) | [teste-cartao-recusado.jpg](evidencias/teste-cartao-recusado.jpg) + teste `cartaoRecusadoNaoGravaPedidoNemMexeNoEstoque` |
| PIX pago leva o pedido para "Em preparação" | [teste-pix-pago-em-preparacao.jpg](evidencias/teste-pix-pago-em-preparacao.jpg) |
| Mudança de status pelo admin aparece para o cliente em menos de 10 s, com notificação | [teste-status-tempo-real.jpg](evidencias/teste-status-tempo-real.jpg) |
| Bloqueio de conta após 5 tentativas | teste `contaBloqueadaAposCincoTentativasErradas` |
| Cliente não acessa pedido de outro cliente (404) | teste `pedidoCompletoComPixEOwnership` |
| Upload de arquivo que não é imagem é recusado | teste `rejeitaArquivoDisfarcadoDeImagem` + teste manual M10 |

## 4. Limitações conhecidas (não são defeitos)

| # | Limitação | Impacto | Plano |
|---|---|---|---|
| L1 | Pagamento, PIX, e-mail e push são simulados | Nenhuma cobrança ou e-mail real | Trocar as implementações dos services (ver [Arquitetura](../situacao-1-documentacao/08-arquitetura.md)) |
| L2 | Endereços não ficam salvos na conta | O cliente digita o CEP a cada pedido | Sprint 4 |
| L3 | Sessões ficam na memória do servidor | Reiniciar o servidor desconecta os usuários | Aceitável no plano gratuito; sessão no banco se necessário |
| L4 | Hospedagem gratuita "dorme" depois de 15 min sem uso | O primeiro acesso pode levar cerca de 1 minuto | Avisar os testadores no roteiro |
| L5 | US-11, US-12 (repetir pedido), US-13 e US-17 não implementadas | Fora do escopo das Sprints 1 a 3 | Sprint 4 |

## 5. Defeitos reportados pelos testadores

> **A preencher** depois dos testes com os 5 colegas (ver [resultados](05-resultados-testes-usuarios.md)). Para cada item: descrição, testador, severidade, correção aplicada, reteste e print.

| ID | Descrição | Testador | Severidade | Correção | Reteste |
|---|---|---|---|---|---|
| | | | | | |
