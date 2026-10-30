const messages = {
  "User must be active.": "O usuário atribuído ao pedido está inativo. Selecione um usuário ativo.",
  "Presentation must be active and belong to the product.":
    "A apresentação foi alterada ou não pertence a este SKU. Recarregue as apresentações e selecione novamente.",
  "Presentation does not belong to the selected product.":
    "A apresentação selecionada pertence a outro SKU. Selecione novamente a apresentação deste produto.",
  "Presentation must be active.":
    "Esta apresentação está inativa. Selecione uma apresentação ativa para continuar.",
  "Product presentation not found.":
    "A apresentação não está mais disponível. Recarregue as opções deste produto.",
  "Product must be active.": "Este produto está inativo. Selecione um produto ativo.",
  "All order products must be active.":
    "O pedido contém um produto inativo. Corrija os itens antes de confirmar.",
  "Customer must be active.":
    "O cliente está inativo. Selecione um cliente ativo antes de confirmar.",
  "Only pending orders can be edited.":
    "A situação do pedido mudou. Atualize a consulta; somente pedidos pendentes podem ser editados.",
  "Only pending orders can have their items modified.":
    "A situação do pedido mudou. Atualize a consulta antes de alterar os itens.",
  "Product has no stock item in the selected warehouse.":
    "Este produto não tem estoque configurado no depósito selecionado. Escolha outro depósito ou configure o item em Estoque.",
  "An order product has no stock item in its selected warehouse.":
    "Um item do pedido não tem estoque configurado no depósito escolhido. Corrija o depósito antes de confirmar.",
  "Insufficient available stock for the order.":
    "Estoque disponível insuficiente para reservar o pedido. Confira os saldos e as quantidades de todos os itens.",
  "This product already exists in this order.":
    "Este produto e apresentação já estão no pedido para esse depósito. Edite a linha existente.",
  "This product presentation already exists in this order and warehouse.":
    "Esta combinação de produto, apresentação e depósito já está no pedido. Edite a linha existente.",
  "Discount percentage cannot exceed 100.": "O desconto percentual não pode ultrapassar 100%.",
  "Discount cannot exceed the item gross amount.":
    "O desconto não pode ser maior que o valor bruto do item.",
};

export function salesError(message) {
  return messages[message] || message;
}
