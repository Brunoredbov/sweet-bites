# 6. Diagramas comportamentais (revisados)

## 6.1 Diagrama de sequência: Realizar pedido (UC-08)

```mermaid
sequenceDiagram
    autonumber
    actor C as Cliente
    participant N as Navegador<br/>(HTML/JS)
    participant PC as PedidoController
    participant PS as PedidoService
    participant FS as FreteService
    participant CEP as ViaCEP
    participant PG as PagamentoService<br/>(gateway simulado)
    participant BD as PostgreSQL

    C->>N: Toca em "Finalizar pedido"
    N->>N: Lê o carrinho do localStorage
    C->>N: Informa o CEP
    N->>FS: GET /api/frete?cep= (via ApiController)
    FS->>CEP: Consulta o CEP
    CEP-->>FS: Rua, bairro, cidade, UF
    FS-->>N: Endereço + frete + tempo estimado
    C->>N: Escolhe pagamento e confirma
    N->>PC: POST /pedidos (itens, endereço, pagamento)
    PC->>PS: criarPedido(usuario, formulário)
    PS->>FS: Recalcula o frete (não confia no navegador)
    Note over PS,BD: Início da transação
    PS->>BD: SELECT cupcakes ... FOR UPDATE (trava o estoque)
    BD-->>PS: Preço e estoque atuais
    PS->>PS: Valida estoque, máx. 20/item, mínimo R$ 15
    PS->>PG: processarCartao(dados)
    alt Pagamento aprovado
        PG-->>PS: aprovado, final 1111
        PS->>BD: INSERT pedido, itens, pagamento, entrega
        PS->>BD: UPDATE estoque (baixa)
        PS->>BD: INSERT notificação
        Note over PS,BD: Commit
        PS-->>PC: Pedido
        PC-->>N: Redireciona para /pedidos/{id}?confirmado
        N->>N: Esvazia o carrinho
        N-->>C: Tela "Pedido confirmado!"
    else Pagamento recusado (curso Beta)
        PG-->>PS: recusado
        Note over PS,BD: Rollback: nada é gravado
        PS-->>PC: "Pagamento não aprovado..."
        PC-->>N: Checkout com a mensagem e o endereço preenchido
    end
```

**Correções em relação ao PIT I:**
- No diagrama original, o **app enviava os dados do cartão direto ao Sistema de Pagamento**, sem passar pelo servidor. Isso permite alterar valores pelo cliente e expõe credenciais do gateway no aplicativo. Agora **só o servidor** fala com o gateway, e o valor cobrado é calculado no servidor.
- Incluídos a **consulta de CEP**, o **bloqueio de estoque** (evita vender a última unidade para dois clientes) e o fluxo de **recusa com rollback**.
- O título duplicado ("'Realizar Pedido' app - Realizar Pedido") foi corrigido.

## 6.2 Diagrama de estados: ciclo de vida do pedido *(novo)*

```mermaid
stateDiagram-v2
    [*] --> AguardandoPagamento: Pedido via PIX
    [*] --> EmPreparacao: Cartão aprovado ou dinheiro

    AguardandoPagamento --> EmPreparacao: PIX pago
    AguardandoPagamento --> Cancelado: PIX expirou (30 min) / cliente cancelou

    EmPreparacao --> SaiuParaEntrega: Admin avança
    EmPreparacao --> Cancelado: Cliente ou admin cancela

    SaiuParaEntrega --> Entregue: Admin confirma a entrega
    SaiuParaEntrega --> Cancelado: Admin cancela

    Entregue --> [*]
    Cancelado --> [*]

    note right of Cancelado
        Todo cancelamento devolve o estoque
        e estorna o pagamento aprovado
    end note
    note right of Entregue
        Não pode ser cancelado (RN US-16).
        Pagamento em dinheiro é marcado como pago.
    end note
```

As transições permitidas ficam no próprio enum `StatusPedido` (`proximo()` e `permiteCancelamento()`) e são verificadas no servidor. Por isso, mesmo uma chamada feita fora da tela não consegue, por exemplo, cancelar um pedido entregue.

## 6.3 Diagrama de sequência: Recuperar senha (UC-18) *(novo)*

```mermaid
sequenceDiagram
    autonumber
    actor C as Cliente
    participant AC as AuthController
    participant RS as RecuperacaoSenhaService
    participant BD as PostgreSQL
    participant EM as EmailService<br/>(simulado)

    C->>AC: POST /recuperar-senha (e-mail)
    AC->>RS: solicitar(email)
    RS->>BD: Busca o usuário
    alt E-mail cadastrado
        RS->>RS: Gera token aleatório (32 bytes)
        RS->>BD: Grava o hash SHA-256 do token (válido por 1 h)
        RS->>EM: Envia o link com o token
    end
    AC-->>C: "Se houver uma conta com esse e-mail, enviamos um link" (mesma resposta sempre)
    C->>AC: POST /redefinir-senha (token, nova senha)
    AC->>RS: redefinir(token, senha)
    RS->>BD: Valida o token (existe, não usado, não expirado)
    RS->>BD: Grava a nova senha (BCrypt), desbloqueia a conta e marca o token como usado
    RS->>RS: Encerra todas as sessões do usuário
    AC-->>C: Login com "Senha redefinida!"
```
