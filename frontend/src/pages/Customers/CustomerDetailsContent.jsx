import useAuth from "../../auth/useAuth";
import RecordMetadata from "../../components/record/RecordMetadata";
import CustomerAddresses from "./CustomerAddresses";

export const customerSections = ["general", "addresses", "prices", "metadata"];
export default function CustomerDetailsContent({ customer, section, onSectionChange, onChanged }) {
  const { has } = useAuth();
  const labels = {
    general: "Dados gerais",
    addresses: "Endereços",
    prices: "Tabela de preços",
    metadata: "Metadados",
  };
  return (
    <section className="panel">
      <div className="customer-detail-title">
        <h2>{customer.legalName}</h2>
        <span className={`badge ${customer.active ? "active" : ""}`}>
          {customer.active ? "Ativo" : "Inativo"}
        </span>
      </div>
      <nav className="product-toolbar" aria-label="Seções da ficha">
        {customerSections
          .filter((key) => key !== "prices" || has("prices.read"))
          .map((key) => (
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
              ["Tipo", customer.personType === "PF" ? "Pessoa física" : "Pessoa jurídica"],
              [customer.personType === "PF" ? "CPF" : "CNPJ", customer.taxId],
              ...(customer.personType === "PJ"
                ? [["Nome fantasia", customer.tradeName]]
                : [["Nascimento", customer.birthDate?.split("-").reverse().join("/")]]),
              ["Inscrição estadual", customer.stateReg],
              ["Inscrição municipal", customer.municipalReg],
              ["Telefone", customer.phone],
              ["E-mail", customer.email],
              ["WhatsApp", customer.whatsapp],
              ...(has("prices.read")
                ? [["Tabela de preços", customer.priceTable?.name || "Sem tabela de preços"]]
                : []),
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
        <CustomerAddresses customerId={customer.id} onChanged={onChanged} />
      )}
      {section === "prices" &&
        (has("prices.read") ? (
          <p>Tabela de preços: {customer.priceTable?.name || "Sem tabela de preços"}</p>
        ) : (
          <p>Seu cargo não permite consultar tabelas de preços.</p>
        ))}
      {section === "metadata" && <RecordMetadata record={customer} />}
    </section>
  );
}
