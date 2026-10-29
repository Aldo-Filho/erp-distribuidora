# Plano e execução: depósito por item do pedido

**Objetivo:** permitir produtos de um mesmo pedido saírem de depósitos diferentes, preparando a identificação de origem para a integração de estoque.

**Especificação:** o usuário escolheu um depósito diferente por item. Preservar a regra já aprovada de uma linha por produto em cada pedido; não introduzir alocação de uma mesma linha entre vários depósitos.

**Arquitetura:** OrderItem passa a referenciar Warehouse, além de Product. Os DTOs recebem warehouseId; os services verificam a existência do estoque do par depósito/produto. A reserva e a baixa física continuam sendo operações futuras, sem alterar StockMovementService nesta entrega.

**Restrições:** seguir AGENTS.md e customer/address; preservar alterações concorrentes; não criar/executar testes, build ou lint; não fazer commit/push, recriar banco ou implementar faturamento. A preferência anterior do usuário por incorporar mudanças à V1 e recriar o banco é aplicada aqui; a V2 recebe somente a coluna necessária para manter os inserts válidos.

## Contrato

- warehouseId é obrigatório no cadastro do item e opcional no PATCH. Omissão/null no PATCH preserva o depósito.
- O depósito e o produto devem existir, e o produto deve possuir StockItem cadastrado naquele depósito.
- Produtos diferentes do mesmo pedido podem selecionar depósitos diferentes.
- A troca de depósito só ocorre com pedido PENDING, como as demais alterações de item; na troca simultânea de produto/depósito, validar a combinação final antes dos setters.
- Alterar apenas depósito não recalcula o preço unitário: preserva o snapshot salvo e as regras existentes de total e desconto.
- A confirmação revalida o par produto/depósito de cada item. Nesta etapa não verifica disponibilidade de quantidade nem reserva estoque; existência de cadastro não garante saldo.
- A verificação de existência é pontual; impedir remoção de StockItem com reservas dependerá do modelo persistido de reservas na integração seguinte.
- As respostas da entidade incluem warehouse; o filtro de itens aceita warehouseId.

## Tarefas

- [x] Mapear OrderItem.warehouse e ajustar construtor, RequestOrderItemDTO, PatchOrderItemDTO e OrderItemFilter.
- [x] Resolver e validar depósito/produto em register/update, mantendo locks, snapshots e regra de duplicidade.
- [x] Revalidar estoque cadastrado no depósito ao confirmar o pedido.
- [x] Incluir warehouse_id obrigatório na V1 e atualizar o insert de order_items na V2 com o warehouse_key já usado pelo estoque e movimento de venda daquele cenário.
- [x] Revisar diretamente e por revisão independente estática os cenários: depósitos diferentes no mesmo pedido, par inexistente, PATCH parcial, troca combinada, confirmação, seed e preservação de valores.

Revisões concluídas sem achados acionáveis. Não foram executados testes, build, lint, migrations, banco ou verificações de concorrência em runtime. Banco não recriado; alterações mantidas locais sem commit.

## Próximo passo

Definir e persistir reservas por item/StockItem, para reservar na confirmação e liberar no cancelamento. A baixa comercial deverá consumir essas reservas e registrar OUTBOUND/SALE na mesma transação. A seleção do depósito, por si só, não executa nenhuma dessas operações.
