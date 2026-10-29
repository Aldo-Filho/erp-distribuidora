# Plano de implementação: conclusão do módulo user

> **Status:** plano aprovado pelo usuário e implementado em 22/10/2026 com `superpowers:executing-plans`. Revisão estática concluída; evidências e limites de verificação em `2026-10-22-user-module-progress.md`.

**Objetivo:** concluir a API de administração de usuários, perfis, permissões e vínculos entre perfis e permissões nas pastas existentes de `user`.

**Arquitetura:** seguir `customer` para CRUDs de recursos com ID simples e `supplier/brand` para a associação com chave composta. Controllers recebem e validam entradas; services concentram regras e escritas; repositories acessam dados. Retornar entidades, preservando o tratamento centralizado de erros e a ocultação de `passwordHash`.

**Tecnologias:** Java 21, Spring Boot, Spring Data JPA, Bean Validation, PostgreSQL e o PasswordEncoder Argon2id já configurado.

**Especificação de referência:** solicitação deste chat, `AGENTS.md`, entidades de `user`, schema atual em `backend/src/main/resources/db/migration/V1__create-table-erp.sql`, `customer` e `supplier/brand`. As decisões abaixo foram aprovadas com a autorização para implementar este plano.

## Restrições globais

- Preservar alterações locais, inclusive as de compras, pedidos, estoque, preços e migrations. Conferir novamente o Git antes da execução, pois há trabalho simultâneo no projeto.
- Não criar/executar testes automatizados, build ou lint sem solicitação explícita. A verificação prevista aqui é revisão direta do código, contratos e schema; não comprova funcionamento em execução.
- Não aplicar migrations, recriar banco, executar formatação global ou fazer commit/push como parte deste plano.
- Seguir injeção por campos com `@Autowired`, métodos `search`, `findOne`, `register`, `update`, `delete`, records e `Specification.allOf()` com variável `spec`.
- Não adicionar DTOs de resposta, abstrações genéricas ou dependências além das que já foram declaradas para Argon2id.
- Não criar frontend nem alterar contratos de outros módulos.
- Não alterar V1/V2 para atender ao CRUD: as tabelas necessárias já existem.
- Preservar inglês em identificadores e mensagens de exceção, terminadas com ponto final.

## Estado atual observado

- `User`, `Role`, `Permission`, `RolePermission` e `RolePermissionId` estão mapeados; o vínculo usa `@EmbeddedId` e `@MapsId`.
- Existem `RequestUserDTO`, o construtor de cadastro de `User` e `UserService.register(RequestUserDTO)` retornando `User`.
- O cadastro verifica duplicidade de e-mail, exige senha preenchida, busca o perfil e salva somente o hash.
- `PasswordConfig` declara Argon2id com salt de 16 bytes, hash de 32 bytes, paralelismo 1, memória de 65536 KiB e 3 iterações. O `pom.xml` declara Spring Security Crypto e Bouncy Castle. Não houve build nem validação de desempenho/dependências.
- `RoleRepository` já usa a entidade correta. `PermissionRepository` precisa trocar `PermissionRepository` por `Permission` como tipo da entidade.
- `RolePermissionRepository` precisa trocar `Long` por `RolePermissionId` como tipo da chave.
- Faltam controllers, filtros, PATCHs e os services de perfis, permissões e seus vínculos.

## Escopo e decisões propostas

### Usuários

- Cadastro mantém `name`, `email`, `password`, `roleId` obrigatórios e `active` opcional, com padrão `true` sem sobrescrever `false`.
- PATCH aceita `name`, `email`, `password`, `roleId` e `active`. Omitido ou `null` preserva o valor atual.
- Rejeitar nome, e-mail ou senha informados vazios/somente espaços. Validar formato do e-mail com `@Email` no PATCH e `@Valid` no controller, sem tornar os demais campos obrigatórios.
- Nunca aparar espaços ou normalizar a senha. Codificar somente quando uma nova senha for informada; não codificar novamente o hash existente.
- Preservar a comparação de e-mail atual, por igualdade, de acordo com a unicidade do schema. Não introduzir normalização de caixa/espaços nem prometer unicidade sem distinção de maiúsculas sem uma decisão adicional.
- PATCH verifica duplicidade com o e-mail final e `existsByEmailAndIdNot`, permitindo manter o próprio e-mail.
- Perfil é obrigatório: ID ausente no cadastro gera `IllegalArgumentException`; ID informado e inexistente gera `ResourceNotFoundException`. No PATCH, `roleId = null` preserva a associação.
- Não bloquear automaticamente um perfil inativo: esse comportamento ainda não existe no contrato. `active` será administrado como estado, sem efeitos implícitos em outros registros.
- Não aceitar alterações em `id`, `createdAt`, `updatedAt` ou `lastLoginAt` pelos DTOs. `lastLoginAt` fica reservado para a futura autenticação.
- Não retornar senha ou hash no JSON; preservar `@JsonIgnore` em `User.passwordHash`.

