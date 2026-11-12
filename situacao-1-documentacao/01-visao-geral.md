# 1. Visão geral do projeto

## Problema

A **Sweet Bites** é uma loja de cupcakes gourmet que vendia apenas no balcão e por mensagens. Os pedidos ficavam espalhados em conversas, sem controle de estoque. Também não havia como o cliente ver o cardápio atualizado ou acompanhar a entrega (briefing do PIT I).

## Objetivo

Oferecer uma **loja virtual mobile-first** em que o cliente:
- consulta o cardápio com fotos e preços;
- monta o carrinho e faz o pedido com entrega;
- paga com cartão, PIX ou dinheiro;
- acompanha o status do pedido em tempo real.

O administrador mantém o cardápio, o estoque e os pedidos num painel próprio.

## Partes interessadas (stakeholders)

| Ator | Papel no sistema |
|---|---|
| Cliente | Navega pela vitrine, compra e acompanha pedidos |
| Administrador | Gerencia cupcakes, categorias, estoque e pedidos |
| Sistema de Pagamento (externo) | Processa cartão e PIX. **No PIT II é simulado** (ver [Arquitetura](08-arquitetura.md)) |
| Serviço de CEP (externo) | ViaCEP: valida o endereço de entrega |

## Escopo

| Entregue no PIT II (Sprints 1 a 3) | Fica para a evolução (Sprint 4) |
|---|---|
| Cadastro, login, logout, recuperação e troca de senha | US-11 Avaliar pedido |
| Vitrine, busca, detalhes, carrinho | US-12 Histórico com "Repetir pedido" |
| Pedido com frete por CEP, pagamento, acompanhamento | US-13 Favoritos |
| Admin: produtos, estoque, pedidos, categorias | US-17 Relatórios de vendas em PDF |

> A tela "Meus pedidos" já lista os pedidos do cliente com status e valor, mas sem a opção "Repetir pedido". Essa opção completa a US-12 na Sprint 4.

## Decisão de plataforma: de "app nativo" para "web app mobile-first"

O PIT I descrevia um **aplicativo mobile**. No PIT II a solução virou uma **aplicação web responsiva com layout de celular**. Os motivos foram:

1. **Exigências da disciplina:** o material pede front-end em HTML, CSS e JavaScript, back-end em Java/PHP/C#/Python no padrão MVC e um **link da solução em funcionamento** para os colegas testarem.
2. **Uso imediato pelos testadores:** basta um link no navegador do celular, sem instalar APK nem passar por loja de aplicativos.
3. **Fidelidade ao design:** os mockups do PIT I (rosa e branco, navegação inferior, cards) foram mantidos, então no celular a experiência é a de um app.
