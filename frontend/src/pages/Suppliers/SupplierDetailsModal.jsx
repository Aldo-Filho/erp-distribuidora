import { useEffect, useState } from "react";
import useAuth from "../../auth/useAuth";
import { suppliers } from "../../services/suppliers";
import Modal from "../../components/feedback/Modal";
import ErrorAlert from "../../components/feedback/ErrorAlert";
import RecordMetadata from "../../components/record/RecordMetadata";
import { addressTypes } from "./SupplierAddressFields";

export default function SupplierDetailsModal({
  supplierId,
  onClose,
  onEdit,
  onAddresses,
  onCatalogLinks,
}) {
  const { has } = useAuth();
  const [state, setState] = useState({ loading: true });
  const [addressState, setAddressState] = useState({ loading: true });
  const [revision, setRevision] = useState(0);
  const [addressRevision, setAddressRevision] = useState(0);
  useEffect(() => {
    let live = true;
    const controller = new AbortController();
    setState({ loading: true });
    suppliers.findOne(supplierId, { signal: controller.signal }).then(
      (supplier) => {
        if (live) setState({ supplier });
      },
      (error) => {
        if (live) setState({ error: error.message });
      },
    );
    return () => {
      live = false;
      controller.abort();
    };
  }, [supplierId, revision]);
  useEffect(() => {
    let live = true;
    const controller = new AbortController();
    setAddressState({ loading: true });
    suppliers.addresses(supplierId, { signal: controller.signal }).then(
      (addresses) => {
        if (live) setAddressState({ addresses });
      },
      (error) => {
        if (live) setAddressState({ error: error.message });
      },
    );
    return () => {
      live = false;
      controller.abort();
    };
  }, [supplierId, addressRevision]);
  const supplier = state.supplier;
  return (
    <Modal title="Detalhes do fornecedor" onClose={onClose} wide>
      <ErrorAlert message={state.error} retry={() => setRevision((number) => number + 1)} />
      {state.loading ? (
        <p role="status">Carregando fornecedor…</p>
      ) : supplier ? (
        <>
          <div className="customer-detail-title">
            <h3>{supplier.legalName}</h3>
            <span className={"badge " + (supplier.active ? "active" : "")}>
              {supplier.active ? "Ativo" : "Inativo"}
            </span>
          </div>
          <dl className="customer-details">
            {[
              ["CNPJ", supplier.taxId],
              ["Nome fantasia", supplier.tradeName],
              ["Prazo médio de entrega", supplier.avgDeliveryDays == null
                ? "—" : `${supplier.avgDeliveryDays} dia(s)`],
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
          <section className="customer-detail-addresses">
            <h3>Endereços</h3>
            <ErrorAlert
              message={addressState.error}
              retry={() => setAddressRevision((number) => number + 1)}
            />
            {addressState.loading ? (
              <p role="status">Carregando endereços…</p>
            ) : addressState.addresses?.length ? (
              addressState.addresses.map((address) => (
                <div className="customer-address-card" key={address.id}>
                  <strong>{addressTypes[address.addressType]}</strong>
                  <p>
                    {address.street}, {address.number}
                    {address.complement ? ` · ${address.complement}` : ""}
                    <br />
                    {address.neighborhood} · {address.city}/{address.state}
                    {" · "}{address.country} · CEP {address.zipCode}
                  </p>
                </div>
              ))
            ) : (
              !addressState.error && <p>Nenhum endereço cadastrado.</p>
            )}
          </section>
          <RecordMetadata record={supplier} />
          <div className="actions">
            {onCatalogLinks && has("suppliers.read") && (
              <button type="button" className="secondary" onClick={() => onCatalogLinks(supplier.id)}>
                Marcas e categorias
              </button>
            )}
            <button type="button" className="secondary" onClick={() => onAddresses(supplier.id)}>
              Endereços
            </button>
            {has("suppliers.update") && (
              <button type="button" onClick={() => onEdit(supplier)}>
                Editar fornecedor
              </button>
            )}
          </div>
        </>
      ) : (
        !state.error && <p>Fornecedor não encontrado.</p>
      )}
    </Modal>
  );
}
