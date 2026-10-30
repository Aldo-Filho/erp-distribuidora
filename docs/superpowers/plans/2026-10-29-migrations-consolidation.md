# Migrations consolidadas para recriar o banco

Pedido: reorganizar as oito migrations existentes, atualizar os inserts ao modelo atual e deixar os inserts na ultima versao. Autorizada a reescrita para banco novo; nao autorizada a execucao de SQL, Flyway, testes ou build.

## Organizacao final

- `V1__create-table-erp.sql`: estrutura final, sem inserts. Incorpora as definicoes anteriores de V1, V3, V5, V6, V7 e V8 diretamente nos CREATE TABLE, sem backfills para um banco vazio. A unica declaracao ALTER cria a FK circular de compatibilidade entre vinculo e opcao legada depois que as duas tabelas existem.
- `V2__insert-initial-data.sql`: ultima migration; configuracoes de seguranca, catalogo real de permissoes, modelos de cargo e dados ficticios de desenvolvimento. Incorpora os inserts de V2, V3 e V4.
- V3 a V8 removidas da pasta de migrations; seus efeitos de estrutura/dados foram incorporados acima. Numeros e checksums anteriores deixam de ser reutilizaveis. Usar em banco novo, com novo historico Flyway.

## Inserts atualizados

- Preservados os 20 cenarios regionais de clientes/fornecedores/depositos/documentos e os exemplos financeiros/fiscais que ja existiam.
- Produtos: 20 catalogos, sendo 19 simples e um com variacoes Preto/Azul; 21 SKUs gerados pela sequencia atual. Unidade UN explicitamente definida para cada pacote comercial dos exemplos; nao inferida de textos de peso/volume no nome.
- Apresentacoes e opcoes de fornecedor: UN/fator 1 e Caixa 6/fator 6, com conversao confirmada, custos comerciais proprios e tabela de preco por apresentacao. Custos sairam do vinculo e foram para as opcoes.
- Itens de compra/venda incluem referencias comerciais, quantidade BASE e snapshots de nome, SKU, unidade, fator e atributos.
- Cada compra historica recebida tem recibo, UUID deterministico por cenario e linha de recibo vinculada ao movimento de entrada. Cada venda despachada tem reserva CONSUMED vinculada a saida. Saldo de abertura tambem tem movimento: zero → abertura → entrada da compra → saida da venda → saldo final. Reservas ativas iniciais continuam zero.
- Usuarios ficticios continuam inativos e sem hash de senha utilizavel. Sem administrador com credencial fixa, sessoes, tokens ou eventos de acesso artificiais. Criacao de administrador continua pelo fluxo existente.
- Removidas permissoes ficticias `distribution.*`, substituidas por consultas reais nos perfis regionais. Modelos Vendas/Compras recebem `warehouses.read` para escolher deposito nas telas atuais; demais capacidades desses modelos preservadas.
- A ordem anterior dos cargos foi preservada: primeiro os 20 regionais, depois os cinco modelos. Referencias usam IDs retornados e chaves de permissao, sem presumir coincidencia de IDs entre tabelas.
- Ajuste solicitado depois da consolidacao: cada um dos 20 pedidos passa a conter tres produtos de catalogos distintos. Os dois itens adicionais usam apresentacao BASE, o mesmo deposito do pedido e seus proprios estoques, aberturas, reservas CONSUMED e movimentos SALE. Totais de pedido, itens/documento fiscal ficticio, conta a receber e pagamentos de exemplo sao atualizados juntos. As compras existentes permanecem sem alteracao. Revisao estatica; sem execucao de SQL ou testes.

## Revisao e limites

- Leitura de Git/diff, oito migrations e mapeamentos atuais; revisao direta dos CREATE TABLE, indices, chaves, tipos decimais, colunas dos INSERTs, dependencias, snapshots e vinculos dos movimentos.
- Nao executados banco, SQL, Flyway, servidor, testes, build ou lint. A revisao estatica nao comprova aplicacao das migrations nem validacao Hibernate.
- Existem copias antigas de V1-V8 em `backend/target/classes/db/migration`, provenientes de execucoes anteriores. Foram preservadas. Antes de iniciar o backend sobre o banco recriado, gerar artefatos limpos para que o classpath contenha somente as duas migrations atuais; isso nao foi executado neste trabalho.
- Documentos anteriores que citam V3-V8 permanecem como historico; esta nota descreve a organizacao vigente.
