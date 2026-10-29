# Clientes — desenho da próxima entrega

Data: 29/10/2026. Estado: desenho consolidado aprovado pelo usuário para elaboração do plano técnico. A aprovação abrange as propostas de interface e a mudança explícita de paginação descritas abaixo; não autoriza iniciar implementação.

## Objetivo

Entregar o módulo de Clientes no frontend atual, integrado à autenticação, às permissões e ao backend existente. O desenho de 10/10/2026 é referência histórica; a navegação por fragmentos, os componentes antigos e as exclusões de escopo daquele documento não devem ser reaplicados automaticamente.

## Decisões confirmadas

### Informações do registro

Adotar o padrão de [informações de criação e atualização](2026-10-29-record-metadata-design.md): datas somente de leitura nos detalhes, com a mesma permissão de consulta do cliente. updatedAt nulo é Sem atualização registrada; não implica ausência comprovada de alterações. Não confundir metadados com auditoria ou acrescentar datas inexistentes aos endereços.

O padrão também abrange Funcionários e Cargos existentes, em ajuste próprio; não anunciar esse ajuste como implementado.

### Situação do cliente e exclusão

- A primeira versão da tela de Clientes oferece somente ativação/inativação, com confirmação e a permissão customers.update.
- Não disponibilizar ação de exclusão física de clientes na interface. Esta decisão não autoriza remover ou alterar silenciosamente o endpoint DELETE existente no backend.
- Inativação mantém cadastro, associações e histórico; consultas e detalhes continuam disponíveis com customers.read. Aplicar as restrições de cliente ativo nas operações comerciais conforme o backend existente.
- A decisão se refere ao cadastro principal. O gerenciamento separado de endereços mantém suas regras e permissões próprias, incluindo exclusão quando autorizada; não confundir remoção de endereço com exclusão do cliente.

### Complemento opcional

- Complemento opcional no endereço inicial aninhado do cadastro e no gerenciamento posterior dos endereços do cliente.
- Cadastro com complemento ausente, vazio ou somente espaços grava null; texto preenchido mantém o valor informado, sem inventar conteúdo.
- No PATCH, omissão/null preserva e texto vazio/somente espaços limpa para null.
- Retirar a obrigatoriedade correspondente dos DTOs e adaptar a validação do service sem enfraquecer as regras dos demais campos.
- Propor nova migration que permita null em customer_addresses.complement; não editar V1 nem migrations aplicadas. Preservar valores históricos existentes.
- Mudança restrita aos endereços de clientes; não expandir automaticamente para depósitos ou outros módulos.

### Remoção da data de nascimento

- Permitir remover uma data de nascimento já cadastrada durante a edição do cliente.
- Proposta de contrato: acrescentar removeBirthDate do tipo Boolean ao PatchCustomerDTO, sem obrigatoriedade.
- Omissão/null/false da flag não remove a data; birthDate omitido/null preserva o valor atual.
- removeBirthDate=true remove a data; a combinação com birthDate preenchido é contraditória e deve ser rejeitada antes de aplicar setters.
- Data nova continua sujeita à regra existente que rejeita nascimento futuro. Não criar obrigatoriedade de nascimento para PF/PJ.
- Na interface, apagar uma data que existia produz intenção explícita de remoção; deixar vazio um campo originalmente sem data não cria uma ação de remoção. Cancelar a edição não altera o cadastro.
- A coluna de nascimento já permite null; esta alteração de contrato não exige tornar a coluna nullable novamente nem editar migrations existentes.

### Tabela de preços no formulário

- Incluir seleção opcional de tabela de preços já cadastrada no mesmo formulário de criação/edição do cliente.
- Disponibilizar Sem tabela de preços. Não criar tabela nem editar preços de produtos dentro do formulário de cliente.
- Criação: tabela escolhida informa priceTableId; Sem tabela omite a associação.
- Edição: preservar associação quando não alterada; nova escolha informa priceTableId; remoção explícita envia removePriceTable=true sem outro ID. Omitir/null no PATCH não remove associação.
- Não reenviar preço de produtos nem reprecificar pedidos antigos ao alterar a tabela do cliente.
- Usar a consulta existente /price-table/search e respeitar prices.read para carregar opções; cadastro sem tabela continua possível para quem possui customers.create sem acesso à consulta de preços. A interface não pode limpar um vínculo existente apenas porque não conseguiu carregar as opções.

