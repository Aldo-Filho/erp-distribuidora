# Preferências do usuário para este projeto

Estas instruções se aplicam a todo o projeto, tanto ao front-end quanto ao backend, e devem ser respeitadas em novos chats neste diretório.

## Testes e economia de tokens

- Não criar nem executar testes automatizados por padrão, incluindo testes unitários, de integração e de ponta a ponta.
- Criar ou executar testes somente quando o usuário solicitar explicitamente. Uma solicitação de implementar ou corrigir algo não autoriza testes automaticamente.
- Priorizar a implementação solicitada e a revisão direta dos trechos alterados, economizando tokens e evitando verificações repetitivas ou desnecessárias.
- Não perguntar a cada tarefa se deve executar testes; seguir esta preferência.
- Ao informar o resultado, não afirmar que testes passaram quando não foram executados. Se relevante, mencionar brevemente que não foram executados conforme a preferência do usuário.

## Padrão de referência para o backend: módulo customer

- Usar `backend/src/main/java/com/pi/erp/customer` e sua subpasta `address` como referência para organizar, padronizar e implementar os outros módulos do backend.
- Antes de alterar um módulo, ler os arquivos correspondentes de `customer`, o código atual do módulo de destino e suas regras de negócio. A referência é a organização e o estilo; não copiar automaticamente regras específicas de clientes.
- Manter os módulos o mais próximo possível de `customer` na estrutura, nos nomes, na apresentação do código e na distribuição de responsabilidades entre controller, service, repository, DTO e entidade. Não introduzir um estilo alternativo apenas por preferência técnica.
- O padrão não exige implementações idênticas: adaptar o código às regras de negócio, exceções, limitações e necessidades concretas de cada módulo e entidade. Cada diferença deve ter uma razão no domínio, no contrato ou na operação; evitar tanto a uniformização forçada quanto desvios sem necessidade. Não reproduzir inconsistências incidentais da referência.
- Aplicar o padrão ao escopo solicitado. Não reescrever outros módulos, alterar contratos públicos ou criar abstrações genéricas apenas para uniformizar o código.

### Organização e nomes

- Organizar pacotes por funcionalidade, com subpastas para partes relacionadas, como `customer/address`, evitando pastas globais que misturem controllers e services de todos os módulos.
- Seguir os nomes `<Entity>`, `<Entity>Controller`, `<Entity>Service`, `<Entity>Repository`, `<Entity>Filter`, `Request<Entity>DTO` e `Patch<Entity>DTO`, quando cada arquivo for necessário.
- Usar inglês nos nomes de classes, métodos, atributos, variáveis e mensagens de exceção. Classes usam PascalCase; métodos e variáveis usam camelCase; pacotes usam letras minúsculas.
- Usar `data` para o DTO recebido, `filter` para filtros e `id` para o identificador do recurso principal.
- Usar `repository` e `service` para as dependências principais. Nomear dependências de outras funcionalidades explicitamente, como `customerRepository`, `priceTableRepository` e `customerAddressService`.
- Nomear entidades locais pelo domínio, como `customer` e `address`; quando necessário distinguir o resultado salvo, usar `savedCustomer` ou o equivalente do módulo. Evitar nomes vagos como `obj`, `temp` e `result` quando um nome específico for possível.

### Formatação e sintaxe

- Respeitar `.editorconfig` e `.prettierrc.json`: quatro espaços em Java, sem tabulações, espaços finais ou alinhamento manual com espaços extras. Não executar formatação global como efeito colateral de uma tarefa localizada.
- Usar chaves em todos os blocos `if`, inclusive quando houver apenas uma instrução. Manter espaços regulares em `if (...)`, operadores e `} else if (...) {`.
- Separar métodos e blocos de responsabilidades diferentes com uma linha em branco. Não deixar dois blocos colados na mesma linha nem linhas vazias sem propósito antes de fechar métodos.
- Seguir a apresentação dos arquivos equivalentes de `customer` para assinaturas, parâmetros em múltiplas linhas, records e chamadas encadeadas, respeitando o limite de linha configurado.
- Manter um campo por linha nos records e um método por declaração nos repositórios. Usar imports explícitos nos controllers e services, seguindo o arquivo de referência, e remover imports sem uso.
- Preferir condições explícitas e fluxo simples. Usar ternários apenas para escolhas curtas e legíveis, como manter um valor atual ou converter texto vazio em `null`.

### Controllers e endpoints

