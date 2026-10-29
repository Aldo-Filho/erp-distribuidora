# Fluxograma geral do ERP Distribuidora

O fluxograma apresenta a operação prevista do sistema completo, desde o acesso até os processos de compras, vendas, estoque, financeiro e emissão fiscal. Financeiro e fiscal estão previstos no schema; suas ligações representam a visão funcional de integração, sem afirmar que já estejam implementadas ou definir o momento exato de geração dos títulos.

```mermaid
flowchart TD
    INICIO([Início]) --> LOGIN[Informar e-mail e senha]
    LOGIN --> ACESSO{Acesso autorizado?}
    ACESSO -->|Não| ERRO[Informar impedimento de acesso]
    ERRO --> LOGIN
    ACESSO -->|Sim| MENU[Exibir módulos conforme as permissões]
    MENU --> OPERACAO{Selecionar operação}

    OPERACAO -->|Administração| ADMIN[Gerenciar usuários, cargos,<br/>permissões e segurança]
    OPERACAO -->|Cadastros| CAD[Manter clientes, fornecedores,<br/>produtos, marcas, categorias,<br/>depósitos e tabelas de preços]
    ADMIN --> CONCLUSAO
    CAD --> CONCLUSAO

    OPERACAO -->|Compras| COMPRA[Criar pedido de compra<br/>com fornecedor, produtos e valores]
    COMPRA --> CCONF[Enviar e confirmar compra]
    CCONF --> RECEBER[Conferir e registrar recebimento]
    RECEBER --> ENTRADA[Registrar entrada no estoque<br/>e histórico da movimentação]
    ENTRADA --> COMPLETA{Recebimento completo?}
    COMPLETA -->|Não| PARCIAL[Manter compra parcialmente recebida]
    PARCIAL --> RECEBER
    COMPLETA -->|Sim| CRECEBIDA[Concluir recebimento da compra]
    CRECEBIDA --> CONCLUSAO
    CCONF -.->|Integração financeira prevista| PAGAR[Registrar conta a pagar]

    OPERACAO -->|Vendas| VENDA[Criar pedido com cliente,<br/>produtos, preços e descontos]
    VENDA --> SALDO{Estoque disponível?}
    SALDO -->|Não| REVER[Revisar pedido ou aguardar reposição]
    REVER --> VENDA
    SALDO -->|Sim| RESERVA[Confirmar pedido e reservar estoque]
    RESERVA --> DESTINO{Prosseguir ou cancelar?}
    DESTINO -->|Cancelar| CANCELAR[Cancelar pedido<br/>e liberar reservas]
    CANCELAR --> CONCLUSAO
    DESTINO -->|Prosseguir| EXPEDIR[Expedir pedido, baixar estoque<br/>e registrar movimentação]
    EXPEDIR --> CONCLUSAO
    RESERVA -.->|Integração financeira prevista| RECEBERVALOR[Registrar conta a receber]
    RESERVA -.->|Integração fiscal prevista| FISCAL[Preparar e transmitir NF-e]
    FISCAL --> RETORNO{NF-e autorizada?}
    RETORNO -->|Sim| DOCUMENTO[Disponibilizar documento fiscal]
    RETORNO -->|Não| TRATAR[Analisar retorno e tratar pendência fiscal]
    TRATAR --> CONCLUSAO
    DOCUMENTO --> CONCLUSAO

    OPERACAO -->|Estoque| ESTOQUE[Consultar saldos, reservas<br/>e histórico por produto e depósito]
    ESTOQUE --> AJUSTE[Quando necessário, registrar<br/>entrada, saída ou ajuste manual]
    AJUSTE --> CONCLUSAO

    OPERACAO -->|Financeiro| FIN[Consultar títulos, vencimentos<br/>e contas bancárias ou caixa]
    PAGAR --> FIN
    RECEBERVALOR --> FIN
    FIN --> BAIXA[Registrar pagamentos e recebimentos<br/>parciais ou totais]
    BAIXA --> SITUACAO[Atualizar situação dos títulos<br/>e posição financeira]
    SITUACAO --> CONCLUSAO

    CONCLUSAO[Concluir operação e disponibilizar consulta] --> CONTINUAR{Realizar outra operação?}
    CONTINUAR -->|Sim| MENU
    CONTINUAR -->|Não| SAIR[Encerrar sessão]
    SAIR --> FIM([Fim])
```

## Leitura do fluxo

- Retângulos representam atividades; losangos representam decisões; formas arredondadas indicam início e fim.
- Linhas pontilhadas representam integrações previstas para o sistema completo. O momento de criação dos títulos e as condições fiscais para expedição dependem das regras finais desses módulos.
- A confirmação de uma venda reserva o estoque. A expedição realiza a baixa física; o cancelamento anterior à expedição libera a reserva.
- O recebimento de compras pode ocorrer em partes. Cada recebimento registra a entrada correspondente no estoque.
- A expedição e a autorização de NF-e são operações distintas. O status comercial do pedido não comprova autorização fiscal.
- As operações dependem das permissões do usuário e das validações de cada módulo. O desenho resume os caminhos principais e não detalha todas as exceções, cancelamentos ou recuperações de acesso.
