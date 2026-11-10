# 3. Product Backlog e Sprints

Backlog priorizado do PIT I, atualizado com o andamento real do PIT II.

| ID | História | Pontos | Prioridade | Sprint | Situação no PIT II |
|---|---|---|---|---|---|
| US-01 | Cadastrar conta | 3 | Alta | 1 | ✅ Entregue |
| US-02 | Fazer login | 2 | Alta | 1 | ✅ Entregue |
| US-03 | Fazer logout | 1 | Alta | 1 | ✅ Entregue |
| US-18 | Recuperar senha | 3 | Alta | 1 | 🟡 Entregue (e-mail simulado) |
| US-04 | Navegar pela vitrine | 5 | Alta | 1 | ✅ Entregue |
| US-05 | Buscar cupcake | 3 | Alta | 2 | ✅ Entregue |
| US-06 | Visualizar detalhes | 3 | Alta | 2 | ✅ Entregue |
| US-07 | Adicionar ao carrinho | 3 | Alta | 2 | ✅ Entregue |
| US-08 | Realizar pedido | 8 | Alta | 2 | 🟡 Entregue (sem endereços salvos) |
| US-09 | Escolher forma de pagamento | 5 | Alta | 2 | 🟡 Entregue (gateway simulado) |
| US-10 | Acompanhar status do pedido | 5 | Alta | 3 | 🟡 Entregue (notificação no app) |
| US-14 | Gerenciar produtos (Admin) | 8 | Alta | 3 | ✅ Entregue |
| US-15 | Gerenciar estoque (Admin) | 5 | Alta | 3 | ✅ Entregue |
| US-16 | Gerenciar pedidos (Admin) | 5 | Alta | 3 | ✅ Entregue |
| US-19 | Cancelar pedido (cliente) *(nova)* | 2 | Média | 3 | ✅ Entregue |
| US-20 | Gerenciar categorias (Admin) *(nova)* | 2 | Média | 3 | ✅ Entregue |
| US-11 | Avaliar pedido | 3 | Média | 4 | ⏳ Planejada |
| US-12 | Histórico com "Repetir pedido" | 3 | Média | 4 | ⏳ Parcial (listagem pronta) |
| US-13 | Adicionar aos favoritos | 2 | Média | 4 | ⏳ Planejada |
| US-17 | Relatórios de vendas (Admin) | 8 | Média | 4 | ⏳ Planejada |

## Resumo por sprint

| Sprint | Objetivo | Pontos | Situação |
|---|---|---|---|
| 1 | Acesso e vitrine | 14 | Concluída |
| 2 | Compra: busca, carrinho, pedido e pagamento | 22 | Concluída |
| 3 | Pós-venda e administração | 27 (23 + 4 das histórias novas) | Concluída |
| 4 | Fidelização e gestão | 16 | Próximo ciclo |

**Velocidade:** 63 pontos entregues nas Sprints 1 a 3, de 79 pontos no backlog total (80%), com média de 21 pontos por sprint.

## Definição de pronto (Definition of Done)

Uma história só é considerada pronta quando:
1. todos os critérios de aceitação estão implementados;
2. as regras de negócio estão no back-end, e não só na tela;
3. existe teste automatizado para as regras principais (`./mvnw test` passa);
4. a história foi verificada manualmente no navegador, com layout de celular;
5. a documentação foi atualizada.