- Manter os controllers enxutos: receber e validar a entrada, delegar regras de negócio ao service e montar a resposta HTTP.
- Usar os nomes `search`, `findOne`, `register`, `update` e `delete`, conforme a operação. Evitar alternativas como `get`, `create`, `patch` e nomes redundantes como `deleteCustomer`.
- Nos novos CRUDs equivalentes, seguir o formato de `customer`: `GET /<resource>/search`, `GET /<resource>/{id}`, `POST /<resource>`, `PATCH /<resource>/{id}` e `DELETE /<resource>/{id}`. Preservar URLs existentes fora do escopo autorizado.
- Usar `ResponseEntity`, com `200` para consulta/edição, `201` para cadastro e `204` para exclusão. O `findOne` de referência retorna `204` quando ausente; não trocar silenciosamente esse contrato durante uma padronização.
- A busca simples por ID pode ficar no controller usando o repository, como em `CustomerController.findOne`. Regras de negócio e operações de escrita ficam no service.
- Usar `@RequestBody @Valid` nos DTOs de cadastro. No PATCH, adicionar `@Valid` quando houver constraints próprias de atualização, sem tornar obrigatórios todos os campos do cadastro.
- Seguir a decisão atual de retornar entidades, como no módulo `customer`. Não recriar DTOs de resposta de negócio sem solicitação ou aprovação do usuário; preservar `ResponseErrorDTO` e o tratamento centralizado de erros.

### Services, consultas e persistência

- Organizar os métodos públicos na ordem `search`, `register`, `update` e `delete`, seguidos dos métodos auxiliares privados.
- Seguir o padrão atual de injeção com `@Autowired` nos campos, sem migrar o estilo de injeção durante tarefas de padronização.
- Concentrar validações que dependam de estado persistido, combinações de campos ou coordenação das operações no service; manter o repository responsável pelo acesso aos dados. Validações declarativas de entrada e regras de negócio de um campo podem permanecer nos DTOs, conforme as orientações abaixo.
- Usar `Specification.allOf()` e a variável `spec` em buscas com filtros dinâmicos, compondo condições com `spec.and(...)` e retornando `repository.findAll(spec)`.
- Ignorar filtros ausentes; para filtros textuais, verificar `null` e `isBlank()`. Escolher igualdade ou busca parcial conforme a finalidade do campo, sem copiar indiscriminadamente os operadores de outro módulo.
- Buscar associações por repositories, com auxiliares como `findCustomer` e `findPriceTable`, usando `findById(...).orElseThrow(...)`. Não introduzir `EntityManager.find` apenas para fazer a mesma busca por ID.
- Em associações opcionais, permitir retorno `null` quando o ID estiver ausente, se essa for a regra do domínio. Para IDs obrigatórios ausentes, lançar `IllegalArgumentException`; para IDs informados e não encontrados, lançar `ResourceNotFoundException`.
- Validar antes de aplicar setters e salvar, sempre que possível. Retornar a entidade salva por `repository.save(...)`; na exclusão, buscar a entidade e chamar `repository.delete(entity)`.
- Usar `@Transactional` em operações compostas que precisam ser atômicas, como cadastro de entidade e endereço, e nas exclusões que dependam de cascata. Avaliar a necessidade pela operação; não atribuir atomicidade a vários saves independentes.
- Verificar duplicidade com métodos `existsBy...`; em atualizações, excluir o próprio ID com `...AndIdNot` e usar os valores finais resultantes do PATCH, combinando entrada e dados persistidos.
- Respeitar as constraints do banco e as regras específicas de cada módulo. Por exemplo, a unicidade de cliente + tipo de endereço não deve ser copiada para fornecedores sem essa regra.

### DTOs, validações e PATCH

