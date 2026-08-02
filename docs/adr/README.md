# Architecture Decision Records — IdentityHub

Este diretório contém decisões arquiteturais vigentes do IdentityHub.

Os ADRs da baseline abandonada `v0.3.0` permanecem preservados em
`docs/archive/v0.3.0/adr/` e não representam a arquitetura atual.

## Estados

- **Proposed:** em discussão, não autoriza implementação;
- **Accepted:** aprovado e vigente;
- **Superseded:** substituído por ADR posterior;
- **Deprecated:** mantido apenas por compatibilidade temporária;
- **Rejected:** alternativa avaliada e não adotada.

Um ADR aceito não deve ser editado para alterar retroativamente sua decisão. Uma
mudança material cria novo ADR e marca o anterior como superseded.

Correções de ortografia, links e esclarecimentos que não mudem a decisão podem ser
feitas no documento original.

## Índice

| ADR | Status | Decisão |
|---|---|---|
| [ADR-0001](0001-keycloak-as-internal-identity-engine.md) | Accepted | Keycloak como motor interno encapsulado |
| [ADR-0002](0002-global-identity-and-application-membership.md) | Accepted | Identidade global por ambiente e acesso por membership |
| [ADR-0003](0003-distribution-modes.md) | Accepted | Service, Integration e Local Development Modes |
| [ADR-0004](0004-single-realm-per-environment.md) | Accepted | Um realm por ambiente |
| [ADR-0005](0005-modular-monolith-control-plane.md) | Accepted | Modular monolith no plano de controle |
| [ADR-0006](0006-postgresql-outbox-before-message-broker.md) | Accepted | PostgreSQL outbox antes de broker |
| [ADR-0007](0007-public-access-token-claims.md) | Accepted | Contrato público de claims independente do motor |
| [ADR-0008](0008-local-integration-console.md) | Accepted | Console local incorporado ao Integration Mode |
| [ADR-0009](0009-projected-branding-snapshot.md) | Accepted | Snapshot de branding projetado no runtime |
| [ADR-0010](0010-data-ownership-and-runtime-projections.md) | Accepted | Propriedade de dados e projeções operacionais |
| [ADR-0011](0011-communication-capability-name.md) | Accepted | `communication` como nome canônico da capacidade |
| [ADR-0012](0012-supported-platform-baseline.md) | Accepted | Baseline suportada da plataforma |
| [ADR-0013](0013-backend-initiated-onboarding-session.md) | Superseded | Sessão proprietária de onboarding |
| [ADR-0014](0014-standard-oidc-acquisition-correlation.md) | Accepted | Correlação de aquisição com OIDC padrão |

## Decisões pendentes

Devem originar ADR somente quando houver evidência suficiente:

- linha inicial de Spring Boot compatível com o primeiro SaaS;
- mecanismo concreto de leitura do snapshot de branding na versão fixada do
  Keycloak;
- controle compensatório para refresh-token replay, caso o comportamento nativo
  não satisfaça o modelo de segurança;
- storage de objetos inicial;
- retenção concreta por categoria de auditoria e idempotência;
- entrada de broker ou extração de serviço quando os gates do roadmap forem
  satisfeitos.

## Template mínimo

Todo novo ADR deve conter:

1. status e data;
2. contexto;
3. decisão;
4. consequências positivas e negativas;
5. alternativas consideradas;
6. forma de validação;
7. documentos relacionados;
8. ADR substituído, quando aplicável.
