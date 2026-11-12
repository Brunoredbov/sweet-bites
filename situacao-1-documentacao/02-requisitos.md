# 2. Requisitos: histórias de usuário (revisadas)

As histórias seguem o formato *"Como [papel], eu quero [ação], para que [benefício]"*, com critérios de aceitação (CA), regras de negócio (RN) e requisitos não funcionais (RNF).

Legenda de status:
- ✅ implementada;
- 🟡 implementada com ajuste (explicado em **Revisão PIT II**);
- ⏳ planejada para a Sprint 4.

**O que mudou em relação ao PIT I:** regras ambíguas foram esclarecidas, pontos que dependiam de serviços pagos foram ajustados (pagamento, e-mail, push) e entraram duas histórias novas (US-19 e US-20), identificadas durante o desenvolvimento.

---

## Autenticação

### US-01: Cadastrar conta ✅ · Alta · 3 pts
**Ação:** Como um cliente não cadastrado, eu quero criar uma conta com nome, e-mail, senha e telefone, para que eu possa acessar o aplicativo e realizar pedidos.
- CA#1: O sistema valida o e-mail como único (sem diferenciar maiúsculas/minúsculas).
- CA#2: A senha deve ter no mínimo 8 caracteres, com letras e números.
- CA#3: Um e-mail de boas-vindas é enviado após o cadastro.
- CA#4 *(novo)*: O telefone precisa ter DDD, ex.: (11) 91234-5678.
- RN#1: E-mail único no sistema. RN#2: Senha mínima de 8 caracteres.
- RNF#1: Cadastro processado em menos de 3 segundos.

**Revisão PIT II:** o e-mail é **simulado** (registrado no log). As mensagens de erro aparecem embaixo de cada campo, uma por campo.

### US-02: Fazer login ✅ · Alta · 2 pts
**Ação:** Como um cliente cadastrado, eu quero fazer login com meu e-mail e senha, para que eu possa acessar minha conta e meus pedidos.
- CA#1: Autentica em até 3 segundos. CA#2: Opção "Esqueci minha senha" disponível.
- RN#1: Bloquear a conta após 5 tentativas erradas.
- RN#2 *(novo)*: A conta bloqueada é desbloqueada ao redefinir a senha (US-18).
- RNF#1: Tempo de resposta máximo de 2 segundos.

**Revisão PIT II:** o administrador usa a mesma tela de login e é levado direto ao painel. Quem já está logado e abre a tela de login é levado à vitrine. Com a sessão expirada, o sistema volta ao login com o aviso "Sua sessão foi encerrada" (BUG-06).

### US-21: Alterar senha (logado) ✅ · Média · 2 pts · *nova no PIT II*
**Ação:** Como um cliente logado, eu quero trocar minha senha informando a senha atual, para não precisar do fluxo de "Esqueci minha senha".
- RN#1: A senha atual precisa estar correta. RN#2: A nova senha segue a regra de 8 caracteres ou mais, com letras e números, e deve ser diferente da atual.

**Origem:** revisão de qualidade pós-aceitação (BUG-08 do laudo).

### US-03: Fazer logout ✅ · Alta · 1 pt
**Ação:** Como um cliente autenticado, eu quero fazer logout, para que minha sessão seja encerrada com segurança.
- CA#1: Após o logout, o usuário volta à tela de login.
- RN#1: A sessão é invalidada no servidor.

### US-18: Recuperar senha 🟡 · Alta · 3 pts
**Ação:** Como um cliente que esqueceu a senha, eu quero solicitar a recuperação por e-mail, para que eu recupere o acesso à minha conta.
- CA#1: E-mail com link de redefinição. CA#2: O link expira em 1 hora e só pode ser usado uma vez.
- RN#1: Após a redefinição, todas as sessões ativas do usuário são encerradas.
- RN#2 *(novo)*: A resposta é a mesma exista ou não o e-mail (não revela quem é cliente).
- RNF#1: E-mail enviado em menos de 1 minuto.

**Revisão PIT II:** o e-mail é simulado. No **modo demonstração**, o link aparece na própria tela para os testadores conseguirem concluir o fluxo.

---

## Vitrine

