function Field({ label, ...inputProps }) {
  // Campo reutilizável: as demais propriedades chegam diretamente ao <input>.
  return (
    <label className="grid min-w-0 gap-1.5 text-xs font-semibold text-slate-700">
      <span>{label}</span>
      <input
        className="h-11 min-w-0 w-full rounded-lg border border-slate-300 px-3 text-base outline-blue-600 disabled:cursor-not-allowed disabled:bg-slate-100 sm:h-10 sm:text-sm"
        {...inputProps}
      />
    </label>
  );
}

function Select({ label, options, placeholder = "Selecione", ...selectProps }) {
  // Seleção reutilizável para relacionamentos vindos da API (marca e categoria).
  return (
    <label className="grid min-w-0 gap-1.5 text-xs font-semibold text-slate-700">
      <span>{label}</span>
      <select
        className="h-11 min-w-0 w-full rounded-lg border border-slate-300 bg-white px-3 text-base outline-blue-600 sm:h-10 sm:text-sm"
        {...selectProps}
      >
        <option value="">{placeholder}</option>
        {options.map((option) => (
          <option key={option.id} value={option.id}>
            {option.name}
          </option>
        ))}
      </select>
    </label>
  );
}

export function ProductForm({
  product,
  brands,
  categories,
  warehouses,
  warehouseError,
  onRetryWarehouses,
  error,
  form,
  onChange,
  onCancel,
  onSubmit,
  saving,
}) {
  /**
   * Este componente apenas mostra campos. ProductsPage mantém o estado do
   * formulário, decide criar/editar e chama a API, deixando o formulário simples
   * de reutilizar em qualquer módulo com a mesma necessidade.
   */
  const isNew = product === "new";

  return (
    <form
      className="grid min-w-0 grid-cols-1 gap-3 break-words sm:grid-cols-2 sm:gap-4"
      onSubmit={onSubmit}
    >
      {error && (
        <p className="col-span-full rounded-lg bg-red-50 p-3 text-sm text-red-700" role="alert">
          {error}
        </p>
      )}
      <fieldset
        disabled={saving}
        className="col-span-full grid min-w-0 grid-cols-1 gap-3 sm:grid-cols-2 sm:gap-4"
      >
        <Field label="Nome" name="name" value={form.name} onChange={onChange} required />
        {/* O backend não altera SKU no PATCH; ele só é editável ao cadastrar. */}
        <Field
          label="SKU"
          name="sku"
          value={form.sku}
          onChange={onChange}
          required
          disabled={!isNew}
        />
        <Select
          label="Marca"
          name="brandId"
          value={form.brandId}
          onChange={onChange}
          required
          options={brands}
        />
        <Select
          label="Categoria"
          name="categoryId"
          value={form.categoryId}
          onChange={onChange}
          options={categories}
          placeholder="Sem categoria"
        />
        <Field
          label="Custo"
          name="cost"
          type="number"
          step="0.01"
          min="0"
          value={form.cost}
          onChange={onChange}
          required
        />
        <Field
          label="Preço de venda"
          name="price"
          type="number"
          step="0.01"
          min="0"
          value={form.price}
          onChange={onChange}
          required
        />
        <Field
          label="Peso (kg)"
          name="weightKg"
          type="number"
          step="0.001"
          min="0"
          value={form.weightKg}
          onChange={onChange}
        />
        <Field label="Cor" name="color" value={form.color} onChange={onChange} />
        <Field
          label="Comprimento"
          name="dimensionX"
          type="number"
          step="0.01"
          min="0"
          value={form.dimensionX}
          onChange={onChange}
        />
        <Field
          label="Largura"
          name="dimensionY"
          type="number"
          step="0.01"
          min="0"
          value={form.dimensionY}
          onChange={onChange}
        />
        <Field
          label="Altura"
          name="dimensionZ"
          type="number"
          step="0.01"
          min="0"
          value={form.dimensionZ}
          onChange={onChange}
        />
        <Field label="Tamanho" name="size" value={form.size} onChange={onChange} />
      </fieldset>
      {isNew && (
        <fieldset
          disabled={saving}
          className="col-span-full grid min-w-0 grid-cols-1 gap-3 border-t border-slate-200 pt-4 sm:grid-cols-2 sm:gap-4"
        >
          <legend className="px-1 text-sm font-semibold text-slate-900">Estoque inicial</legend>
          <p className="col-span-full text-xs text-slate-500">
            Opcional: selecione um armazém para cadastrar o estoque junto com o produto.
          </p>
          {warehouseError && (
            <p className="col-span-full text-sm text-red-700" role="alert">
              {warehouseError}{" "}
              <button type="button" className="font-semibold underline" onClick={onRetryWarehouses}>
                Tentar novamente
              </button>
            </p>
          )}
          {!warehouseError && warehouses.length === 0 && (
            <p className="col-span-full text-xs text-slate-500">
              Cadastre um armazém no menu Armazéns para informar o estoque inicial.
            </p>
          )}
          <Select
            label="Armazém"
            name="warehouseId"
            value={form.warehouseId}
            onChange={onChange}
            options={warehouses}
            placeholder="Sem estoque inicial"
          />
          {form.warehouseId && (
            <>
              <Field
                label="Quantidade em estoque"
                name="quantity"
                type="number"
                min="0"
                max="2147483647"
                step="1"
                required
                value={form.quantity}
                onChange={onChange}
              />
              <Field
                label="Quantidade reservada"
                name="reservedQuantity"
                type="number"
                min="0"
                max={form.quantity || "0"}
                step="1"
                required
                value={form.reservedQuantity}
                onChange={onChange}
              />
              <Field
                label="Estoque mínimo"
                name="minQuantity"
                type="number"
                min="0"
                max="2147483647"
                step="1"
                required
                value={form.minQuantity}
                onChange={onChange}
              />
              <Field
                label="Estoque máximo"
                name="maxQuantity"
                type="number"
                min={form.minQuantity || "0"}
                max="2147483647"
                step="1"
                required
                value={form.maxQuantity}
                onChange={onChange}
              />
              <p className="col-span-full text-xs text-slate-500">
                Quantidade disponível:{" "}
                {Math.max(0, Number(form.quantity) - Number(form.reservedQuantity))}
              </p>
            </>
          )}
        </fieldset>
      )}
      <div className="col-span-full mt-2 flex flex-col-reverse gap-2 border-t border-slate-200 pt-4 sm:flex-row sm:justify-end">
        <button
          type="button"
          className="min-h-11 w-full rounded-lg border border-slate-200 px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50 sm:w-auto"
          onClick={onCancel}
          disabled={saving}
        >
          Cancelar
        </button>
        <button
          className="min-h-11 w-full rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-700 disabled:opacity-60 sm:w-auto"
          disabled={saving}
        >
          {saving ? "Salvando..." : "Salvar produto"}
        </button>
      </div>
    </form>
  );
}
