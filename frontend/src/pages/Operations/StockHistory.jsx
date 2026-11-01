import Modal from "../../components/feedback/Modal";
import StockHistoryContent from "./StockHistoryContent";

export default function StockHistory({ filter, onClose }) {
  return (
    <Modal title="Histórico de movimentos · BASE" onClose={onClose} wide className="stock-dialog">
      <StockHistoryContent filter={filter} />
      <div className="actions">
        <button type="button" className="secondary" onClick={onClose}>
          Fechar
        </button>
      </div>
    </Modal>
  );
}
