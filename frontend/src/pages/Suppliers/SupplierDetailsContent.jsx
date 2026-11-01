import RecordMetadata from "../../components/record/RecordMetadata";
import SupplierAddresses from "./SupplierAddresses";
import SupplierCatalogLinksContent from "./SupplierCatalogLinksContent";

export const supplierSections = ["general", "addresses", "links", "metadata"];
export default function SupplierDetailsContent({ supplier, section, onSectionChange, onChanged }) {
  const labels = {
    general: "Dados gerais",
    addresses: "Endereços",
    links: "Marcas e categorias",
    metadata: "Metadados",
  };
  return (
    <section className="panel">
      <div className="customer-detail-title">
        <h2>{supplier.legalName}</h2>
        <span className={`badge ${supplier.active ? "active" : ""}`}>
          {supplier.active ? "Ativo" : "Inativo"}
        </span>
      </div>
      <nav className="product-toolbar" aria-label="Seções da ficha">
        {supplierSections.map((key) => (
          <button
            key={key}
            className={section === key ? "" : "secondary"}
            aria-current={section === key ? "page" : undefined}
            onClick={() => onSectionChange(key)}
          >
            {labels[key]}
          </button>
        ))}
      </nav>
      {section === "general" && (
        <>
          {" "}
          <dl className="customer-details">
            {[
              ["CNPJ", supplier.taxId],
              ["Nome fantasia", supplier.tradeName],
              [
                "Prazo médio de entrega",
                supplier.avgDeliveryDays == null ? "—" : `${supplier.avgDeliveryDays} dia(s)`,
              ],
              ["Telefone", supplier.phone],
              ["E-mail", supplier.email],
              ["WhatsApp", supplier.whatsapp],
            ].map(([label, text]) => (
              <div key={label}>
                <dt>{label}</dt>
                <dd>{text || "—"}</dd>
              </div>
            ))}
          </dl>
        </>
      )}
      {section === "addresses" && (
        <SupplierAddresses supplierId={supplier.id} onChanged={onChanged} />
      )}
      {section === "links" && (
        <SupplierCatalogLinksContent supplierId={supplier.id} onChanged={onChanged} />
      )}
      {section === "metadata" && <RecordMetadata record={supplier} />}
    </section>
  );
}
