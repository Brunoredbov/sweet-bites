# 9. Protótipos e telas implementadas

## 9.1 Do protótipo à tela real

A identidade visual dos mockups do PIT I foi mantida: rosa `#FF6B8A` e branco, cards com foto, botões arredondados e navegação inferior. Algumas mudanças surgiram na implementação:

| Protótipo PIT I | Implementação PIT II | Motivo |
|---|---|---|
| Login social (Google/Facebook) | Removido | Fora das histórias de usuário; exigiria contas de desenvolvedor |
| Ícone de favorito (coração) nos cards | Removido por enquanto | US-13 é da Sprint 4 |
| "Promo code" no carrinho | Removido | Não havia história de usuário |
| Avaliações (estrelas) na vitrine | Removido por enquanto | US-11 é da Sprint 4 |
| Navegação: Home, Favoritos, Cardápio, Carrinho, Perfil | Início, Pedidos, Carrinho, Perfil (+ Admin para administradores) | Pedidos ganhou atalho, já que acompanhar a entrega é central (US-10) |
| Fotos reais de cupcakes | Ilustrações vetoriais no cardápio de exemplo | O admin envia as fotos reais pelo painel (US-14) |
| — | Sino de notificações com contador | Substitui o push (US-10) |
| — | Linha do tempo do pedido | Deixa claro o status em tempo real (US-10) |

## 9.2 Mapa navegacional (revisado)

```mermaid
flowchart LR
    Login["Login"] --> Vitrine
    Login --> Cadastro["Cadastro"] --> Login
    Login --> Recuperar["Recuperar senha"] --> Redefinir["Redefinir senha"] --> Login
    Vitrine["Início / Vitrine<br/>(busca + categorias)"] --> Detalhe["Detalhes do cupcake"]
    Detalhe --> Carrinho
    Vitrine --> Carrinho["Carrinho"]
    Carrinho --> Checkout["Checkout<br/>endereço + pagamento + resumo"]
    Checkout --> Confirmacao["Confirmação / Acompanhamento"]
    Confirmacao --> Pedidos["Meus pedidos"]
    Pedidos --> Confirmacao
    Vitrine --> Notificacoes["Notificações"] --> Confirmacao
    Vitrine --> Perfil["Perfil"] --> Pedidos
    Perfil --> Sair(["Sair"])
    Login -.->|administrador| Painel["Admin: Painel"]
    Painel --> APedidos["Admin: Pedidos"] --> APedido["Admin: Pedido<br/>(mudar status)"]
    Painel --> ACupcakes["Admin: Cupcakes"] --> AForm["Admin: Novo/Editar cupcake"]
    Painel --> AEstoque["Admin: Estoque"]
    Painel --> ACategorias["Admin: Categorias"]
```

## 9.3 Telas do cliente (celular, 375 px)

| Login | Cadastro (validação) | Vitrine | Busca "morango" |
|---|---|---|---|
| ![Login](imagens/tela-01-login.jpg) | ![Cadastro](imagens/tela-02-cadastro-validacao.jpg) | ![Vitrine](imagens/tela-03-vitrine.jpg) | ![Busca](imagens/tela-04-busca.jpg) |

| Item indisponível | Detalhes | Carrinho | Checkout: endereço |
|---|---|---|---|
| ![Indisponível](imagens/tela-04b-indisponivel.jpg) | ![Detalhe](imagens/tela-05-detalhe.jpg) | ![Carrinho](imagens/tela-06-carrinho.jpg) | ![Checkout](imagens/tela-07-checkout-endereco.jpg) |

| Checkout: pagamento | Pagamento via PIX | Confirmação | Acompanhamento |
|---|---|---|---|
| ![Pagamento](imagens/tela-07b-checkout-pagamento.jpg) | ![PIX](imagens/tela-08-pix.jpg) | ![Confirmação](imagens/tela-08b-confirmacao.jpg) | ![Acompanhamento](imagens/tela-09-acompanhamento.jpg) |

| Notificações | Meus pedidos |
|---|---|
| ![Notificações](imagens/tela-10-notificacoes.jpg) | ![Meus pedidos](imagens/tela-11-meus-pedidos.jpg) |

## 9.4 Telas do administrador (desktop)

**Painel**: contadores por status e alerta de estoque baixo
![Painel](imagens/tela-admin-01-painel.jpg)

**Pedidos**: filtro por status e período
![Pedidos](imagens/tela-admin-02-pedidos.jpg)

**Detalhe do pedido**: avançar status ou cancelar
![Pedido](imagens/tela-admin-03-pedido.jpg)

**Cupcakes**
![Cupcakes](imagens/tela-admin-04-cupcakes.jpg)

**Editar cupcake**: com upload de foto
![Editar](imagens/tela-admin-05-editar-cupcake.jpg)

**Estoque**
![Estoque](imagens/tela-admin-06-estoque.jpg)

## 9.5 Artefatos originais do PIT I

Mantidos para comparação em [`imagens/pit1-original/`](imagens/pit1-original/): [wireframes](imagens/pit1-original/wireframes.jpg), [mapa navegacional](imagens/pit1-original/mapa-navegacional.jpg) e [mockups](imagens/pit1-original/mockups.jpg).
