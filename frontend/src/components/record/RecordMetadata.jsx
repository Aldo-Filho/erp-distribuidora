import { formatRecordDateTime } from "../../utils/dateTime";

const labels = { createdAt: "Criado em", updatedAt: "Última atualização" };
export default function RecordMetadata({ record, fields = ["createdAt", "updatedAt"] }) {
  return (
    <section className="record-metadata">
      <h3>Informações do registro</h3>
      <dl>
        {fields.map((field) => (
          <div key={field}>
            <dt>{labels[field]}</dt>
            <dd>
              {record?.[field] == null
                ? field === "updatedAt"
                  ? "Sem atualização registrada"
                  : "Data não registrada"
                : formatRecordDateTime(record[field])}
            </dd>
          </div>
        ))}
      </dl>
    </section>
  );
}
