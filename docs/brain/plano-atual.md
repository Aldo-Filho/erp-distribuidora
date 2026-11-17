# Plano ativo — Tarefa 11: NF-e demonstrativa

**Estado:** implementação aplicada e revisão estática independente concluída; validação dinâmica pendente.

**Escopo autorizado:** implementar a emissão demonstrativa no despacho, com snapshots e identificação inequívoca de documento sem validade fiscal. Preservar alterações locais, contratos de despacho/financeiro/estoque e pendências anteriores do roadmap.

## Decisões

- Emissão única e atômica com o despacho: documento, consumo das reservas e mudança para INVOICED participam da mesma transação. Retry não duplica documento nem movimentos. Não emitir retroativamente para vendas já despachadas.
- Documento próprio, com número `DEMO-` e snapshots imutáveis de emissor, destinatário/endereço, pagamento, produtos, apresentações, quantidades comerciais/BASE e valores/descontos. Não usar estados AUTHORIZED, chave, protocolo, QR code, XML oficial ou cálculos tributários simulados como se fossem reais.
- Separar o documento demonstrativo do schema fiscal legado (`invoices`, `invoice_items`, `invoice_events`). Tabela nova evita reaproveitar estados de autorização real ou limitar quantidades a inteiros. Migration V10 somente se continuar livre; não aplicar sem pedido específico.
- Emissor vem exclusivamente do vínculo global de `company_appearance.company_id`. Se ausente/incompleto, capturar aviso explícito; não inventar empresa/documento nem acrescentar bloqueio ao fluxo operacional vigente. Cadastro fiscal da empresa não entra nesta tarefa.
- Consulta `GET /orders/{id}/demonstrative-invoice`, protegida por `orders.read`, com no-store. Emissão interna exige `orders.dispatch`; não oferecer endpoint de emissão manual. Documento inexistente retorna 404, inclusive legado sem emissão.
- Consulta no detalhe da venda, reutilizando modal/design system. Impressão em janela própria, escapando valores, com aviso e marca em todas as páginas. Nenhuma comunicação externa ou arquivo fiscal real.
- Interface `InvoiceProvider` e implementação demonstrativa isolam a futura integração, sem alterar venda/financeiro. Provedor futuro exige novo contrato e autorização; não basta trocar uma URL para emissão real.

## Etapas

- [x] 1. Criar migration V10, DTOs, armazenamento e interface do provedor.
- [x] 2. Capturar snapshots/valores com a mesma fórmula comercial e integrar à transação do despacho.
- [x] 3. Expor consulta autorizada sem escrita retroativa e preservar o contrato de despacho.
- [x] 4. Implementar consulta e impressão no detalhe da venda, com estados de carga/erro/ausência e avisos de validade.
- [x] 5. Revisar estaticamente atomicidade, retry, permissões, snapshots, decimais e impressão; consolidar contexto/regras/roadmap.

## Revisão estática e aceite pendente

- Revisão independente do módulo, migração, integração e impressão. Corrigidos: aviso de endereços existentes porém incompletos; captura/apresentação das características das variantes (tratadas como parte da identificação comercial do item).
- Inspecionados: lock/transação compartilhada, unicidade por venda, guarda de retry, ausência de backfill, snapshots e BigDecimal, consulta com permissão/no-store e escape integral da impressão. A inspeção não comprova compilação ou execução.
- Atualizada apenas a expectativa de versões do teste de migrations até V10, sem criar/executar testes.
- Aceite futuro autorizado: aplicar V10 em ambiente apropriado; despachar venda nova com UN/quantidade fracionária/caixa/descontos; conferir documento e movimentos; repetir e concorrer o despacho; simular falha para verificar rollback; alterar cliente/produto/empresa após emissão e conferir imutabilidade; consultar legado sem documento; verificar negação sem orders.read e impressão multipágina em claro/escuro/mobile.
- Arquivos: módulo `backend/.../fiscal`, V10, chamada no `OrderDispatchService`, serviço de consulta em `operations.js`, componente no `OrderDetails` compartilhado e componentes/impressão em `frontend/src/pages/Fiscal`.

## Limites de validação

Esta nova frente não autoriza testes/build/lint ou aplicação de migrations. O pedido anterior de testes tratava do defeito do dashboard. Para esta tarefa, não criar/executar testes, build, lint ou serviços sem solicitação explícita.

## Pendências anteriores preservadas

Dashboard: implementação aplicada; cinco testes HTTP/SQL passaram, frontend 15/15 e build, compilação backend passou. Suíte geral 71/72, com falha na fixture de último administrador em base reutilizada. Aceite visual completo pendente. Auditoria, fluxos, financeiro, identidade, código de barras e sugestões mantêm as pendências do roadmap. Encerramento de saldo parcial adiado; configuração externalizada revertida; WhatsApp/integrações reais fora de escopo.