### US-04: Navegar pela vitrine ✅ · Alta · 5 pts
**Ação:** Como um cliente autenticado, eu quero visualizar o cardápio com fotos, nomes, preços e categorias, para que eu escolha os produtos.
- CA#1: Cupcakes em grade (2 colunas no celular) com foto, nome e preço. CA#2: Filtro por categoria.
- RN#1: Cupcakes sem estoque aparecem como "Indisponível", com o botão desabilitado.
- RNF#1: Imagens carregam em menos de 2 segundos (carregamento tardio e cache de 7 dias).

### US-05: Buscar cupcake ✅ · Alta · 3 pts
**Ação:** Como um cliente, eu quero buscar cupcakes pelo nome ou ingrediente, para que eu encontre rapidamente o produto.
- CA#1: A busca retorna resultados enquanto o cliente digita. CA#2: Mostra "Produto não encontrado" quando não há resultados.
- RN#1: A busca não diferencia maiúsculas de minúsculas.
- RNF#1: Resposta em até 1 segundo.

### US-06: Visualizar detalhes do produto ✅ · Alta · 3 pts
**Ação:** Como um cliente, eu quero ver foto, descrição, ingredientes e preço de um cupcake, para decidir se compro.
- CA#1: Foto ampliada, descrição, ingredientes e preço. CA#2: Botão "Adicionar ao carrinho" com seletor de quantidade.
- RN#1: O preço exibido já inclui impostos ("Impostos inclusos").

**Revisão PIT II:** aviso "Últimas N unidades" quando o estoque está baixo.

### US-07: Adicionar ao carrinho ✅ · Alta · 3 pts
**Ação:** Como um cliente, eu quero adicionar cupcakes ao carrinho com a quantidade desejada, para comprar vários itens num único pedido.
- CA#1: Quantidade por item. CA#2: Subtotal atualizado na hora. CA#3: Remover itens.
- RN#1: Máximo de 20 unidades por item (e nunca mais do que o estoque).
- RN#2: O carrinho continua salvo mesmo se o app for fechado.
- RN#3 *(novo)*: Os preços do carrinho são sempre recalculados pelo servidor.
- RN#4 *(novo)*: O carrinho é de cada usuário: outra pessoa que entrar no mesmo aparelho não vê os itens do anterior (BUG-09).

---

## Pedido e pagamento

### US-08: Realizar pedido 🟡 · Alta · 8 pts
**Ação:** Como um cliente com itens no carrinho, eu quero confirmar o pedido com o endereço de entrega, para que os cupcakes sejam preparados e entregues.
- CA#1: O cliente informa o CEP, e rua, bairro e cidade são preenchidos automaticamente.
- CA#2: O sistema calcula o frete. CA#3: Um resumo do pedido aparece antes da confirmação.
- RN#1: Endereço validado pela API de CEP (ViaCEP).
- RN#2: Pedido mínimo de **R$ 15,00 em produtos** (o frete não conta).
- RN#3 *(novo)*: Frete por zona: mesma cidade da loja R$ 7,00 / 45 min; outras cidades do estado R$ 15,00 / 120 min; fora do estado não há entrega.
- RNF#1: Cálculo do frete em até 3 segundos (tempo limite de 2 s na consulta de CEP).

