# 5. Resultados dos testes de aceitação

> **Observação:** os colegas convidados não puderam testar a aplicação dentro do prazo. Por isso, o desenvolvedor executou o [roteiro de 20 tarefas](03-roteiro-para-testadores.md) inteiro em **10/11/2026**, na aplicação local com PostgreSQL, dividido em 5 rodadas por área do sistema.
>
> **Como foi executado:**
> - as verificações de servidor foram automatizadas com o script [`scripts/roteiro_aceitacao.py`](scripts/roteiro_aceitacao.py), que simula o navegador com sessão, CSRF e envio de formulários;
> - as partes que dependem de JavaScript foram conferidas manualmente no navegador, com tela de celular: busca enquanto digita, carrinho salvo, aviso de pedido mínimo e atualização do status em tempo real.
>
> **Resultado:** o roteiro automatizado teve **54 de 54 verificações aprovadas** (saída completa em [`evidencias/resultados-aceitacao-2026-11-10.json`](evidencias/resultados-aceitacao-2026-11-10.json)). A exploração manual das mesmas áreas encontrou **9 problemas (BUG-06 a BUG-14)**, listados em cada rodada abaixo e corrigidos depois (ver a seção 2.1 do [laudo](06-laudo-de-qualidade.md)). Com as correções, o roteiro foi repetido: 54 de 54.
>
> Se algum colega testar depois, o formulário continua disponível e o retorno dele entra na seção "Testes com colegas", no fim deste documento.

## Teste 1: Conta e acesso (tarefas 1, 2, 15 e 16)
| Campo | Resultado |
|---|---|
| Executado por | Bruno Villanova da Silva (desenvolvedor) |
| Data | 10/11/2026 |
| Testou e funcionou | **Cadastro:** senha fraca recusada com mensagem; e-mail repetido (em maiúsculas) recusado; cadastro válido leva ao login com "Conta criada com sucesso". **Login:** saudação pelo nome. **Bloqueio:** após 5 senhas erradas, nem a senha certa entra. **Esqueci minha senha:** link exibido (modo demonstração), conta desbloqueada e link de uso único. **Logout:** volta ao login e a vitrine passa a exigir login. |
| Não funcionou / corrigir | **BUG-08:** "Alterar senha" no perfil abre o fluxo de "Esqueci minha senha" mesmo com o cliente logado. Corrigir: tela própria com senha atual e nova. **BUG-13:** quem já está logado e abre `/login` ou `/cadastro` vê o formulário de novo. Corrigir: redirecionar para a vitrine. |
| Não testado | Recebimento real de e-mail (simulado); login social (fora do escopo) |

## Teste 2: Vitrine e carrinho (tarefas 3 a 8)
| Campo | Resultado |
|---|---|
| Executado por | Bruno Villanova da Silva (desenvolvedor) |
| Data | 10/11/2026 |
| Testou e funcionou | **Vitrine:** 10 cupcakes e 5 categorias; filtro "Chocolate" mostra só os 2 de chocolate. **Busca:** acontece enquanto o cliente digita ("lim" → Limão Siciliano); "MORANGO" encontra 2 cupcakes por nome e ingrediente; busca sem resultado mostra "Produto não encontrado". **Indisponível:** Nutella com Avelã aparece sem botão de compra. **Detalhes:** com ingredientes e "Impostos inclusos". **Carrinho:** limite de 20 por item; continua salvo após recarregar; abaixo de R$ 15,00 mostra "Faltam R$ 0,50" e bloqueia a finalização; com R$ 29,00 libera. |
| Não funcionou / corrigir | **BUG-09:** o carrinho fica no navegador sem dono; a cliente que entrou depois do admin no mesmo navegador encontrou o item dele no carrinho. Corrigir: carrinho separado por usuário. **BUG-12:** `/cupcakes/abc` mostra "Algo deu errado". Corrigir: página "Não encontramos" (404). |
| Não testado | Favoritos e avaliações (Sprint 4) |

