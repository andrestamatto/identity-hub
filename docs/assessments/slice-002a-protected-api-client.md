# SLICE-002A — API protegida e projeção no Keycloak

> **Status:** awaiting human review
>
> **Data:** 2026-07-31
>
> **Branch:** `feat/slice-002-api-client-projection`
>
> **Base:** `develop` em `9624375`

## 1. Objetivo

Entregar o primeiro incremento interno de `IH-MVP-002`: um `PLATFORM_ADMIN`
configura um `ApplicationClient` do tipo API e o IdentityHub projeta essa
intenção no Keycloak de forma durável, concorrente, idempotente e reconciliável.

Esta fatia não conclui todo o requisito. SPA, BFF/web confidencial e cliente de
máquina permanecem para incrementos próprios.

## 2. Escopo entregue

### 2.1 Modelo e fonte de verdade

- API habilitada, pertencente a uma `ClientApplication` existente;
- UUID opaco, chave única dentro da aplicação e audience exata e única no
  ambiente;
- ausência de wildcard, redirect URI, segredo ou fluxo de obtenção de token;
- configuração e intenção `PENDING` persistidas atomicamente no PostgreSQL;
- replay idêntico sem nova configuração ou operação.

### 2.2 Outbox específica

A migration `V3__create_application_client_projection_outbox.sql` cria as tabelas
`application_client` e `application_client_projection_outbox`. A operação guarda
versão de payload, correlação, estado, tentativas, próximo instante, código
fechado de falha e lease do worker.

A reserva usa `FOR UPDATE SKIP LOCKED`. Duas instâncias não recebem a mesma
operação durante o lease; uma projeção abandonada volta a ser elegível após sua
expiração. Falhas transitórias recebem backoff exponencial e falhas permanentes
ou tentativas esgotadas terminam em `FAILED`.

A outbox mantém uma única linha de estado corrente por `ApplicationClient`,
atualizada em vez de acumular uma linha por tentativa. Por isso, a retenção desta
linha acompanha a vida do client e não há limpeza automática destrutiva nesta
fatia; a política de exclusão do client será definida junto ao seu lifecycle.

### 2.3 Fronteira Keycloak

O adapter usa somente OIDC e Admin REST, sem biblioteca ou tipo do Keycloak fora
de `adapter.out.keycloak`. A projeção cria um cliente OIDC bearer-only:

- identificador interno determinístico `ih-api-{applicationClientId}`;
- nenhum secret, redirect URI ou web origin;
- standard, implicit, direct access, service account e authorization services
  desabilitados;
- audience pública preservada como atributo gerenciado pelo IdentityHub;
- marcas de propriedade impedem sobrescrever cliente remoto homônimo;
- reaplicação idêntica não duplica e drift de campos gerenciados é reparado.

O runtime autentica na Admin REST com service account próprio. No harness ele
recebe somente `realm-management/manage-clients`, com `fullScopeAllowed=false`.
Conta e senha bootstrap não são usadas pelo serviço.

### 2.4 API administrativa e operação

| Operação | Autorização | Resultado |
|---|---|---|
| `PUT /internal/admin/client-applications/{applicationId}/clients/{clientId}` | `PLATFORM_ADMIN` + TOTP | `201` ou `200` replay |
| `GET /internal/admin/client-applications/{applicationId}/clients/{clientId}` | admin ou auditor + TOTP | configuração e diagnóstico |
| `POST .../{clientId}/projection/reconcile` | `PLATFORM_ADMIN` + TOTP | `202` e nova projeção `PENDING` |

O diagnóstico apresenta `PENDING`, `APPLIED` ou `FAILED`, tentativas, próximo
instante e código sanitizado da última falha. A reconciliação explícita reaplica
o estado local desejado e corrige drift sem expor contrato do Keycloak.

A auditoria administrativa existente cobre acessos permitidos e negados. As
métricas registram configuração, reconciliação e ciclos do worker somente com
tags de resultado, sem UUID, audience, token ou segredo.

## 3. Evidência de testes

Os testes cobrem:

- invariantes de chave, audience e API;
- criação, replay, conflitos e atomicidade;
- reserva exclusiva, lease expirado, retry, backoff e falha definitiva;
- payload versão 1, correlação validada e diagnóstico seguro;
- criação idempotente, proteção de ownership e reparo de drift;
- retry após o efeito remoto ter sido criado e sua resposta ter sido perdida;
- contratos HTTP positivos e negativos para admin e auditor;
- service account de privilégio mínimo contra Keycloak 26.7 real;
- fluxo vertical real: Admin API autenticada com senha+TOTP, PostgreSQL,
  scheduler, outbox e estado final `APPLIED`;
- regras ArchUnit e ausência de tipos Keycloak fora do adapter dedicado.

## 4. Gates executados

### 4.1 Windows

```text
.\gradlew.bat clean build --console=plain
BUILD SUCCESSFUL in 1m 13s
102 testes identificados; 14 integrações Docker ignoradas no host Windows
```

### 4.2 WSL com Docker

```text
./gradlew clean build --console=plain --no-daemon
BUILD SUCCESSFUL in 7m 24s
102 testes; 0 ignorados; 0 falhas
```

Uma prova focada posterior habilitou a composição Spring do worker e percorreu
Admin API → PostgreSQL/outbox → Keycloak → `APPLIED` em Keycloak 26.7 real:

```text
./gradlew :identityhub-service:test \
  --tests '*KeycloakAdminTokenIntegrationTest' --no-daemon --console=plain
BUILD SUCCESSFUL in 3m 39s
```

Os três testes rápidos do harness Python também passaram.

## 5. Harness para revisão manual

O bootstrap local agora cria e restringe o service account de gerenciamento sem
imprimir seu segredo. Após iniciar o serviço:

```sh
local-env token
local-env smoke
```

O smoke cadastra a aplicação, configura a API, aguarda `APPLIED`, solicita
reconciliação e aguarda `APPLIED` novamente. A tentativa automatizada durante o
desenvolvimento encontrou corretamente uma sessão administrativa expirada e
retornou `401`; o script agora orienta executar novamente a ação `token`. A
conclusão manual requer novo login humano com TOTP.

## 6. Fora de escopo

- SPA, BFF/web confidencial e cliente de máquina;
- emissão ou validação pública de tokens;
- roles, memberships e autorização de negócio;
- exclusão ou desabilitação de clients;
- reconciliação periódica de todos os itens já aplicados;
- broker, outbox genérica ou eventos externos;
- console administrativo próprio.

## 7. Revisão humana

Pendente. Nenhum push será realizado antes da aprovação da fatia completa.
