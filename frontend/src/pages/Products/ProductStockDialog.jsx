import useAuth from "../../auth/useAuth";
import Modal from "../../components/feedback/Modal";
import ProductStockPanel from "./ProductStockPanel";

export default function ProductStockDialog({ item, onClose, onChanged }) {
  const { has } = useAuth();
  if (!has("stock.read")) return null;
  return (
    <Modal title={`Estoque · ${item.sku}`} onClose={onClose} wide className="product-stock-dialog">
      <ProductStockPanel item={item} onChanged={onChanged} />
    </Modal>
  );
}