### Perfis

- Cadastro: `name` obrigatório, `description` opcional, `active` opcional com padrão `true`.
- PATCH: `name`, `description`, `active`; omissão/null preserva. Nome vazio é inválido; descrição vazia/somente espaços limpa para `null`.
- Não impor unicidade ao nome: `roles.name` não tem essa constraint.
- Preservar geração de `createdAt` no callback da entidade e permitir atualização explícita de `active`.
- Inativar um perfil não inativa usuários nem apaga vínculos.

### Permissões

- Cadastro: `module`, `action`, `permissionKey` obrigatórios e preenchidos. PATCH permite os mesmos campos opcionalmente e rejeita vazio quando informado.
- `permissionKey` é explícita, sem derivação automática de módulo/ação; unicidade por igualdade, conforme o banco.
- Não impor unicidade adicional para o par módulo/ação nem criar enums de módulos e ações sem requisitos concretos.
- Preservar vínculos existentes quando uma permissão for editada: eles referenciam seu ID. A futura autorização deverá considerar essa possibilidade.

### Vínculos de perfil e permissão

- Consultar, vincular e desvincular permissões por perfil. Não criar PATCH: a associação não possui atributos próprios; trocar um vínculo exige remover e cadastrar.
- Usar `RequestRolePermissionDTO(@NotNull Long permissionId)` e receber `roleId` na URL.
- Validar existência do perfil na listagem e de ambas as entidades no cadastro. Não filtrar silenciosamente perfis inativos.
- Rejeitar o par já existente com `IllegalArgumentException`; usar `RolePermissionId` em `existsById`, `findById` e exclusão.
- Excluir vínculo não exclui perfil, permissão ou usuário. Não adicionar cascata de remoção nas associações `@ManyToOne`.

### Exclusões e referências externas

- DELETE é exclusão física do recurso; inativação de usuário/perfil ocorre por PATCH de `active`.
- Bloquear exclusão de perfil com usuários ou vínculos de permissão e de permissão com vínculos. Usar consultas `existsBy...` antes da exclusão e mensagens específicas em inglês.
- Usuários referenciados por pedidos, compras, recebimentos, movimentações ou pagamentos permanecem protegidos pelas FKs. Não apagar históricos, remover referências nem modificar outros módulos para permitir a exclusão.
- Conflitos residuais de banco, inclusive concorrência entre verificação e escrita, seguem o `GlobalExceptionHandler`: `DataIntegrityViolationException` retorna 409. Não converter indiscriminadamente falhas de integridade em duplicidade de e-mail.
- Recursos ausentes em escritas usam `ResourceNotFoundException`. Violações de negócio detectadas no service usam `IllegalArgumentException`, conforme o padrão existente.

### Limite em relação à autenticação

- Este plano conclui a administração dos cadastros. Não implementa login/logout, JWT, sessão, recuperação de senha, políticas de autorização, proteção de rotas ou regras de administrador.
- Cadastrar perfis e permissões não aplica controle de acesso. Concluir autenticação/autorização exige uma etapa própria antes de disponibilizar esses endpoints em ambiente compartilhado.

## Contratos HTTP

| Recurso | Endpoints | Respostas |
| --- | --- | --- |
| Usuários | `GET /user/search`, `GET /user/{id}`, `POST /user`, `PATCH /user/{id}`, `DELETE /user/{id}` | 200 em busca/edição; 201 em cadastro; 204 em exclusão e consulta individual ausente |
| Perfis | `GET /role/search`, `GET /role/{id}`, `POST /role`, `PATCH /role/{id}`, `DELETE /role/{id}` | Mesmo padrão de usuários |
| Permissões | `GET /permission/search`, `GET /permission/{id}`, `POST /permission`, `PATCH /permission/{id}`, `DELETE /permission/{id}` | Mesmo padrão de usuários |
| Vínculos | `GET /role/{roleId}/permissions`, `POST /role/{roleId}/permissions`, `DELETE /role/{roleId}/permissions/{permissionId}` | 200 com lista, 201 com vínculo, 204 em exclusão; vínculo ausente na exclusão gera 404 |

`findOne` dos três CRUDs simples usa repository no controller, como `customer`. Listagens retornam listas sem paginação, conforme a referência atual. O vínculo segue o formato já utilizado em `supplier/brand`.

## Arquivos e responsabilidades

Todos os caminhos Java abaixo são relativos a `backend/src/main/java/com/pi/erp/user/`.

