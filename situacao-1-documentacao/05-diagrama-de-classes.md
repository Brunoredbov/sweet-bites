# 5. Diagrama de classes (revisado)

Este é o diagrama do modelo de domínio, ou seja, das classes de persistência que de fato existem no código, em `situacao-2-codificacao/src/main/java/br/com/sweetbites/model`.

```mermaid
classDiagram
    direction LR

    class Usuario {
        -Long id
        -String nome
        -String email
        -String senhaHash
        -String telefone
        -Papel papel
        -int tentativasFalhas
        -boolean bloqueado
        -LocalDateTime dataCriacao
        +registrarFalhaLogin()
        +registrarLoginComSucesso()
        +redefinirSenha(novoHash)
        +isAdmin() boolean
    }

    class Categoria {
        -Long id
        -String nome
        -String descricao
    }

    class Cupcake {
        -Long id
        -String nome
        -String descricao
        -String ingredientes
        -BigDecimal preco
        -int estoque
        -boolean ativo
        +isDisponivel() boolean
        +isEstoqueBaixo() boolean
        +baixarEstoque(qtd)
        +devolverEstoque(qtd)
    }

    class ImagemProduto {
        -Long id
        -String tipo
        -byte[] dados
    }

    class Pedido {
        -Long id
        -LocalDateTime dataCriacao
        -StatusPedido status
        -BigDecimal subtotal
        -BigDecimal frete
        -BigDecimal valorTotal
        -FormaPagamento formaPagamento
        +adicionarItem(item)
        +definirFrete(valor)
        +atualizarStatus(status)
        +getNumero() String
    }

    class ItemPedido {
        -Long id
        -int quantidade
        -BigDecimal precoUnitario
        +calcularSubtotal() BigDecimal
    }

    class Pagamento {
        -Long id
        -FormaPagamento metodo
        -StatusPagamento status
        -BigDecimal valor
        -int parcelas
        -String cartaoFinal
        -String pixCodigo
        -LocalDateTime pixExpiraEm
        -LocalDateTime dataAprovacao
        +aprovar(dataHora)
        +isPixPendente() boolean
    }

    class Entrega {
        -Long id
        -String cep
        -String logradouro
        -String numero
        -String complemento
        -String bairro
        -String cidade
        -String uf
        -int tempoEstimadoMin
        -LocalDateTime dataPrevista
        +getEnderecoFormatado() String
    }

    class Notificacao {
        -Long id
        -String mensagem
        -boolean lida
        -LocalDateTime dataCriacao
        +marcarComoLida()
    }

    class TokenRecuperacaoSenha {
        -Long id
        -String tokenHash
        -LocalDateTime expiraEm
        -boolean usado
        +isValido(agora) boolean
    }

    class Papel {
        <<enumeration>>
        CLIENTE
        ADMIN
    }
    class StatusPedido {
        <<enumeration>>
        AGUARDANDO_PAGAMENTO
        EM_PREPARACAO
        SAIU_PARA_ENTREGA
        ENTREGUE
        CANCELADO
        +proximo() StatusPedido
        +permiteCancelamento() boolean
    }
    class FormaPagamento {
        <<enumeration>>
        CARTAO
        PIX
        DINHEIRO
    }
    class StatusPagamento {
        <<enumeration>>
        PENDENTE
        APROVADO
        RECUSADO
        EXPIRADO
        ESTORNADO
        CANCELADO
    }

    Usuario "1" --> "0..*" Pedido : faz
    Pedido "1" *-- "1..*" ItemPedido : contém
    ItemPedido "0..*" --> "1" Cupcake : refere-se a
    Cupcake "0..*" --> "1" Categoria : pertence a
    Cupcake "1" *-- "0..1" ImagemProduto : foto
    Pedido "1" *-- "1" Pagamento : pago por
    Pedido "1" *-- "1" Entrega : entregue em
    Usuario "1" --> "0..*" Notificacao : recebe
    Notificacao "0..*" --> "0..1" Pedido : sobre
    Usuario "1" --> "0..*" TokenRecuperacaoSenha : solicita
    Usuario ..> Papel
    Pedido ..> StatusPedido
    Pagamento ..> StatusPagamento
    Pagamento ..> FormaPagamento
```

## O que foi corrigido em relação ao diagrama do PIT I

| # | Problema no PIT I | Correção |
|---|---|---|
| 1 | A classe **Pagamento aparecia duas vezes**, e uma delas tinha os atributos de Entrega | Uma única classe `Pagamento`, com método, status, valor, parcelas e dados do PIX |
| 2 | Atributos de **Entrega ilegíveis** ("endercc Entrega", "valorPrvoant") | `Entrega` com endereço completo (CEP, rua, número, bairro, cidade, UF) e previsão |
| 3 | `Pedido` ligado **diretamente** a `Cupcake` (1..1) | A ligação passa por `ItemPedido`: um pedido tem vários itens, e cada item aponta para um cupcake |
| 4 | `Categoria` ligada a `ItemPedido` | `Categoria` ligada a `Cupcake`, que é quem de fato tem categoria |
| 5 | Composição Usuario ◆ Pedido | Associação simples: o pedido não é "parte" do usuário e é preservado como histórico |
| 6 | Valores monetários como `Float` | `BigDecimal`, para não acumular erro de arredondamento em dinheiro |
| 7 | `Usuario.endereco` como texto solto | O endereço fica em `Entrega`, uma por pedido; o cliente pode receber em locais diferentes |
| 8 | Sem perfil de acesso | `Papel` (CLIENTE/ADMIN) separa o que cada um pode fazer |
| 9 | Status como `String` livre | Enumerações (`StatusPedido`, `StatusPagamento`, `FormaPagamento`), com as transições permitidas codificadas |
| 10 | Faltavam classes exigidas pelas histórias | `Notificacao` (US-10), `TokenRecuperacaoSenha` (US-18), `ImagemProduto` (US-14), além de `tentativasFalhas`/`bloqueado` (US-02) |
| 11 | `Usuario.senha` em texto | `senhaHash`: só o hash BCrypt é guardado |

> O diagrama original continua disponível em [`imagens/pit1-original/diagrama-de-classes.jpg`](imagens/pit1-original/diagrama-de-classes.jpg), para comparação.
