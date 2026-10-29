import { useEffect, useState } from "react";
import { access } from "../../services/access";
import Modal from "../../components/feedback/Modal";
import ErrorAlert from "../../components/feedback/ErrorAlert";
import RecordMetadata from "../../components/record/RecordMetadata";

export default function UserDetailsModal({ userId, onClose }) {
  const [state, setState] = useState({ loading: true });
  const [revision, setRevision] = useState(0);
  useEffect(() => {
    const controller = new AbortController();
    let live = true;
    setState({ loading: true });
    access.user(userId, { signal: controller.signal }).then(
      (user) => {
        if (live) setState({ user });
      },
      (error) => {
        if (live) setState({ error: error.message });
      },
    );
    return () => {
      live = false;
      controller.abort();
    };
  }, [userId, revision]);
  return (
    <Modal title="Detalhes do funcionário" onClose={onClose}>
      <ErrorAlert message={state.error} retry={() => setRevision((value) => value + 1)} />
      {state.loading ? (
        <p role="status">Carregando funcionário…</p>
      ) : state.user ? (
        <>
          <dl className="account-details">
            <dt>Nome</dt>
            <dd>{state.user.name}</dd>
            <dt>E-mail</dt>
            <dd>{state.user.email}</dd>
            <dt>Cargo</dt>
            <dd>{state.user.role?.name || "—"}</dd>
            <dt>Situação</dt>
            <dd>{state.user.active ? "Ativo" : "Inativo"}</dd>
          </dl>
          <RecordMetadata record={state.user} />
        </>
      ) : (
        !state.error && <p>Funcionário não encontrado.</p>
      )}
    </Modal>
  );
}