| Pasta | Criar | Revisar/alterar |
| --- | --- | --- |
| raiz | `UserController.java`, `UserFilter.java`, `PatchUserDTO.java` | `UserService.java`, `UserRepository.java`; preservar `User.java` e `RequestUserDTO.java`, ajustando apenas se necessário para o contrato descrito |
| `config/role` | `RoleController.java`, `RoleService.java`, `RoleFilter.java`, `RequestRoleDTO.java`, `PatchRoleDTO.java` | `Role.java`, `RoleRepository.java` |
| `config/permission` | `PermissionController.java`, `PermissionService.java`, `PermissionFilter.java`, `RequestPermissionDTO.java`, `PatchPermissionDTO.java` | `Permission.java`, `PermissionRepository.java` |
| `config` | `RolePermissionController.java`, `RolePermissionService.java`, `RequestRolePermissionDTO.java` | `RolePermissionRepository.java`; preservar `RolePermission.java`, `RolePermissionId.java` e `PasswordConfig.java` |

## Focos da revisão

1. PATCH de senha omitida preserva hash; senha preenchida passa pelo encoder; senha vazia não chega à persistência. A implementação da biblioteca gera o salt, sem construção manual.
2. `false` explícito não vira `true`; textos opcionais e obrigatórios têm tratamentos distintos.
3. Duplicidade em PATCH exclui o próprio recurso e usa valores finais. Constraints do banco continuam protegendo concorrência.
4. Tipos genéricos de repositories e atributos dos `@MapsId` correspondem às entidades/chave composta.
5. Exclusão nunca remove registros comerciais ou recursos compartilhados por cascata. JSON nunca expõe `passwordHash`.

## Tarefa 1: corrigir os contratos de persistência

**Arquivos:** `UserRepository.java`, `config/role/RoleRepository.java`, `config/permission/PermissionRepository.java`, `config/RolePermissionRepository.java`.

**Interfaces produzidas:** repositories com `JpaRepository` e `JpaSpecificationExecutor` para `User`, `Role`, `Permission`; vínculo com `JpaRepository<RolePermission, RolePermissionId>`.

- [x] Corrigir os dois tipos genéricos identificados no estado atual, mantendo formatação e nomes da referência.
- [x] Manter `UserRepository.existsByEmail(String email)` e adicionar `existsByEmailAndIdNot(String email, Long id)` e `existsByRoleId(Long roleId)`.
- [x] Adicionar `PermissionRepository.existsByPermissionKey(String permissionKey)` e `existsByPermissionKeyAndIdNot(String permissionKey, Long id)`.
- [x] Adicionar `RolePermissionRepository.findAllByRoleId(Long roleId)`, retornando `List<RolePermission>`, e `existsByRoleId(Long roleId)` / `existsByPermissionId(Long permissionId)`.
- [x] Revisar diretamente os caminhos derivados (`role.id`, `permission.id`) contra os atributos das entidades e conferir o tipo da chave em todas as consultas do vínculo.

## Tarefa 2: concluir o CRUD de perfis

**Arquivos:** os seis arquivos de `config/role` listados na tabela e `RolePermissionRepository.java`/`UserRepository.java` da tarefa 1.

**Interfaces produzidas:** `RoleService.search(RoleFilter filter): List<Role>`, `register(RequestRoleDTO data): Role`, `update(Long id, PatchRoleDTO data): Role`, `delete(Long id): void`.

- [x] Criar os records com os campos/regras definidos; `RoleFilter` contém `String name` e `Boolean active`.
- [x] Implementar busca por nome parcial sem distinção de caixa e status exato; ignorar texto nulo/vazio e filtros ausentes.
- [x] Implementar cadastro, construtor `Role(RequestRoleDTO data)`, PATCH e exclusão. Buscar e validar antes de setters; excluir por entidade. No DELETE, impedir referências em usuários e vínculos.
- [x] Criar `RoleController` conforme os contratos HTTP; cadastro usa `@Valid`. PATCH sem constraints próprias não exige `@Valid`.
- [x] Revisar `active` nulo/false, descrição limpa, nome vazio, perfil ausente e exclusão com referências. Confirmar que não foi criada unicidade de nome.

## Tarefa 3: concluir o CRUD de permissões

**Arquivos:** os seis arquivos de `config/permission` listados na tabela e repository do vínculo.

**Interfaces produzidas:** `PermissionService.search(PermissionFilter filter): List<Permission>`, `register(RequestPermissionDTO data): Permission`, `update(Long id, PatchPermissionDTO data): Permission`, `delete(Long id): void`.