## Base atual conferida

RequestCustomerDTO já possui priceTableId opcional e endereço aninhado; PatchCustomerDTO possui priceTableId e removePriceTable. CustomerService valida associação existente e rejeita remoção combinada com novo ID. A seleção não exige criar um novo mecanismo de precificação.

Os DTOs dos endereços ainda exigem complemento, o service rejeita seu esvaziamento e a coluna customer_addresses.complement é NOT NULL na V1. Tornar o campo opcional exige adequação explícita dessas três camadas em implementação futura.

O frontend atual tem navegação protegida e cliente de API com sessão/CSRF. Não existe tela operacional de Clientes nesta base. Preservar esses componentes e o padrão customer/address, sem restaurar arquivos antigos indiscriminadamente.

## Proposta de listagem e navegação

- Integrar Clientes à navegação e ao roteador atuais, com customers.read para consulta. Reutilizar sessão, CSRF, feedback e componentes do frontend atual, sem voltar à navegação por fragmentos antiga.
- Listagem compacta com Cliente (nome/razão social e fantasia quando houver), CPF/CNPJ, Contato, Situação e Ações. Sem cartões de indicadores ou colunas de timestamps por padrão.
- Ações: Detalhes, Editar, Endereços e Ativar/Desativar. Cadastro exige customers.create; edição e situação exigem customers.update; consulta exige customers.read. Não oferecer exclusão do cliente.
- Barra com Novo cliente, seletor de pesquisa Nome/CPF-CNPJ/Contato, texto de busca, filtros PF/PJ e situação e ação de limpar filtros. Busca e filtros devem combinar-se, mantendo os valores durante recarga e voltando à primeira página quando mudarem.
- Proposta de paginação: 25 registros por página, opções de 50/100 e limite máximo de 100 no servidor. Ordenação estável por nome/razão social e ID para desempate. Contagem refere-se aos resultados filtrados no backend, não ao tamanho da página.
- Nome pesquisa legalName ou tradeName; Contato pesquisa email, phone ou whatsapp. A API atual combina campos individuais com AND; enviar o mesmo texto em todos esses filtros não equivale à busca agrupada e não deve ser usado como atalho.
- Carregamento, falha com nova tentativa, ausência de cadastros e nenhum resultado de filtros têm estados distintos. Falha de rede não é lista vazia. Respostas antigas não substituem resultados de um filtro mais recente.
- A lista deve adaptar-se ao espaço disponível, com ações acessíveis por teclado e identificação dos botões. Evitar textos auxiliares genéricos; Tentar novamente permanece junto ao erro quando o espaço permitir.

### Mudança de contrato de busca aprovada

CustomerController.search retorna atualmente List<Customer>, sem paginação. A proposta é transformar a busca em resposta paginada, com conteúdo de entidades e metadados de página, em /customer/search, mantendo filtros individuais existentes e acrescentando os parâmetros de busca agrupada e paginação.

Essa mudança de array para resposta paginada é uma alteração explícita do contrato aprovada pelo usuário. O plano técnico deve definir o envelope usando a convenção já utilizada no backend atual, a compatibilidade necessária e os consumidores a atualizar, incluindo a coleção Insomnia. Não criar DTOs de resposta de negócio por conveniência nem migrar as buscas dos demais módulos nesta entrega.

## Proposta dos formulários

- Cadastro/edição em modal responsivo, aproveitando a decisão do desenho anterior. Detalhes são somente de leitura e não obrigam iniciar edição para consultar dados.
- Cadastro novo inicia como PF e ativo; o tipo pode ser escolhido antes de salvar. Na edição, PF/PJ é fixo porque o contrato atual não permite alterar personType. Uma mudança de tipo posterior exige decisão própria de domínio, fora desta entrega.
- PF: nome e CPF obrigatórios; nascimento opcional. PJ: razão social, CNPJ e nome fantasia obrigatórios; inscrições municipal/estadual opcionais. Telefone obrigatório e e-mail/WhatsApp opcionais em ambos.
- Respeitar os dados existentes: campos não exibidos para um tipo não são apagados silenciosamente. Não acrescentar validação de autenticidade/dígitos de CPF/CNPJ ou novas exigências fiscais por pressuposto.
- E-mail preenchido recebe validação de formato coerente com a API. Campos opcionais de texto podem ser limpos com string vazia no PATCH; obrigatórios vazios são rejeitados. Não tornar opcionais telefone/documento/nome nesta mudança.
- Tabela de preços é escolha opcional, com Sem tabela de preços. Carregamento/erro das opções é independente do formulário; ausência de prices.read não impede cadastro sem associação e não apaga vínculo existente durante edição.
- Proposta para opções de preço: oferecer tabelas ativas para novos vínculos, indicando a vigência quando houver. Preservar e identificar uma associação atual inativa na edição; não desassociar ou reprecificar por conta própria. O filtro da interface não altera silenciosamente as entradas aceitas pelo backend.
- Endereço inicial opcional na criação: quando habilitado, exigir os campos correspondentes, com complemento opcional; salvar cliente e endereço no POST transacional existente. Não fazer duas gravações independentes que deixem um cadastro parcial em caso de erro.
- Após criação/edição/situação, recarregar dados e usar os valores retornados da API. Bloquear envios repetidos e preservar campos em caso de falha; respeitar o mecanismo de rascunhos em memória já adotado, sem persistir dados pessoais em armazenamento novo sem necessidade.

