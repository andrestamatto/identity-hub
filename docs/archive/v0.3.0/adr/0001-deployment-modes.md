# ADR-0001: Deployment Modes do IdentityHub

> Decisão histórica da baseline `v0.3.0`. Não representa a arquitetura vigente.

- Status: accepted
- Data: 2026-05-14

## Contexto
O produto precisa atender dois cenários:
1. integração rápida em projetos Java/Spring já existentes;
2. operação futura como serviço independente.

Sem explicitar isso no início, decisões de domínio e infraestrutura ficam inconsistentes.

## Decisão
O IdentityHub terá dois modos oficiais:
- `Embedded Mode` (prioritário): biblioteca/starter no projeto consumidor.
- `Service Mode` (posterior): microsserviço independente.

Ambos devem compartilhar o mesmo core de domínio/aplicação.

## Consequências
- Ganho: evolução incremental com baixo custo de adoção no curto prazo.
- Ganho: reutilização de regras de negócio em ambos os modos.
- Custo: necessidade de contratos estáveis e testes de compatibilidade entre modos.
- Risco: divergência comportamental entre modos se adaptadores forem implementados sem suíte de contrato.