- [x] Criar records com os três campos textuais; cadastro usa `@NotBlank`, PATCH permanece opcional e valida vazio no service.
- [x] Definir `PermissionFilter(String module, String action, String permissionKey)`; buscar os três por igualdade, ignorando ausentes/vazios, com `Specification.allOf()`.
- [x] Implementar `Permission(RequestPermissionDTO data)`, cadastro e PATCH com checagem da chave única e exclusão do próprio ID na edição. Validar todos os valores finais antes de alterar a entidade.
- [x] Implementar DELETE bloqueado quando houver vínculos, sem remoções automáticas.
- [x] Criar `PermissionController` conforme os contratos HTTP e revisar chave repetida, chave mantida no próprio registro, PATCH vazio, filtros e ausência de recurso.

## Tarefa 4: administrar permissões dos perfis

**Arquivos:** três novos arquivos de `config` e `RolePermissionRepository.java`.

**Interfaces produzidas:** `RolePermissionService.search(Long roleId): List<RolePermission>`, `register(Long roleId, RequestRolePermissionDTO data): RolePermission`, `delete(Long roleId, Long permissionId): void`.

- [x] Criar o request com `@NotNull Long permissionId`, controller com as três rotas e injeção por campos.
- [x] Implementar auxiliares privados de busca por perfil/permissão, com tratamento explícito de ID obrigatório ausente e recurso inexistente.
- [x] Implementar listagem por perfil e cadastro com `existsById(new RolePermissionId(...))`; manter construtor existente do vínculo.
- [x] Implementar exclusão buscando o vínculo pela chave composta e chamando `repository.delete(entity)`.
- [x] Usar `@Transactional` nas escritas coordenadas, seguindo `SupplierBrandService`; não inventar PATCH ou ID artificial.
- [x] Revisar duplicidade, perfil inexistente, permissão inexistente, lista vazia e vínculo ausente na exclusão. Confirmar ausência de cascata para as entidades associadas.

## Tarefa 5: concluir o CRUD de usuários e a edição de senha

**Arquivos:** `UserController.java`, `UserFilter.java`, `PatchUserDTO.java`, `UserService.java`, `UserRepository.java`; referência a `User.java`, `RequestUserDTO.java` e `config/PasswordConfig.java`.

**Interfaces produzidas:** `UserService.search(UserFilter filter): List<User>`, `register(RequestUserDTO data): User`, `update(Long id, PatchUserDTO data): User`, `delete(Long id): void`.

- [x] Criar `UserFilter(String name, String email, Long roleId, Boolean active)`: nome/e-mail parciais sem distinção de caixa; perfil/status exatos. Ignorar filtros textuais ausentes/vazios.
- [x] Criar `PatchUserDTO(String name, @Email String email, String password, Long roleId, Boolean active)`, com um campo por linha e sem constraints que exijam campos omitidos.
- [x] Preservar o cadastro já implementado e organizar os métodos públicos na ordem `search`, `register`, `update`, `delete`, seguidos dos auxiliares privados.
- [x] Implementar PATCH: carregar usuário, resolver valores finais e perfil, validar textos e duplicidade, codificar nova senha apenas quando fornecida, depois aplicar setters e salvar. Preservar senha exatamente como recebida.
- [x] Implementar DELETE por entidade; deixar FKs e o handler central protegerem o histórico comercial. Não criar dependências com todos os módulos apenas para replicar a verificação do banco.
- [x] Criar `UserController`; usar `@RequestBody @Valid` em POST e PATCH. Manter retorno de entidades e `findOne` direto no repository.
- [x] Revisar omissão/null de senha, nova senha, senha em branco, e-mail inválido/repetido, perfil inexistente, `active = false`, datas não editáveis e `@JsonIgnore` do hash. Confirmar que o encoder é único e que não existe trim da senha.

## Tarefa 6: revisão final do módulo e registro da entrega

**Arquivos:** todos os criados/alterados no módulo; novo `docs/superpowers/plans/2026-10-22-user-module-progress.md`.

- [x] Comparar controllers, services, records, filtros e repositories com `customer` e o vínculo com `supplier/brand`; justificar somente diferenças necessárias ao domínio.
- [x] Revisar correspondência com colunas, nulabilidade, unicidade e FKs do schema atual, sem alterar migrations.
- [x] Conferir erros 400/404/409, contrato de 204 em `findOne` ausente e ausência de referências circulares ou hash no JSON pelo exame dos mapeamentos/anotações.
- [x] Conferir apenas os trechos alterados e os arquivos novos, preservando o trabalho simultâneo e evitando formatação global.
- [x] Registrar no documento de progresso o que foi implementado e as limitações: sem execução, testes, build, lint, validação de dependências, medição de Argon2 ou controle de acesso.

## Critério de conclusão

As três APIs CRUD e a administração de vínculos estão implementadas conforme os contratos, com repositories corretos, PATCH parcial, Argon2id na criação/troca de senha, proteção do hash no JSON e preservação das referências externas. O relatório final deve distinguir implementação revisada de validação em execução. Autenticação e autorização continuam sendo uma entrega separada.