## Proposta de detalhes e endereços

- Detalhes exibem dados do cliente, nome da tabela associada quando disponível, endereços e o bloco Informações do registro já aprovado. Não consultar itens/preços da tabela apenas para mostrar seu nome.
- Endereços são carregados por cliente, com Principal, Cobrança e Entrega. Respeitar a unicidade de cliente/tipo e oferecer apenas tipos disponíveis; na edição, incluir o tipo do próprio endereço.
- Leitura com customers.read, inclusão com customers.create, edição com customers.update e exclusão de endereço com customers.delete, conforme a autorização atual da API. O bloqueio de exclusão do cliente nesta interface não muda a permissão de exclusão do endereço.
- Não transferir um endereço entre clientes pela interface. Confirmar exclusão e impedir operações concorrentes sobre o mesmo fluxo; falhas preservam edição e não fecham o formulário automaticamente.
- Alterar endereço não deve simular atualização do timestamp do cliente; exibir somente as datas realmente persistidas de cada registro.

## Consulta de CEP

- Usar ViaCEP para preenchimento assistido de UF, cidade, bairro e logradouro; número/complemento permanecem sob entrada do usuário. Consultar apenas após CEP com formato válido.
- Distinguir formato inválido, CEP inexistente e indisponibilidade. Permitir cadastro manual quando a consulta estiver indisponível; não fazer a validade do cadastro depender da disponibilidade do provedor.
- Não sobrescrever uma correção manual ou os dados de outro CEP com uma resposta atrasada. Não completar campos com valores fictícios quando o CEP não trouxer informação suficiente.
- A interface deve oferecer ação explícita de consultar o CEP; não depender de solicitações a cada tecla. O plano técnico definirá chamada pelo backend ou navegador considerando o ambiente de implantação, sem instalar bibliotecas por padrão.

## Limites e critérios de conclusão

- Entrega contempla Clientes e endereços, seleção de tabela existente e o componente de metadados reutilizável. A aplicação do mesmo padrão em Funcionários/Cargos é um ajuste de interface já solicitado e deve aparecer explicitamente no plano, sem reabrir autenticação.
- Não criar manutenção completa de tabelas de preços, novos módulos operacionais, fluxo WhatsApp, emissão fiscal, exclusão física de clientes na tela ou histórico de alterações de negócio.
- Preservar permissões, autenticação/CSRF e respostas existentes fora das mudanças explicitamente aprovadas para Clientes. Não reescrever V1/V2.
- Critérios futuros: cadastro PF/PJ coerente, endereço opcional atômico, complemento removível, nascimento removível por intenção explícita, associação/remover tabela sem efeitos em pedidos antigos, filtros combinados e paginação real, permissões respeitadas, datas corretas e estados de erro/vazio completos.
- A validação futura deve distinguir revisão estática, execução manual e testes automatizados. Não afirmar funcionamento com base apenas na presença de código ou checklist.

## Próximo passo

O usuário aprovou o desenho consolidado, inclusive a busca/paginação e os comportamentos de interface. Preparar e revisar o plano técnico por arquivos, contratos, dependências e critérios verificáveis, sem iniciar implementação automaticamente. Novas mudanças de escopo não estão aprovadas por inferência.

Somente documentação foi preparada. Não executar código, migrations, testes automatizados, build ou lint nem fazer commits nesta fase.
