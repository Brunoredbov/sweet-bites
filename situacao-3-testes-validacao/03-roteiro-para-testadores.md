# 3. Roteiro para os testadores (validação com usuários)

Olá! Obrigado por testar a **Sweet Bites**, uma loja virtual de cupcakes. O teste leva uns 15 minutos. Use, de preferência, o **celular**.

**Link da aplicação:** _(será preenchido quando o sistema estiver no ar)_ · O servidor gratuito "dorme" quando ninguém usa: **o primeiro acesso pode levar até 1 minuto**.

> Pagamentos, PIX e e-mails são **simulados**: nada é cobrado de verdade. Não use dados reais de cartão.

## Dados de teste

| Para | Use |
|---|---|
| Cartão aprovado | `4111 1111 1111 1111`, validade `12/30`, CVV `123` |
| Cartão recusado | `4000 0000 0000 0002`, validade `12/30`, CVV `123` |
| CEP em São Paulo (frete R$ 7,00) | `01310-100` (Av. Paulista) |
| CEP em outra cidade de SP (frete R$ 15,00) | `13010-000` (Campinas) |
| CEP fora da área de entrega | `20040-020` (Rio de Janeiro) |

## Tarefas

Faça as tarefas na ordem e anote no [formulário](04-formulario-de-feedback.md) o que funcionou, o que não funcionou e o que você não conseguiu testar. **Tire print** de qualquer erro.

### Parte A: Cliente
1. **Crie uma conta.** Primeiro tente uma senha fraca (ex.: `abc`) e veja a mensagem. Depois use uma senha válida.
2. **Entre** com a conta criada.
3. Na vitrine, **filtre** pela categoria "Chocolate" e depois volte para "Todos".
4. **Busque** "morango" e confira se aparecem cupcakes com morango no nome ou nos ingredientes.
5. Abra um cupcake **indisponível** (ex.: "Nutella com Avelã") e confira se não dá para comprá-lo.
6. Abra um cupcake, escolha **3 unidades** e adicione ao carrinho.
7. No **carrinho**, aumente, diminua e remova itens. Feche o navegador, abra de novo e confira se o carrinho continua lá.
8. Deixe no carrinho **menos de R$ 15,00** e tente finalizar. Deve aparecer o aviso de pedido mínimo.
9. Complete o carrinho e vá em **Finalizar pedido**. Teste o CEP fora da área e depois um CEP válido.
10. Pague com o **cartão recusado**: deve aparecer "Pagamento não aprovado".
11. Pague com o **cartão aprovado**: deve aparecer "Pedido confirmado!".
12. Faça outro pedido com **PIX**, copie o código e use **Simular pagamento do PIX**.
13. Abra **Meus pedidos** e o **sino de notificações**.
14. Cancele um pedido que esteja "Em preparação".
15. Use **Esqueci minha senha**, redefina a senha pelo link exibido e entre com a nova senha.
16. Saia da conta (**Perfil → Sair**).

### Parte B: Administrador *(só se você recebeu o login de admin)*
17. Veja o **painel** e o alerta de estoque baixo.
18. Cadastre um **novo cupcake** com foto (JPG ou PNG). Tente também enviar um arquivo que não seja imagem.
19. Altere o **estoque** de um cupcake para 0 e confira na vitrine se ele aparece como "Indisponível".
20. Em **Pedidos**, abra um pedido e avance o status até "Entregue". Na conta de cliente, veja a notificação. Tente cancelar o pedido entregue.

### Parte C: Opinião
- O que achou do visual e da facilidade de uso?
- O que você mudaria ou acrescentaria?
