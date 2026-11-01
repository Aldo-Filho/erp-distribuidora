import { useCallback, useState } from "react";
import { Pencil, Plus, Power, Star } from "lucide-react";
import useAuth from "../../auth/useAuth";
import Modal from "../../components/feedback/Modal";
import FilterSelect from "../../components/feedback/FilterSelect";
import RowActions from "../../components/feedback/RowActions";
import ErrorAlert from "../../components/feedback/ErrorAlert";
import { productSuppliers } from "../../services/productSuppliers";
import { suppliers } from "../../services/suppliers";
import { useCatalogPage, useProductResource } from "./useProductResource";
import useProductAction from "./useProductAction";
import useProductForm from "./useProductForm";
import { ProductFilters, ProductLoad, ProductPageFooter } from "./ProductControls";
import { money, quantity } from "./productFormData";
import ProductSupplierOptionForm from "./ProductSupplierOptionForm";

function NewProductSupplier({ item, links, onClose, onSaved }) {
  const { has } = useAuth();
  const list = useCatalogPage(suppliers.search, { query: "", searchField: "NAME", active: "" });
  const form = useProductForm(
    `supplier-link:${item.id}:new`,
    { supplierId: "", supplierName: "", preferred: false },
    onClose,
    onSaved,
  );
  const write = async () => {
    if (!has("suppliers.update")) throw new Error("Sem permissão para vincular fornecedores.");
    if (!form.value.supplierId) throw new Error("Selecione um fornecedor nos resultados da busca.");
    if (links.some((link) => String(link.supplier.id) === form.value.supplierId))
      throw new Error("Este fornecedor já está vinculado ao SKU. Edite suas opções.");
    return productSuppliers.register({
      productId: item.id,
      supplierId: Number(form.value.supplierId),
      preferred: form.value.preferred,
    });
  };
  return (
    <Modal title="Vincular fornecedor" onClose={form.close} busy={form.saving} wide>
      <p>{item.sku}. O vínculo é opcional e não cria marca, categoria ou opção automaticamente.</p>
      <ErrorAlert message={form.error} />
      <div className="customer-form">
        <fieldset
          className="customer-form-fields"
          disabled={form.saving || !has("suppliers.update")}
        >
          <label>
            Buscar fornecedor por nome
            <input
              type="search"
              value={list.filters.query}
              onChange={(e) => list.changeFilters({ ...list.filters, query: e.target.value })}
            />
          </label>
          <ProductLoad state={list} empty="Nenhum fornecedor encontrado.">
            <div className="product-supplier-picker">
              {list.data?.content.map((supplier) => {
                const linked = links.some((link) => link.supplier.id === supplier.id);
                return (
                  <label className="check" key={supplier.id}>
                    <input
                      type="radio"
                      name="product-supplier"
                      disabled={linked || form.saving}
                      checked={form.value.supplierId === String(supplier.id)}
                      onChange={() =>
                        form.setValue({
                          ...form.value,
                          supplierId: String(supplier.id),
                          supplierName: supplier.legalName,
                        })
                      }
                    />
                    {supplier.legalName}
                    {!supplier.active ? " · Inativo" : ""}
                    {linked ? " · Já vinculado" : ""}
                  </label>
                );
              })}
            </div>
          </ProductLoad>
          <ProductPageFooter list={list} />
          {form.value.supplierId && <p>Selecionado: {form.value.supplierName}</p>}
          <label className="check">
            <input
              type="checkbox"
              checked={form.value.preferred}
              onChange={(e) => form.change("preferred", e.target.checked)}
            />
            Fornecedor preferencial para este SKU
          </label>
          <p className="muted">
            Marcar preferencial substitui a preferência anterior. As embalagens não possuem
            preferências individuais.
          </p>
        </fieldset>
        <div className="actions">
          <button type="button" className="secondary" disabled={form.saving} onClick={form.close}>
            Cancelar
          </button>
          <button
            type="button"
            onClick={(e) => form.submit(e, write)}
            disabled={form.saving || !has("suppliers.update")}
          >
            {form.saving ? "Salvando…" : "Vincular fornecedor"}
          </button>
        </div>
      </div>
    </Modal>
  );
}

