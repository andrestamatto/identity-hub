# SLICE-007B — Projeção e ativação de Membership

> **Status:** implementation complete; ready for pull request
>
> **Data:** 2026-08-03
>
> **Branch:** `feat/slice-007b-membership-projection`

## 1. Resultado observável

Uma intenção de concessão aceita permanece `PENDING` até que um worker associe o
usuário ao marker técnico da própria aplicação no Keycloak. Somente após a
confirmação remota a `Membership` muda para `ACTIVE`. O cliente consulta a
operação sem obter identificadores de outra aplicação nem detalhes do motor.

## 2. Requisitos e critérios relacionados

- `IH-MVP-016`: concessão própria, retry após falha temporária, idempotência e
  obtenção posterior de novo token;
- ADR-0002, ADR-0004, ADR-0007, ADR-0010, ADR-0016 e ADR-0017;
- `architecture.md`: fonte de verdade local, outbox atômica e projeção
  reconciliável;
- `security-model.md`: isolamento, privilégio mínimo e falha fechada.

## 3. Dentro do escopo

- outbox específica criada atomicamente com novas memberships;
- backfill seguro das memberships `PENDING` da V17;
- lease, retry com backoff, falha permanente sanitizada e reconciliação;
- marker técnico privado por aplicação via Admin REST oficial;
- associação idempotente do `UserAccountRef` opaco ao marker;
- transição `PENDING` para `ACTIVE` somente após efeito remoto confirmado;
- consulta autenticada do estado da operação limitada à aplicação do token;
- métricas de resultado e duração sem identificadores.

## 4. Fora do escopo

- claims, audience ou roles em tokens humanos;
- definição e atribuição de roles de negócio;
- suspensão, remoção e revogação de sessões;
- endpoint administrativo equivalente;
- organização, equipe ou tenant B2B;
- cliente tipado do Integration Mode.

## 5. Invariantes

- `PENDING` e projeção `FAILED` nunca autorizam recurso de negócio;
- `ACTIVE` exige confirmação do marker remoto;
- grupo técnico não é emitido em token nem tratado como domínio;
- application id continua ausente do contrato consumidor;
- um cliente só consulta operações pertencentes à sua aplicação;
- replay e concorrência não duplicam marker, associação ou outbox;
- falha depois do efeito remoto converge sem ampliar acesso.

## 6. Falhas e bordas

- usuário inexistente, marker homônimo não gerenciado e resposta remota inválida
  falham de forma fechada e sanitizada;
- timeout, indisponibilidade e lease abandonado permitem retry;
- tentativa esgotada fica visível como projeção `FAILED`, com a Membership ainda
  `PENDING`;
- operação desconhecida ou de outra aplicação usa resposta que não permite
  enumeração cross-application.

## 7. Evidências implementadas

- domínio: transição para `ACTIVE` e ativação idempotente;
- PostgreSQL real: atomicidade, backfill, claim concorrente, lease, retry e
  commit conjunto de `ACTIVE`/`APPLIED`;
- adapter Keycloak: ownership, criação, replay, usuário ausente, conflito e
  respostas inválidas;
- HTTP/security: consulta e reconciliação próprias, isolamento
  cross-application, audience e scope;
- Keycloak 26.7 real: duas aplicações, markers isolados e associação do usuário;
- worker: retry após falha transitória e replay idempotente após efeito remoto;
- observabilidade: contadores por resultado estável e timer sem identificadores.

## 8. Migration, observabilidade e rollback

A V18 é aditiva, amplia o constraint de estado para `ACTIVE`, cria a outbox e
faz backfill das linhas pendentes. Antes de produção, rollback é o
revert do PR; memberships ativadas não serão usadas por tokens enquanto
`IH-MVP-012` não for entregue. Nenhuma limpeza remota automática será executada
no rollback sem inspeção e autorização.

## 9. Limite residual

Esta slice reconcilia drift conhecido por comando explícito; detecção periódica
de drift não foi adicionada. O marker não concede acesso sozinho e a emissão do
contrato público de audience e roles permanece em `IH-MVP-012`.

## 10. Gates executados

- recuperação pós-compactação: fontes normativas, branch, histórico e escopo da
  slice foram reconfirmados antes da retomada;
- Windows: `gradlew.bat clean build` verde em 1m46s;
- Linux/WSL: `./gradlew --no-daemon clean build` verde em 13m45s;
- 308 testes, sem falhas, erros ou testes ignorados;
- Checkstyle, JaCoCo, ArchUnit e `bootJar` concluídos nos builds canônicos;
- PostgreSQL e Keycloak 26.7 reais executados via Testcontainers;
- novo token humano emitido após a projeção sem claim `groups` e sem marker
  `ih-membership-*`.
