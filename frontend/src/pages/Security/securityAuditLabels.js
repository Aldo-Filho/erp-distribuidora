export const auditActions = {
  ACCESS_DENIED: "Acesso negado",
  ACCESS_EMAIL: "Envio de e-mail de acesso",
  ACCESS_OPERATION_REJECTED: "Operação de acesso recusada",
  ADMINISTRATOR_BOOTSTRAPPED: "Administrador inicial criado",
  INVITATION_ACCEPTANCE_FAILED: "Falha ao aceitar convite",
  INVITATION_ACCEPTED: "Convite aceito",
  INVITATION_CANCELED: "Convite cancelado",
  INVITATION_CREATED: "Convite criado",
  INVITATION_RESENT: "Reenvio de convite solicitado",
  LOGIN: "Entrada no sistema",
  LOGIN_FAILED: "Falha ao entrar no sistema",
  LOGOUT: "Saída do sistema",
  PASSWORD_CHANGED: "Senha alterada",
  PASSWORD_CHANGE_FAILED: "Falha ao alterar senha",
  PASSWORD_RECOVERY_REQUESTED: "Recuperação de senha solicitada",
  PASSWORD_RESET: "Senha redefinida",
  PASSWORD_RESET_FAILED: "Falha ao redefinir senha",
  ROLE_CREATED: "Cargo criado",
  ROLE_DELETED: "Cargo excluído",
  ROLE_PERMISSION_GRANTED: "Permissão concedida ao cargo",
  ROLE_PERMISSION_REVOKED: "Permissão removida do cargo",
  ROLE_UPDATED: "Cargo atualizado",
  SESSION_LIMIT_REACHED: "Limite de sessões atingido",
  SESSION_POLICY_UPDATED: "Política de acesso atualizada",
  SESSION_REVOKED: "Sessão encerrada por solicitação",
  USER_DELETED: "Funcionário excluído",
  USER_UPDATED: "Funcionário atualizado",
};

export const auditOutcomes = {
  SUCCESS: "Sucesso",
  FAILURE: "Falha",
  CONFLICT: "Conflito",
  ACCEPTED: "Solicitação aceita",
  SUBMITTED: "Aceito pelo servidor de e-mail",
};

const legacyTargetTypes = {
  LOGIN: "USER",
  LOGOUT: "USER",
  SESSION_LIMIT_REACHED: "USER",
  SESSION_REVOKED: "USER",
  ADMINISTRATOR_BOOTSTRAPPED: "USER",
  USER_UPDATED: "USER",
  USER_DELETED: "USER",
  INVITATION_ACCEPTED: "USER",
  PASSWORD_RECOVERY_REQUESTED: "USER",
  PASSWORD_RESET: "USER",
  PASSWORD_CHANGED: "USER",
  ROLE_CREATED: "ROLE",
  ROLE_UPDATED: "ROLE",
  ROLE_DELETED: "ROLE",
  ROLE_PERMISSION_GRANTED: "ROLE",
  ROLE_PERMISSION_REVOKED: "ROLE",
  INVITATION_CREATED: "INVITATION",
  INVITATION_RESENT: "INVITATION",
  INVITATION_CANCELED: "INVITATION",
  ACCESS_EMAIL: "INVITATION",
  SESSION_POLICY_UPDATED: "POLICY",
};

const targetLabels = {
  USER: "Funcionário",
  ROLE: "Cargo",
  INVITATION: "Convite",
  POLICY: "Política de acesso",
};

export function auditActor(event) {
  if (event.actorId != null) {
    return event.actorName || "Nome não registrado";
  }
  if (
    event.actorType === "SYSTEM" ||
    (!event.actorType && ["ACCESS_EMAIL", "ADMINISTRATOR_BOOTSTRAPPED"].includes(event.action))
  ) {
    return "Sistema";
  }
  return "Não identificado";
}

export function auditTarget(event) {
  if (!event.target) return null;
  const type = event.targetType || legacyTargetTypes[event.action];
  return {
    label: targetLabels[type] || "Registro",
    name: type === "POLICY" ? null : event.targetName || "Nome não registrado",
  };
}

export function auditChanges(event) {
  if (event.action !== "SESSION_POLICY_UPDATED" || !event.changes) return "";
  const labels = {
    idleMinutes: ["Inatividade", "min"],
    absoluteMinutes: ["Duração máxima", "min"],
    maxSessions: ["Limite de sessões", ""],
  };
  return event.changes
    .split(";")
    .flatMap((entry) => {
      const [key, value] = entry.split("=");
      const label = labels[key];
      if (!label || !/^\d+$/.test(value)) return [];
      return [`${label[0]}: ${value}${label[1] ? " " + label[1] : ""}`];
    })
    .join(" · ");
}