function SupplierOptions({ catalog, item, link }) {
  const { has } = useAuth();
  const read = useCallback(
    (filter, page, size, options) => productSuppliers.options(link.id, filter, page, size, options),
    [link.id],
  );
  const list = useCatalogPage(read);
  const action = useProductAction();
  const [flow, setFlow] = useState(null);
  const unavailable = !link.active || !link.supplier.active || !item.active;
  const toggle = (option) => {
    if (!has("suppliers.update")) return;
    return action.run(
      async () => {
        await productSuppliers.updateOption(option.id, { active: !option.active });
        list.reload();
      },
      `${option.active ? "Inativar" : "Ativar"} a opção ${option.name}? Isso preserva histórico e não altera a situação do produto, vínculo ou fornecedor.`,
      {
        title: option.active ? "Inativar opção de compra" : "Ativar opção de compra",
        confirmLabel: option.active ? "Inativar" : "Ativar",
        destructive: option.active,
        successMessage: option.active ? "Opção de compra inativada." : "Opção de compra ativada.",
      },
    );
  };
  return (
    <section>
      <div className="product-toolbar">
        <h3>Opções de {link.supplier.legalName}</h3>
        {has("suppliers.update") && (
          <button
            className="customer-icon-label"
            disabled={action.pending || !catalog.unit}
            onClick={() => setFlow({})}
          >
            <Plus size={17} />
            Nova opção
          </button>
        )}
      </div>
      {unavailable && (
        <p className="product-warning">
          Indisponível para novas compras:{" "}
          {!item.active
            ? "produto inativo"
            : !link.active
              ? "vínculo inativo"
              : "fornecedor inativo"}
          . As situações próprias das opções são preservadas.
        </p>
      )}
      <ProductFilters list={list} placeholder="Nome da opção de compra" />
      <ErrorAlert message={action.error} />
      {action.notice && (
        <p role="status" className="success">
          {action.notice}
        </p>
      )}
      <ProductLoad state={list} empty="Nenhuma opção cadastrada para este fornecedor / SKU.">
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th>Opção</th>
                <th>Equivalência BASE</th>
                <th>Custo comercial</th>
                <th>Código / prazo</th>
                <th>Situação</th>
                <th>Ações</th>
              </tr>
            </thead>
            <tbody>
              {list.data?.content.map((option) => (
                <tr key={option.id}>
                  <td>
                    <strong>{option.name}</strong>
                    <small>
                      {!option.conversionConfirmed
                        ? "Conversão legada pendente"
                        : option.basePresentation
                          ? "Unidade base"
                          : "Embalagem composta"}
                    </small>
                  </td>
                  <td>
                    {quantity(option.baseFactor)} {catalog.unit || "Unidade pendente"}
                  </td>
                  <td>{money(option.cost)}</td>
                  <td>
                    {option.supplierProductCode || "Sem código"}
                    <small>
                      {option.effectiveAvgDeliveryDays == null
                        ? "Prazo não informado"
                        : `${option.effectiveAvgDeliveryDays} dia(s) · ${option.avgDeliveryDays == null ? "prazo geral" : "prazo específico"}`}
                    </small>
                  </td>
                  <td>
                    {option.active ? "Ativa própria" : "Inativa própria"}
                    {(unavailable || !option.conversionConfirmed) && <small>Indisponível</small>}
                  </td>
                  <td>
                    {has("suppliers.update") && (
                      <RowActions label={`Ações de ${option.name}`}>
                        <button
                          title={!option.conversionConfirmed ? "Conferir opção legada" : "Editar"}
                          aria-label={`Editar ${option.name}`}
                          className="secondary icon-button"
                          disabled={action.pending}
                          onClick={() => setFlow({ option })}
                        >
                          <Pencil size={17} />
                        </button>
                        <button
                          title={option.active ? "Inativar" : "Ativar"}
                          aria-label={`${option.active ? "Inativar" : "Ativar"} ${option.name}`}
                          className="secondary icon-button row-status-action"
                          disabled={action.pending}
                          onClick={() => toggle(option)}
                        >
                          <Power size={17} />
                        </button>
                      </RowActions>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </ProductLoad>
      <ProductPageFooter list={list} />
      {flow && has("suppliers.update") && (
        <ProductSupplierOptionForm
          catalog={catalog}
          item={item}
          link={link}
          option={flow.option}
          onClose={() => setFlow(null)}
          onSaved={() => {
            setFlow(null);
            list.reload();
            action.succeed("Opção de compra salva.");
          }}
        />
      )}
    </section>
  );
}

export default function ProductSuppliers({ catalog, item }) {
  const { has } = useAuth();
  const state = useProductResource(
    (options) => productSuppliers.search({ productId: item.id }, options),
    [item.id],
    has("suppliers.read"),
  );
  const action = useProductAction();
  const [selected, setSelected] = useState("");
  const [creating, setCreating] = useState(false);
  const link = state.data?.find((entry) => String(entry.id) === selected);
  const changeLink = (link, body, message, label) => {
    if (!has("suppliers.update")) return;
    let successMessage = body.preferred
      ? "Fornecedor definido como preferencial."
      : "Preferência removida.";
    if (body.active != null) {
      successMessage = body.active ? "Vínculo ativado." : "Vínculo inativado.";
    }
    return action.run(
      async () => {
        await productSuppliers.update(link.id, body);
        state.reload();
      },
      message,
      {
        title: label ? `${label} vínculo` : undefined,
        confirmLabel: label,
        destructive: body.active === false,
        successMessage,
      },
    );
  };
  if (!has("suppliers.read")) return null;
  return (
    <section>
      <div className="product-toolbar">
        <h3>Fornecedores do SKU</h3>
        {has("suppliers.update") && (
          <button
            className="customer-icon-label"
            disabled={action.pending || state.loading || !!state.error}
            onClick={() => setCreating(true)}
          >
            <Plus size={17} />
            Vincular fornecedor
          </button>
        )}
      </div>
      <p className="muted">
        Preferência por fornecedor e SKU, opcional. Opções de compra não alteram custo de
        referência, preço ou apresentações de venda.
      </p>
      <ErrorAlert message={action.error} />
      {action.notice && (
        <p role="status" className="success">
          {action.notice}
        </p>
      )}
      <ProductLoad state={state} empty="Nenhum fornecedor vinculado.">
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th>Fornecedor</th>
                <th>Preferência</th>
                <th>Situação do vínculo</th>
                <th>Ações</th>
              </tr>
            </thead>
            <tbody>
              {state.data?.map((entry) => (
                <tr key={entry.id}>
                  <td>
                    {entry.supplier.legalName}
                    {!entry.supplier.active && <small>Fornecedor inativo</small>}
                  </td>
                  <td>{entry.preferred ? "Preferencial" : "—"}</td>
                  <td>{entry.active ? "Ativo" : "Inativo"}</td>
                  <td>
                    {has("suppliers.update") && (
                      <RowActions label={`Ações do vínculo ${entry.supplier.legalName}`}>
                        <button
                          title={entry.preferred ? "Retirar preferência" : "Tornar preferencial"}
                          aria-label={`Preferência de ${entry.supplier.legalName}`}
                          className="secondary icon-button"
                          disabled={action.pending}
                          onClick={() => changeLink(entry, { preferred: !entry.preferred })}
                        >
                          <Star size={17} />
                        </button>
                        <button
                          title={entry.active ? "Inativar vínculo" : "Ativar vínculo"}
                          aria-label={`${entry.active ? "Inativar" : "Ativar"} vínculo com ${entry.supplier.legalName}`}
                          className="secondary icon-button row-status-action"
                          disabled={action.pending}
                          onClick={() =>
                            changeLink(
                              entry,
                              { active: !entry.active },
                              `${entry.active ? "Inativar" : "Ativar"} o vínculo com ${entry.supplier.legalName}? A inativação impede novas compras em todas as suas opções, preservando saldos, histórico e compromissos já confirmados.`,
                              entry.active ? "Inativar" : "Ativar",
                            )
                          }
                        >
                          <Power size={17} />
                        </button>
                      </RowActions>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <label>
          Consultar opções de compra
          <FilterSelect
            value={selected}
            disabled={action.pending}
            onChange={(e) => setSelected(e.target.value)}
          >
            <option value="">Selecione um fornecedor vinculado</option>
            {state.data?.map((entry) => (
              <option key={entry.id} value={entry.id}>
                {entry.supplier.legalName}
                {entry.preferred ? " · Preferencial" : ""}
                {!entry.active ? " · Vínculo inativo" : ""}
              </option>
            ))}
          </FilterSelect>
        </label>
      </ProductLoad>
      {link && !state.loading && (
        <SupplierOptions key={`${item.id}:${link.id}`} catalog={catalog} item={item} link={link} />
      )}
      {creating && has("suppliers.update") && (
        <NewProductSupplier
          item={item}
          links={state.data || []}
          onClose={() => setCreating(false)}
          onSaved={(saved) => {
            setCreating(false);
            setSelected(String(saved.id));
            state.reload();
            action.succeed("Fornecedor vinculado.");
          }}
        />
      )}
    </section>
  );
}