- Usar records separados para cadastro, atualização parcial e filtros. Colocar constraints de cadastro como `@NotNull` e `@NotBlank` em `Request<Entity>DTO`; usar `@Valid` em objetos aninhados que precisem ser validados.
- Usar os DTOs simples de `customer` como ponto de partida. Não adicionar annotations de validação apenas por conveniência ou para tornar um módulo mais restritivo que a referência, sem uma necessidade concreta do domínio ou do contrato.
- Não usar `@Size` apenas para repetir o tamanho das colunas do banco. Limites físicos de armazenamento ficam no schema; um limite de tamanho que seja uma regra de negócio independente pode ser validado no DTO, com essa finalidade clara.
- Permitir annotations que expressem regras de negócio do campo, mesmo sem equivalente em `customer`, como `@PositiveOrZero` para impedir prazo de entrega negativo. Validações de formato, faixa ou precisão devem atender a uma regra concreta; não presumir essa regra apenas pelo tipo Java ou pela definição da coluna.
- Manter DTOs de PATCH e filtros sem constraints quando não houver regra específica que as justifique. Quando necessárias, usar constraints compatíveis com campos opcionais e `@Valid` no ponto de entrada, sem exigir valores omitidos. Nos filtros, avaliar a finalidade da busca antes de restringir os valores aceitos.
- Preferir records que apenas transportem os dados. Seguir `customer` ao tratar a normalização e a limpeza de textos no service; não criar construtores de DTO com essa lógica sem necessidade específica do módulo ou do contrato.
- No PATCH, campo omitido ou `null` preserva o valor atual, conforme o contrato de `customer`. Não tornar todos os campos obrigatórios por reutilizar o DTO de cadastro.
- Para textos obrigatórios enviados no PATCH, rejeitar vazio ou somente espaços com `IllegalArgumentException`, em vez de ignorar silenciosamente a tentativa de apagar.
- Para textos opcionais enviados no PATCH, permitir limpar: texto vazio ou somente espaços vira `null`; texto preenchido atualiza. Só aplicar essa regra a campos que possam ser nulos no domínio e no banco.
- Validar obrigatoriedade condicional conforme o domínio, tanto no cadastro quanto na edição, como nome fantasia obrigatório para PJ. Não transformar essa regra em obrigatoriedade universal para todos os tipos de pessoa.
- Usar `Boolean` no PATCH quando for necessário distinguir omissão de `false`. Flags opcionais de ação, como `removePriceTable`, também devem usar `Boolean`, sem `@NotNull`, e ser verificadas no service com `Boolean.TRUE.equals(...)`: omissão, `null` e `false` não executam a ação. Rejeitar combinações contraditórias, como remover uma associação e informar um novo ID ao mesmo tempo. Essa orientação não proíbe `boolean` primitivo em outros contextos.
- Para datas tipadas como `LocalDate`, validar regras de domínio quando houver valor, como impedir nascimento futuro no cadastro e na edição. Não usar `isBlank()` em datas nem presumir que o service recebe o texto original do JSON.
- Para remoção de associações, usar uma intenção explícita quando o contrato precisar diferenciar manter de remover, como `removePriceTable`; não remover uma associação apenas porque o ID foi omitido no PATCH.
- Usar `IllegalArgumentException` para dados inválidos e conflitos de regras tratados no service, e `ResourceNotFoundException` para recurso não encontrado, mantendo mensagens específicas, em inglês e com ponto final.
- Nas entidades, seguir os mapeamentos JPA e os callbacks de datas já usados em `customer`, respeitando o schema existente. Definir padrões como `active = true` sem sobrescrever um `false` recebido; não modificar migrations já aplicadas apenas por formatação.

### Limites da padronização

- A referência `customer` é para o backend. Não recriar telas nem código do frontend ao padronizar módulos do backend; preservar a estrutura, dependências e configurações do frontend até nova solicitação.
- Antes de modificar arquivos, conferir o estado do Git e preservar alterações existentes do usuário. Ao concluir, revisar diretamente os trechos alterados e informar o que mudou e o que não foi verificado.
- Na revisão final, comparar os arquivos alterados com seus equivalentes em `customer`, incluindo annotations, lógica nos DTOs e localização das validações. Distinguir diferenças justificadas pelo módulo de divergências de estilo. Não remover validações ou alterar entradas aceitas pela API silenciosamente apenas para igualar os módulos; preservar regras existentes e informar divergências cuja alteração esteja fora do escopo autorizado.
- As preferências de testes acima continuam prioritárias: padronizar, criar ou corrigir módulos não autoriza criar/executar testes nem executar build ou lint automaticamente.

## Padrão de organização e apresentação do frontend

Estas orientações se aplicam às próximas telas e aos ajustes de frontend solicitados. Antes de implementar, consultar os componentes compartilhados e as telas atuais de clientes e fornecedores. Reutilizar o padrão existente, adaptando o conteúdo às regras de cada módulo, sem reescrever outras telas fora do escopo.

### Identidade, estilos e componentes

- O nome do sistema é `JamsHub`. No menu, usar o ícone `Package` da biblioteca `lucide-react`, o nome ao lado e o subtítulo `Gestão para distribuidoras` abaixo. Preservar o favicon atual.
- Manter a paleta existente: verde petróleo `#102f39` no menu, verde `#087b77` nas ações principais, `#1c4b54` na opção selecionada e fundos claros nas páginas. Consultar `frontend/src/index.css` para bordas, textos, estados e demais cores.
- Usar `lucide-react` para os ícones. Não introduzir outra biblioteca de ícones ou componentes visuais sem necessidade aprovada.
- O Tailwind está disponível, e os componentes atuais também usam classes compartilhadas em `index.css`. Seguir esse padrão e reutilizar os estilos existentes; não migrar toda a estilização nem duplicar regras para uma tarefa localizada.
- Manter controles compactos, alinhados e funcionais, com bordas suaves, cantos arredondados e foco discreto e perceptível. Evitar subtítulos genéricos e elementos decorativos redundantes.

### Menu e estrutura das páginas

