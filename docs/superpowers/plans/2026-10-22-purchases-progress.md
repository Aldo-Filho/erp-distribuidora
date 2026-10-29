# Implementação: compras e recebimentos

Plano: [2026-10-22-purchases.md](2026-10-22-purchases.md).
Aprovação: usuário respondeu "ok, aprovado" em 22/10/2026. Implementação no checkout atual, preservando alterações locais anteriores e trabalho paralelo em usuários/estoque. Sem commit ou push.

## Entrega

- CRUD de compras e itens no padrão customer/customer.address, com filtros, DTOs records, respostas com entidades e services transacionais.
- Compra DRAFT, responsável e fornecedor obrigatórios/ativos, previsão opcional e remoção explícita da data no PATCH.
- DRAFT → SENT → CONFIRMED; edição somente em DRAFT, fornecedor alterável somente sem itens, exclusão de rascunho vazio, cancelamento antes de qualquer recebimento.
- Produto ativo e vínculo ativo de fornecedor na inclusão/troca do produto e ao enviar/confirmar. Custo explícito ou snapshot do catálogo, sem reprecificação posterior. Total calculado com BigDecimal, precisão monetária e limite NUMERIC(12,2).
- Depósito de destino por item, com StockItem previamente cadastrado e uma linha por produto na compra.
- Recebimentos parciais com histórico próprio, operador, notas normalizadas, data do backend, contador acumulado e derivação de PARTIALLY_RECEIVED/RECEIVED. Atualização de updatedAt em cada entrega.
- requestId UUID único por compra. Retry idêntico devolve o lote anterior antes de validar estado/operador ativo; mudança de conteúdo com mesma chave é rejeitada. Ordem dos itens não afeta a comparação.
- Bloqueios compra → itens por ID → estoques por ID. Edição/exclusão de item consulta o ID da compra sem lock e só bloqueia o item após a raiz; consultas bloqueadas são restritas à compra. Origem do estoque é revalidada após lock.
- Toda entrega gera movimentos INBOUND/PURCHASE com saldos anterior/posterior e vínculo único à linha recebida. O coordenador incrementa receivedQuantity; StockMovementService altera somente o saldo físico e registra o movimento, preservando reservas.
- V1 inclui depósito nos itens, tabelas de histórico e vínculo único/constraint de origem no movimento. V2 informa o depósito no item histórico; não cria lotes retroativos.
- Sem frontend, fiscal, financeiro, devoluções, estornos, sugestões automáticas ou alteração de custos de produto/catálogo.

## API

CRUDs: /purchase e /purchaseItem, com /search, /{id}, POST, PATCH e DELETE, conforme customer.

Receber: POST /purchase/{id}/receive. Nome interno do método: register, seguindo os verbos de AGENTS.md. Retorno 201 na criação, 200 no retry válido. O record interno ReceiptRegistration não é o corpo HTTP; o corpo é a entidade PurchaseReceipt com suas linhas.

Exemplo de corpo para uma entrega (IDs ilustrativos):

```json
{
  "requestId": "e4db3192-46c0-4f4e-b02b-229d72c2ce65",
  "operatorUserId": 1,
  "notes": "Primeira entrega",
  "items": [
    { "purchaseItemId": 10, "quantity": 4 }
  ]
}
```

Para receber as 6 unidades restantes de uma linha comprada com 10, enviar nova chamada com outro requestId e quantity 6. Reutilizar o primeiro requestId somente para repetir a primeira entrega sem entrada adicional.

Histórico: GET /purchase/{id}/receipts e GET /purchase/{purchaseId}/receipts/{id}. O último consulta o lote dentro da compra e retorna 204 quando ausente. Histórico não tem PATCH/DELETE.

## Revisão e limites

Revisão direta e independente estática dos arquivos novos e integrações, incluindo schema/seed, custos, estados, serialização, locks e retries. Nenhum achado acionável na revisão independente inicial. Conferidos por leitura os cenários e contratos da tarefa 7 do plano; os itens marcados representam revisão estática, não execução dos cenários.

Não foram criados/executados testes, build ou lint, conforme AGENTS.md. Não houve aplicação de migrations, recriação do banco ou chamadas HTTP. Compilação, SQL/ORM, serialização HTTP e concorrência em runtime permanecem sem comprovação. Como V1/V2 foram alteradas para banco recriável, esta entrega não é uma migração incremental de banco existente.
