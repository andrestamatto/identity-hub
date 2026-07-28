# ADR-0002: Fonte de Verdade de Usuários e Autorizações

> Decisão histórica da baseline `v0.3.0`. Não representa a arquitetura vigente.

- Status: accepted
- Data: 2026-05-14

## Contexto
No MVP, o objetivo é minimizar fricção de integração com sistemas já existentes.
Ao mesmo tempo, o produto deve permitir futura execução como serviço.

## Decisão
No `Embedded Mode` (MVP), a fonte de verdade de `User`, `Role` e `Permission` será o projeto consumidor, via portas/adaptadores do IdentityHub.

No `Service Mode`, a estratégia de persistência será definida por versão, incluindo storage mínimo para sessão/revogação/auditoria.

## Consequências
- Ganho: integração rápida sem migração de dados no MVP.
- Ganho: flexibilidade para múltiplos modelos de usuário no consumidor.
- Custo: definição clara e rígida de interfaces SPI para evitar acoplamento acidental.
- Risco: consumidores com modelagem fraca podem degradar segurança/comportamento.

