import FilterSelect from "../../components/feedback/FilterSelect";
export default function FinanceSelect({ label, value, onChange, values, empty = "Todos", ...props }) {
  return <label>{label}<FilterSelect value={value} onChange={onChange} {...props}>
    {empty !== null && <option value="">{empty}</option>}
    {Object.entries(values).map(([id, text]) => <option key={id} value={id}>{text}</option>)}
  </FilterSelect></label>;
}
