import useAuth from "../../auth/useAuth";
import RecordMetadata from "../../components/record/RecordMetadata";
import ProductVariants from "./ProductVariants";
import { ProductItemContent } from "./ProductItemDetails";

export const productSections = [
  "general",
  "commercial",
  "variants",
  "presentations",
  "suppliers",
  "stock",
];
export default function ProductDetailsContent({
  catalog,
  item,
  section,
  onSectionChange,
  onChanged,
}) {
  const { has } = useAuth();
  const labels = {
    general: "Dados gerais",
    commercial: "Dados comerciais",
    variants: "Variações",
    presentations: "Apresentações",
    suppliers: "Fornecedores / opções",
    stock: "Estoque",
  };
  const allowed = productSections.filter(
    (key) =>
      (key !== "variants" || catalog.type === "VARIANT") &&
      (key !== "suppliers" || has("suppliers.read")) &&
      (key !== "stock" || has("stock.read")) &&
      (catalog.type === "SIMPLE" || !["commercial", "presentations", "suppliers"].includes(key)),
  );
  return (
    <section className="panel">
      <div className="customer-detail-title">
        <h2>{catalog.name}</h2>
        <span className={`badge ${catalog.active ? "active" : ""}`}>
          {catalog.active ? "Ativo" : "Inativo"}
        </span>
      </div>
      <nav className="product-toolbar product-tabs" aria-label="Seções do produto">
        {allowed.map((key) => (
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
      {!allowed.includes(section) ? (
        <p>Esta seção não está disponível para este produto ou cargo.</p>
      ) : section === "general" ? (
        <>
          <dl className="customer-details">
            {[
              ["Marca", catalog.brand?.name],
              ["Categoria", catalog.category?.name],
              ["Unidade BASE", catalog.unit || "Pendente de conferência"],
              ["Tipo", catalog.type === "SIMPLE" ? "Simples" : "Com variações"],
            ].map(([label, value]) => (
              <div key={label}>
                <dt>{label}</dt>
                <dd>{value || "—"}</dd>
              </div>
            ))}
          </dl>
          <RecordMetadata record={catalog} />
        </>
      ) : catalog.type === "VARIANT" ? (
        <ProductVariants
          key={`${catalog.id}:${section}`}
          catalog={catalog}
          pageDetails
          startStock={section === "stock"}
          onChanged={onChanged}
        />
      ) : (
        item && (
          <ProductItemContent
            key={item.id}
            catalog={catalog}
            item={item}
            section={section}
            onSectionChange={onSectionChange}
            onStockChanged={onChanged}
          />
        )
      )}
    </section>
  );
}