- Reutilizar `frontend/src/components/layout/AppLayout.jsx`. Manter o cabeçalho compacto do menu, as divisórias e a seleção com fundo arredondado, sem borda ao redor da aba selecionada.
- No desktop, o menu tem 244 px expandido e 78 px recolhido. O botão de recolher/expandir fica centralizado acima do bloco inferior com usuário, cargo e Sair; o botão Sair fica à direita do nome e do cargo.
- Até 780 px, manter o menu lateral sempre visível e recolhido, apenas com os ícones. Preservar nomes acessíveis e dicas das ações, além da rolagem interna quando a altura disponível for pequena.
- Sincronizar a largura do menu e o espaço ocupado pelo conteúdo durante o recolhimento. Os ícones não devem se deslocar lateralmente durante a animação. Respeitar a preferência de movimento reduzido.
- Não recriar a barra superior com o nome do sistema e o indicador Local. O conteúdo começa no cabeçalho da própria página.
- Nas páginas de listagem, reutilizar `frontend/src/components/layout/PageHeading.jsx`: título e, abaixo, quantidade em texto pequeno e discreto, com singular/plural corretos. Posicionar a ação principal, como Novo cliente, à direita quando houver espaço e permitir reorganização em telas pequenas.
- Em listas paginadas, usar o total informado pela API, como `totalElements`, e não o tamanho da página atual. Quando a contagem corresponder a filtros aplicados, indicar registros encontrados. Não apresentar zero como se fosse uma contagem carregada quando os dados ainda estiverem indisponíveis.

### Filtros, listas e ações

- Agrupar os filtros em um painel claro, com rótulos acima dos campos, alinhamento consistente e reorganização responsiva.
- Reutilizar `frontend/src/components/feedback/FilterSelect.jsx` nos dropdowns, incluindo seletores de formulário como Cargo. A lista aberta deve seguir o visual da página: fundo branco, borda suave, cantos arredondados e destaque verde discreto para a seleção. Preservar a validação de campos obrigatórios, estados desabilitados e operação por teclado.
- Manter as linhas das tabelas com o padrão de 88 px, conteúdo centralizado verticalmente e espaço para informações secundárias, como telefone, e-mail e WhatsApp. Evitar que quebras de texto ou dos botões gerem alturas diferentes entre os registros; preservar o acesso às informações.
- No desktop amplo, manter as ações das linhas compactas, sem caixas ou bordas individuais, com destaque suave no hover/foco. Separar discretamente a ação de ativar/inativar das ações de consulta e edição.
- Reutilizar `frontend/src/components/feedback/RowActions.jsx`. Até 1200 px, agrupar as ações em um menu de três pontos, mesmo quando a barra lateral ainda estiver expandida. Acima desse limite, exibir os botões diretamente.
- Nas larguras em que as ações estiverem agrupadas, manter a coluna de ações fixa à direita, acessível sem arrastar horizontalmente a tabela. O menu aberto deve caber na tela e não ser cortado pela rolagem da tabela.
- Preservar permissões, estados desabilitados, nomes acessíveis, navegação por teclado e as regras de cada ação ao mudar sua apresentação.

### Modais, confirmações e feedback

- Reutilizar `frontend/src/components/feedback/Modal.jsx` para janelas de cadastro, edição e demais diálogos.
- Usar `ConfirmationProvider` e `useConfirm`, em `frontend/src/components/feedback`, para as confirmações necessárias. Não usar janelas nativas como `window.confirm` ou `window.alert` para esses fluxos.
- Os modais seguem o visual do sistema: fundo branco, cantos arredondados, título claro, mensagem específica e botões como Cancelar, Ativar, Inativar, Excluir ou Descartar. Dar foco inicial a Cancelar nas confirmações e diferenciar visualmente ações destrutivas.
- Confirmar apenas as ações que já exigem essa intenção, como ativar/inativar, excluir ou descartar alterações não salvas; não adicionar confirmação obrigatória a todo cadastro ou salvamento comum.
- Cancelar, fechar ou pressionar Esc não deve enviar a operação. Confirmar deve executar a ação uma única vez. Preservar os dados do formulário ao cancelar um descarte e o funcionamento de modais sobrepostos.
- Abrir e fechar modais não pode deslocar o fundo. Preservar `scrollbar-gutter: stable`, a posição de rolagem, o bloqueio do fundo e o controle de foco sem rolagem involuntária. Garantir funcionamento também em telas estreitas.
- Os feedbacks de sucesso usam o padrão verde suave existente, com espaçamento interno equilibrado e margem inferior de 22 px, sem encostar na tabela ou no próximo bloco.
- As preferências de testes do início deste arquivo continuam valendo para o frontend: não criar/executar testes, build ou lint sem solicitação explícita. Revisar diretamente as alterações e informar com precisão o que foi verificado.
