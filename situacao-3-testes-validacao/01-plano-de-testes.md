# 1. Plano de testes

## Objetivo

Mostrar que o sistema faz o que as histórias de usuário pedem (**verificação**) e que atende quem vai usá-lo (**validação**).

## Estratégia

| Nível | O que testa | Como | Quem |
|---|---|---|---|
| Unitário | Regras de negócio isoladas (estoque, pagamento, frete, bloqueio de conta, transições de status) | JUnit 5 + Mockito, sem banco | Automatizado |
| Integração | Aplicação inteira: telas MVC, Spring Security, JPA e Flyway, com banco H2 | Spring MockMvc | Automatizado |
| Sistema (manual) | Fluxos completos no navegador, com layout de celular | Roteiro executado pelo desenvolvedor | Desenvolvedor |
| Aceitação | Uso real por pessoas que não conhecem o código | [Roteiro](03-roteiro-para-testadores.md) + [formulário](04-formulario-de-feedback.md) | 5 colegas |

## Ambiente

| Item | Valor |
|---|---|
| Testes automatizados | `./mvnw test`, com banco H2 em memória (modo PostgreSQL) e as mesmas migrations de produção |
| Teste manual | Aplicação local + PostgreSQL 16 (Docker), navegador com tela de 375×812 (celular) e 1280×800 (desktop) |
| Testes dos colegas | Link público da aplicação (Render), celular ou computador do testador |

## Critérios de aceite da entrega

1. 100% dos testes automatizados passando.
2. Todos os critérios de aceitação das histórias das Sprints 1 a 3 verificados.
3. Nenhum defeito de severidade **alta** em aberto.
4. Feedback de 5 testadores registrado, com cada item classificado como *corrigido*, *planejado* ou *não aplicável*.

## Severidade dos defeitos

| Severidade | Definição |
|---|---|
| Alta | Impede um fluxo principal (comprar, pagar, entrar) ou gera dado errado (valor, estoque) |
| Média | Funciona, mas com resultado incorreto numa situação específica, ou erro técnico exibido ao usuário |
| Baixa | Problema visual ou de texto que não impede o uso |
