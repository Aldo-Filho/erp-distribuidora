# Informações de criação e atualização dos registros

Data: 29/10/2026. Escopo confirmado na conversa: incluir Funcionários e Cargos existentes e adotar o padrão nos próximos módulos. Este documento registra o desenho para revisão; nenhuma tela foi alterada.

## Objetivo

Permitir consultar as datas disponíveis de um registro em uma área discreta de Informações do registro, sem confundir esses metadados com auditoria ou histórico de alterações.

## Acesso

- Quem tem permissão para consultar o registro também pode consultar suas datas.
- Não criar uma permissão específica nem exigir cargo de administrador para esses metadados.
- Preservar as permissões atuais das telas existentes; não liberar acesso a Funcionários ou Cargos a quem não possui autorização para esses registros.
- Auditoria permanece separada e restrita a security.audit.read.

## Apresentação

| Campo disponível | Rótulo | Comportamento |
| --- | --- | --- |
| createdAt | Criado em | Mostrar data e hora; quando o valor não estiver registrado, informar Data não registrada. |
| updatedAt | Última atualização | Mostrar data e hora; quando nulo, informar Sem atualização registrada. |

- Informações somente de leitura, provenientes da API; não enviar esses campos ao cadastrar ou editar.
- Mostrar o bloco nos detalhes do registro, sem acrescentar colunas às listagens por padrão.
- Campos inexistentes no contrato/schema não geram uma linha vazia: Cargo possui apenas createdAt, portanto exibe apenas Criado em.
- Distinguir campo suportado com valor nulo de campo não suportado. O componente futuro recebe essa indicação explicitamente, sem tentar inferi-la apenas do valor.
- Formato brasileiro com data e hora; formato e tratamento de ausência serão compartilhados. Não exibir Invalid Date nem substituir ausência pelo horário atual.
- Timestamps atuais sem offset, como os LocalDateTime de User e Role, mantêm seus valores de calendário/hora, sem acrescentar Z ou presumir UTC. Instantes com offset preservam seu significado e seguem a convenção de exibição adotada pela aplicação. Não alterar o contrato temporal do backend nesta mudança.

## Alcance imediato

- Funcionários: disponibilizar consulta das informações a partir da linha do funcionário em um detalhe somente de leitura, sem obrigar o usuário a iniciar uma edição. Exibir createdAt e updatedAt do funcionário selecionado.
- Cargos: incluir o bloco junto aos detalhes do cargo selecionado, fora dos campos editáveis; exibir somente createdAt. Não mostrar datas no formulário de Novo cargo antes de persistir.
- Reutilizar o padrão nas futuras telas de clientes, fornecedores, produtos, preços, pedidos e compras conforme os campos realmente disponíveis.
- Usar o dado do registro consultado/recarregado; não conservar uma data antiga no estado do formulário depois de salvar.

## Semântica e limites

- updatedAt informa a última atualização registrada pela persistência; não comprova mudança específica de campo, autoria ou alteração comercial significativa.
- updatedAt nulo não deve ser traduzido como Nunca alterado: registros históricos ou caminhos antigos podem não ter registrado a data.
- Alteração de um endereço, item ou concessão não garante atualização do timestamp da entidade principal. Não modificar callbacks ou propagar timestamps como efeito colateral desta exibição.
- receivedAt, occurredAt, issuedAt, paidAt, authenticatedAt e lastActivityAt continuam sendo datas de eventos distintos; não serão renomeadas automaticamente para Criado em/Última atualização.
- Não criar tela central de todas as tabelas, histórico de mudanças, novos campos, migrations, DTOs de resposta de negócio ou alterações de permissão nesta entrega.

## Base verificada

User e Customer possuem createdAt/updatedAt; Role possui somente createdAt. A V1 confirma que há tabelas sem esses campos. O frontend possui datas de sessão/auditoria, mas não apresenta ainda um bloco compartilhado de metadados de cadastro. UsersPage seleciona atualmente apenas os campos editáveis; RolesPage mantém a seleção do cargo e os dados carregados.

## Critérios para a implementação futura

- Funcionário com updatedAt preenchido mostra as duas datas; com valor nulo mostra Sem atualização registrada.
- Cargo mostra apenas Criado em; cadastro novo ainda não persistido não ganha datas fictícias.
- Consulta dos metadados usa a mesma autorização do registro e não concede acesso à auditoria.
- Datas não aparecem como campos editáveis nem são enviadas no PATCH.
- Após salvar/recarregar, o bloco usa o dado atualizado da API; ausência e valor inválido têm apresentação controlada.
- Interface permanece compacta, responsiva e sem textos auxiliares genéricos.

AGENTS.md permanece prioritário. Não implementar, criar/executar testes, build ou lint, aplicar migrations ou fazer commits apenas por este documento existir. Este chat permanece dedicado ao planejamento; execução futura precisa respeitar o escopo solicitado pelo usuário.
