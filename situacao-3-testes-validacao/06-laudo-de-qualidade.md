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
| Testes automatizados | **56 de 56 aprovados** (37 unitários + 19 de integração) |
| Histórias das Sprints 1 a 3 | 17 de 17 implementadas (4 com ajustes documentados) |
| Cenários manuais executados | 13 de 13 aprovados, após as correções |
| Roteiro de aceitação (20 tarefas) | 54 de 54 verificações aprovadas |
| Defeitos encontrados | **14**: 5 na 1ª rodada (3 altos, 1 médio, 1 baixo) e 9 na 2ª rodada (3 médios, 6 baixos) |
| Defeitos corrigidos e retestados | **14 de 14** |
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

## 2.1 Segunda rodada: revisão de qualidade pós-aceitação (10/11/2026)

Depois do roteiro de aceitação, o sistema foi revisado procurando **casos extremos e problemas de usabilidade**: sessão expirada, troca de usuário no mesmo aparelho, URLs digitadas à mão, filtros incoerentes e arquivos grandes. Os 9 problemas encontrados foram corrigidos, ganharam testes automatizados (`CorrecoesRodada2Test`, 9 testes) e foram retestados no navegador. A versão antiga foi fotografada antes da correção, e a nova, depois.

| ID | Problema encontrado | Sev. | Correção | Antes → depois |
|---|---|---|---|---|
| BUG-06 | Com a sessão expirada, enviar um formulário (ex.: confirmar o pedido) mostrava **"Acesso negado – você não tem permissão"**, mensagem errada e assustadora | Média | Token de segurança (CSRF) vencido agora leva ao login com "Sua sessão foi encerrada. Entre novamente." | [antes](evidencias/bug06-sessao-expirada-acesso-negado.jpg) → [depois](evidencias/bug06-corrigido-volta-ao-login.jpg) |
| BUG-07 | Sem sessão, a API do acompanhamento respondia com a página de login (HTML), e a tela do pedido **parava de atualizar sem avisar** | Baixa | A API responde 401 e o JavaScript leva o cliente ao login | teste `bug07ApiSemSessaoResponde401ParaOJavaScript` |
| BUG-08 | "Alterar senha" no perfil abria o fluxo de **"Esqueci minha senha"** (pedia e-mail e link) mesmo com o cliente logado | Média | Nova tela **Perfil → Alterar senha** (senha atual + nova senha), avisando quando a senha atual está errada ou a nova é igual à atual | [antes](evidencias/bug08-alterar-senha-ia-para-esqueci.jpg) → [depois](evidencias/bug08-corrigido-tela-alterar-senha.jpg) |
| BUG-09 | O carrinho ficava salvo no navegador sem dono: quem saía pelo "Sair" do painel (ou tinha a sessão expirada) **deixava o carrinho para o próximo usuário** (a cliente viu o Pistache colocado pelo admin) | Média | Carrinho separado por usuário (chave com o id da conta); a chave antiga, compartilhada, é apagada | [antes](evidencias/bug09-carrinho-do-admin-com-a-cliente.jpg) → [depois](evidencias/bug09-corrigido-carrinho-separado.jpg) |
| BUG-10 | No admin, o status do pagamento aparecia como código interno (**"APROVADO"**, "PENDENTE") | Baixa | Status com descrição em português ("Aprovado", "Pendente") | [antes](evidencias/bug10-pagamento-APROVADO-em-maiusculas.jpg) → [depois](evidencias/bug10-corrigido-pagamento-aprovado.jpg) |
| BUG-11 | Filtro de pedidos com a data inicial **depois** da final mostrava só "Nenhum pedido encontrado", sem dizer o motivo | Baixa | Aviso: "A data inicial é posterior à data final. Ajuste o período." | [antes](evidencias/bug11-periodo-invertido-sem-aviso.jpg) → [depois](evidencias/bug11-corrigido-aviso-periodo.jpg) |
| BUG-12 | URL com código inválido (ex.: `/cupcakes/abc`) mostrava **"Algo deu errado"** (erro 400) | Baixa | Mostra a página "Ops! Não encontramos essa página" (404) | [antes](evidencias/bug12-id-invalido-algo-deu-errado.jpg) → [depois](evidencias/bug12-corrigido-pagina-404.jpg) |
| BUG-13 | Cliente já logado que abria `/login` ou `/cadastro` via de novo o formulário de entrada | Baixa | Redireciona para a vitrine (ou para o painel, se for admin) | [antes](evidencias/bug13-logado-ve-tela-de-login.jpg) → teste `bug13UsuarioLogadoNaoVeTelaDeLoginDeNovo` |
| BUG-14 | Imagem entre 5 e 10MB enviada sem passar pelo JavaScript gerava erro genérico (HTTP 413), porque o limite do servidor estourava antes da validação | Baixa | O limite técnico do servidor subiu para 10MB e a regra de 5MB ficou no validador, com a mensagem "A imagem deve ter no máximo 5MB."; a página de erro também explica o caso 413 | teste `bug14ImagemEntre5e10MBRecebeMensagemAmigavel` + reteste com 6MB no servidor real |

**Lição aprendida:** a primeira versão da correção BUG-07 fez **todas** as páginas responderem 401 para requisições sem cabeçalho de navegador. O teste antigo `visitanteEhRedirecionadoParaOLogin` pegou a regressão na hora, e a regra foi restrita a `/api/**`. Depois das correções, o roteiro de aceitação foi executado de novo: **54 de 54**.

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

## 5. Testes de aceitação (roteiro completo)

O roteiro de 20 tarefas foi executado pelo desenvolvedor em 10/11/2026 (os colegas convidados não puderam testar no prazo): **54 de 54 verificações automatizadas aprovadas**. A exploração manual das mesmas áreas encontrou os 9 problemas BUG-06 a BUG-14, corrigidos na 2ª rodada (seção 2.1). Depois das correções, o roteiro foi repetido com 54 de 54. Os detalhes de cada rodada estão em [Resultados dos testes de aceitação](05-resultados-testes-usuarios.md).
