# Auditoria de acesso legível

Proposta aprovada pelo usuário nesta conversa: exibir responsável com nome e ID, ações e resultados em português, distinguir responsável e alvo e preservar os nomes mesmo após renomeação/exclusão. O acesso continua condicionado a security.audit.read.

Escolha: gravar nomes e tipos junto ao evento. Consultar apenas o cadastro atual seria mais simples, mas mudaria a apresentação do histórico quando um cadastro fosse editado. Novos eventos recebem os nomes disponíveis naquele instante; eventos anteriores continuam identificados pelos IDs e indicam nome não registrado. Não inventar nomes históricos nem atribuir ações anônimas ao usuário afetado.

Login autenticado, aceite de convite e redefinição com token válido identificam o responsável; envio assíncrono e bootstrap são operações do sistema. Falhas sem identidade comprovada são não identificadas. Os alvos distinguem funcionário, cargo, convite e política. Exclusões preservam o nome antes de remover o cadastro, na mesma transação.

Os códigos internos e os contratos existentes permanecem; a consulta adiciona os nomes/tipos. A tela usa rótulos e filtros em português. Uma migration aditiva acrescenta os campos, sem alterar V1–V4, credenciais ou contas. Não inclui informações de criação/atualização de outros módulos.

Revisão direta apenas, sem criar/executar testes, build, lint, aplicação de migrations, envio de e-mail ou startup, conforme AGENTS.md. Sem commits nem descarte das mudanças existentes.
