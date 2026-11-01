import { useState } from "react";
import { OrderHeaderForm } from "./OrdersPage";
import { WarehouseSelect, partyName } from "./OperationSelectors";

const payments = {
  CASH: "Dinheiro",
  CREDIT_CARD: "Cartão de crédito",
  DEBIT_CARD: "Cartão de débito",
  BOLETO: "Boleto (somente registro)",
  PIX: "Pix",
  BANK_TRANSFER: "Transferência bancária",
};

export default function OrderEntryHeader({
  order,
  defaultWarehouseId,
  onWarehouseChange,
  onCreated,
  onSaved,
  disabled,
  canEdit,
  writeHeader,
  onStateChange,
  closeRef,
  onClosed,
}) {
  const [editing, setEditing] = useState(false);
  if (!order || editing) {
    return (
      <OrderHeaderForm
        key={order?.id || "new"}
        order={order}
        embedded
        onStateChange={onStateChange}
        closeRef={closeRef}
        writeHeader={writeHeader}
        onClose={() => {
          if (order) {
            setEditing(false);
            onClosed?.();
          } else onSaved?.();
        }}
        onSaved={(saved) => {
          setEditing(false);
          if (order) onSaved?.(saved);
          else onCreated(saved);
        }}
      />
    );
  }
  return (
    <div className="order-entry-header">
      <div>
        <span className="field-hint">Cliente</span>
        <strong>{partyName(order.customer)}</strong>
      </div>
      <div>
        <span className="field-hint">Pagamento</span>
        <strong>{payments[order.paymentMethod] || "Não informado"}</strong>
      </div>
      <WarehouseSelect
        value={defaultWarehouseId}
        onChange={(event) => onWarehouseChange(event.target.value)}
        disabled={disabled}
      />
      {canEdit && (
        <button
          type="button"
          className="secondary"
          disabled={disabled}
          onClick={() => setEditing(true)}
        >
          Alterar cliente e pagamento
        </button>
      )}
    </div>
  );
}
