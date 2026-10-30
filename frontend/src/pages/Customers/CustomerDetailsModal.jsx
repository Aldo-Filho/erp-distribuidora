import { useEffect, useState } from "react";
import useAuth from "../../auth/useAuth";
import { customers } from "../../services/customers";
import Modal from "../../components/feedback/Modal";
import ErrorAlert from "../../components/feedback/ErrorAlert";
import RecordMetadata from "../../components/record/RecordMetadata";
import { addressTypes } from "./CustomerAddressFields";
import "./customerDetails.css";

export default function CustomerDetailsModal({ customerId, onClose, onEdit, onAddresses }) {
  const { has } = useAuth();
  const [state, setState] = useState({ loading: true });
  const [addressState, setAddressState] = useState({ loading: true });
  const [revision, setRevision] = useState(0);
  const [addressRevision, setAddressRevision] = useState(0);
  useEffect(() => {
    let live = true;
    const controller = new AbortController();
    setState({ loading: true });
    customers.findOne(customerId, { signal: controller.signal }).then(
      (customer) => {
        if (live) setState({ customer });
      },
      (error) => {
        if (live) setState({ error: error.message });
      },
    );
    return () => {
      live = false;
      controller.abort();
    };
  }, [customerId, revision]);
  useEffect(() => {
    let live = true;
    const controller = new AbortController();
    setAddressState({ loading: true });
    customers.addresses(customerId, { signal: controller.signal }).then(
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
  }, [customerId, addressRevision]);
  const customer = state.customer;
  return (
    <Modal title="Detalhes do cliente" onClose={onClose} wide className="customer-details-dialog">
      <ErrorAlert message={state.error} retry={() => setRevision((number) => number + 1)} />
      {state.loading ? (
        <p role="status">Carregando cliente…</p>
      ) : customer ? (
        <>
          <div className="customer-detail-title">
            <h3>{customer.legalName}</h3>
            <span className={"badge " + (customer.active ? "active" : "")}>
              {customer.active ? "Ativo" : "Inativo"}
            </span>
          </div>
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
              ["Tabela de preços", customer.priceTable?.name || "Sem tabela de preços"],
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
              <div className="customer-details-address-grid">
                {addressState.addresses.map((address) => (
                  <div className="customer-details-address-card" key={address.id}>
                    <strong>{addressTypes[address.addressType]}</strong>
                    <p>
                      {address.street}, {address.number}
                      {address.complement ? ` · ${address.complement}` : ""}
                      <br />
                      {address.neighborhood} · {address.city}/{address.state} · CEP{" "}
                      {address.zipCode}
                    </p>
                  </div>
                ))}
              </div>
            ) : (
              !addressState.error && <p>Nenhum endereço cadastrado.</p>
            )}
          </section>
          <RecordMetadata record={customer} />
          <div className="actions">
            <button type="button" className="secondary" onClick={() => onAddresses(customer.id)}>
              Endereços
            </button>
            {has("customers.update") && (
              <button type="button" onClick={() => onEdit(customer)}>
                Editar cliente
              </button>
            )}
          </div>
        </>
      ) : (
        !state.error && <p>Cliente não encontrado.</p>
      )}
    </Modal>
  );
}