**Revisão PIT II:** a opção "selecionar endereço cadastrado" (CA#1 do PIT I) fica para a Sprint 4. Hoje o cliente informa o endereço a cada pedido.

### US-09: Escolher forma de pagamento 🟡 · Alta · 5 pts
**Ação:** Como um cliente finalizando o pedido, eu quero escolher entre cartão, PIX ou dinheiro, para que meu pedido seja processado.
- CA#1: Opções de cartão, PIX e dinheiro na entrega. CA#2: QR Code gerado para o PIX. CA#3: Cartão processado por gateway.
- RN#1: Cartão em até 3x sem juros. RN#2: O PIX expira em 30 minutos; depois disso o pedido é cancelado e o estoque volta.
- RN#3 *(novo)*: Só os 4 últimos dígitos do cartão são guardados.
- RNF#1: Comunicação por HTTPS/TLS.

**Revisão PIT II:** o **gateway é simulado**: cartão válido é aprovado, cartão terminado em 0002 é recusado e o PIX tem o botão "Simular pagamento". Ligar um gateway real (Mercado Pago, Stripe) exige só trocar a implementação de `PagamentoService`.

### US-10: Acompanhar status do pedido 🟡 · Alta · 5 pts
**Ação:** Como um cliente que fez um pedido, eu quero acompanhar o status em tempo real (em preparação, saiu para entrega, entregue), para saber quando chega.
- CA#1: Status exibido em tempo real, numa linha do tempo.
- CA#2: Uma notificação é gerada a cada mudança de status.
- RN#1: O tempo estimado de entrega depende da zona de entrega (ver US-08 RN#3).
- RNF#1: A mudança aparece para o cliente em até 10 segundos.

**Revisão PIT II:** a notificação **push** virou **notificação dentro do app** (sino com contador), e a tela do pedido consulta o status a cada 10 s. O RNF original pedia 5 s; o prazo foi ajustado para reduzir a carga no servidor gratuito.

### US-19: Cancelar pedido (cliente) ✅ · Média · 2 pts · *nova no PIT II*
**Ação:** Como um cliente, eu quero cancelar um pedido que ainda não saiu para entrega, para desistir da compra sem ligar para a loja.
- CA#1: O botão "Cancelar pedido" aparece só em "Aguardando pagamento" e "Em preparação".
- RN#1: O cancelamento devolve o estoque.

**Origem:** o método `cancelarPedido()` do diagrama de classes do PIT I não tinha história de usuário correspondente.

---

## Administração

### US-14: Gerenciar produtos ✅ · Alta · 8 pts
**Ação:** Como administrador, eu quero adicionar, editar e remover cupcakes com foto, descrição, ingredientes, preço e categoria, para manter o cardápio atualizado.
- CA#1: Upload da foto. CA#2: Campos obrigatórios validados antes de salvar.
- RN#1: Cupcakes em pedidos em andamento não podem ser removidos.
- RN#2 *(novo)*: A remoção é lógica (o cupcake sai do cardápio, mas continua nos pedidos antigos).
- RNF#1: Upload aceita JPG e PNG de até 5MB (formato conferido pelo conteúdo do arquivo).

### US-15: Gerenciar estoque ✅ · Alta · 5 pts
**Ação:** Como administrador, eu quero atualizar a quantidade disponível de cada cupcake, para que os clientes vejam a disponibilidade real.
- CA#1: Alerta no painel quando o estoque está abaixo de 5 unidades. CA#2: Estoque zero aparece como "Indisponível".
- RN#1: O estoque baixa automaticamente a cada pedido confirmado e volta se o pedido for cancelado.

### US-16: Gerenciar pedidos ✅ · Alta · 5 pts
**Ação:** Como administrador, eu quero ver todos os pedidos e atualizar seus status, para gerenciar preparo e entrega.
- CA#1: Filtro por status e por período. CA#2: Mudar o status notifica o cliente automaticamente.
- RN#1: Pedidos entregues não podem ser cancelados.
- RN#2 *(novo)*: O status só avança na ordem *Em preparação → Saiu para entrega → Entregue*.
- RN#3 *(novo)*: Pagamento em dinheiro é marcado como pago quando o pedido é entregue.

### US-20: Gerenciar categorias ✅ · Média · 2 pts · *nova no PIT II*
**Ação:** Como administrador, eu quero criar e remover categorias, para organizar o cardápio.
- RN#1: O nome da categoria é único. RN#2: Só é possível remover uma categoria sem cupcakes.

**Origem:** a classe `Categoria` do PIT I não tinha história para mantê-la.

---

## Planejadas para a Sprint 4 ⏳

| ID | História | Pontos |
|---|---|---|
| US-11 | Avaliar pedido (nota de 1 a 5 e comentário, só após a entrega) | 3 |
| US-12 | Histórico de pedidos com "Repetir pedido" (a listagem já existe) | 3 |
| US-13 | Favoritos (ícone de coração, lista na conta) | 2 |
| US-17 | Relatório de vendas por período, com exportação em PDF | 8 |

---

## Requisitos não funcionais gerais

| ID | Requisito |
|---|---|
| RNF-G1 | Interface mobile-first, utilizável a partir de 360 px de largura |
| RNF-G2 | Senhas guardadas com hash BCrypt; proteção CSRF em todos os formulários |
| RNF-G3 | Acesso de um cliente ao pedido de outro responde "não encontrado" (404) |
| RNF-G4 | Banco PostgreSQL com o esquema versionado (Flyway) |
| RNF-G5 | Regras de negócio cobertas por testes automatizados (JUnit) |
| RNF-G6 | Erros mostram mensagens amigáveis, sem detalhes técnicos para o usuário |
