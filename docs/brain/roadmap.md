# Roadmap

Visão geral do trabalho. Contratos e limites de validação ficam em `contexto-do-sistema.md`; ações da frente ativa ficam em `plano-atual.md`.

| Frente | Situação atual | Próximo resultado |
| --- | --- | --- |
| Usabilidade | Plano aprovado; Etapa 1 autorizada em outro chat, sem conclusão confirmada | Corrigir orientação após estorno e quantidade por embalagem; manter as demais etapas aguardando execução autorizada. |
| Menu e dashboard padrão | Ajustes aplicados no código | Aceite visual da organização atual, menu móvel, foco e distribuição dos blocos. |
| Cadastros, API e operações | Implementados; fluxos centrais exercitados no navegador | Corrigir os achados do plano; validar concorrência, retries e demais cenários ainda não cobertos. |
| Financeiro | Pagamento, recebimento e estorno de venda/cancelamento exercitados | Melhorar identificação/fluxo; validar estorno de compra, casos restantes e permissões. |
| Empresa e demonstrativo | Cadastro/edição, CEP, emissão no despacho e consulta exercitados | Validar instalação nova completa, impressão e casos restantes de concorrência/permissão. |
| Aparência e tema | Seis paletas, logo e tema individual; Claro é o padrão | Validar upload e combinações visuais; manter a aparência existente. |
| Código de barras | Implementado | Aceite com leitor real e fluxos operacionais. |
| Sugestões de compra | Implementadas | Validar geração, cobertura, concorrência e aceite visual. |
| Inicialização SQL | V2 sem dados demonstrativos/usuários; V9 sem operações; scripts V1–V11 validados em base isolada standalone | Validar aplicação pelo Flyway, checksums/reinício e bootstrap em banco novo; não alterar o banco principal. |
| Integridade, segurança e qualidade | Verificações parciais; suíte backend completa e lint ainda sem fechamento | Validar em ambiente isolado e resolver ocorrências confirmadas, com execução solicitada. |
| Integrações externas | Futuro | WhatsApp, emissão fiscal real e pagamentos reais após estabilizar o núcleo. |

## Manutenção

Atualizar somente ao iniciar, concluir, bloquear ou repriorizar uma frente. Implementação aplicada não significa validação completa. Não criar planos paralelos nem históricos por conversa.