## Teste 3: Checkout e pagamento (tarefas 8 a 12)
| Campo | Resultado |
|---|---|
| Executado por | Bruno Villanova da Silva (desenvolvedor) |
| Data | 10/11/2026 |
| Testou e funcionou | **CEP:** do Rio de Janeiro e inexistente recusados. **Frete:** São Paulo R$ 7,00 / 45 min; Campinas R$ 15,00 / 120 min. **Validações:** pedido abaixo de R$ 15,00 e quantidade acima do estoque recusados. **Cartão recusado** (0002): estoque não muda (30 → 30). **Cartão aprovado** (4111, em 2x): "Pedido confirmado!" e estoque baixou de 30 para 28. **PIX:** QR Code e código gerados; "Simular pagamento" leva o pedido para Em preparação. |
| Não funcionou / corrigir | **BUG-06:** com a sessão encerrada, "Confirmar pedido" mostra "Acesso negado – você não tem permissão". Corrigir: levar ao login com o aviso de sessão encerrada. |
| Não testado | Gateway real (simulado); expiração do PIX após 30 min (coberta pelo teste automatizado `pixVencidoCancelaPedidoEDevolveEstoque`); endereços salvos (Sprint 4) |

## Teste 4: Acompanhamento e cancelamento (tarefas 13 e 14)
| Campo | Resultado |
|---|---|
| Executado por | Bruno Villanova da Silva (desenvolvedor) |
| Data | 10/11/2026 |
| Testou e funcionou | **Meus pedidos:** lista com número, data, pagamento, valor e status. **Notificações:** sino com 3 não lidas e uma notificação por etapa. **Tempo real:** a tela do pedido recarregou sozinha em menos de 10 s após a mudança feita pelo admin. **Cancelamento:** devolve o estoque (Pistache 11 → 12), e um pedido já cancelado não pode ser cancelado de novo. |
| Não funcionou / corrigir | **BUG-07:** se a sessão expira com a tela do pedido aberta, a atualização do status para sem aviso. Corrigir: avisar e levar ao login. |
| Não testado | Push no celular (substituído por notificação no app); "Repetir pedido" (Sprint 4) |

## Teste 5: Administrador e segurança (tarefas 17 a 20)
| Campo | Resultado |
|---|---|
| Executado por | Bruno Villanova da Silva (desenvolvedor) |
| Data | 10/11/2026 |
| Testou e funcionou | **Painel:** contadores e alerta de estoque baixo. **Cupcakes:** arquivo falso recusado; novo cupcake com PNG aparece na vitrine; estoque 0 deixa o cupcake Indisponível; cupcake em pedido ativo não pode ser removido. **Pedidos:** não é possível pular etapa; o cliente vê "Saiu para entrega" e "Entregue", com notificação; pedido entregue não pode ser cancelado. **Segurança:** cliente recebe 403 no painel admin e 404 no pedido de outro cliente. |
| Não funcionou / corrigir | **BUG-10:** status do pagamento aparece como "APROVADO". Corrigir: descrição em português. **BUG-11:** filtro com período invertido mostra lista vazia sem explicação. Corrigir: avisar. **BUG-14:** imagem de 5 a 10MB enviada sem o JavaScript dá erro genérico 413. Corrigir: mensagem do limite de 5MB. |
| Não testado | Relatórios em PDF (Sprint 4); upload acima de 5MB pela tela (coberto pelo teste automatizado `rejeitaImagemMaiorQue5MB`) |

## Como repetir o teste

Com a aplicação rodando (ver o [README do código](../situacao-2-codificacao/README.md)):

```bash
python3 situacao-3-testes-validacao/scripts/roteiro_aceitacao.py http://localhost:8080 <email-admin> <senha-admin> resultado.json
```

## Testes com colegas

| Nome | Data | Funcionou | Não funcionou | Não testado | Decisão |
|---|---|---|---|---|---|
| | | | | | |
