# 10. Registro de revisões (melhoria contínua)

Mudanças feitas na documentação do PIT I durante o PIT II e o motivo de cada uma.

| # | Artefato | Mudança | Motivo |
|---|---|---|---|
| R01 | Plataforma | App nativo → web app mobile-first | O material pede HTML/CSS/JS e um link público para os testes |
| R02 | Histórias | Esclarecido que o pedido mínimo é sobre o valor dos produtos | A regra "pedido mínimo de R$ 15,00" era ambígua quanto ao frete |
| R03 | Histórias | Criada a regra de frete por zona (cidade/estado), com valores configuráveis | "Calcular o frete" não tinha regra definida |
| R04 | Histórias | Pagamento, e-mail e push marcados como simulados | Serviços pagos fora do alcance do projeto acadêmico |
| R05 | Histórias | Novas US-19 (cancelar pedido) e US-20 (gerenciar categorias) | Métodos e classes do diagrama sem história correspondente |
| R06 | Histórias | Novas regras: desbloqueio por redefinição de senha, resposta igual para e-mail inexistente, transições de status do pedido | Lacunas encontradas durante a implementação |
| R07 | Casos de uso | "Fazer login" deixou de estar duplicado; entraram os casos que faltavam; rótulos traduzidos | Erros de modelagem no diagrama original |
| R08 | UC-08 | Novo curso alternativo Gama (estoque/valor insuficiente) e detalhamento do PIX | Situações reais que o fluxo original não cobria |
| R09 | Classes | Correções 1 a 11 (ver [diagrama de classes](05-diagrama-de-classes.md)) | Classe duplicada, atributos ilegíveis, relacionamentos e tipos incorretos |
| R10 | Sequência | Pagamento passa pelo servidor, não mais direto do app ao gateway | Falha de segurança no desenho original |
| R11 | Novos artefatos | Diagrama de estados do pedido, sequência de recuperação de senha, DER, dicionário de dados, arquitetura | Pedidos do material teórico (projeto físico de BD, MVC) |
| R12 | Protótipos | Removidos elementos sem história (login social, cupom) | Manter o protótipo alinhado ao backlog |
| R13 | Histórias | Nova US-21 (alterar senha logado); novas regras: carrinho por usuário e aviso de sessão expirada | Revisão de qualidade pós-aceitação (BUG-06 a BUG-14 do laudo) |

## Feedback dos colegas sobre a documentação

O material pede que os colegas critiquem a solução. Registre aqui as sugestões recebidas e o que foi feito com cada uma:

| Colega | Sugestão | Aceita? | O que foi feito |
|---|---|---|---|
| | | | |
| | | | |
| | | | |
