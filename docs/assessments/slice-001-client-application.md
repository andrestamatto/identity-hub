# SLICE-001 — Cadastro de ClientApplication

> **Status:** awaiting human approval
>
> **Data:** 2026-07-31
>
> **Branch:** `feat/slice-001-client-application`
>
> **Base:** `develop` em `c836b37`

## 1. Objetivo

Entregar `IH-MVP-001` como a primeira capacidade de negócio vertical do novo
IdentityHub: um `PLATFORM_ADMIN` cadastra uma aplicação lógica isolada e um
`PLATFORM_AUDITOR` pode consultá-la sem executar mutações.

## 2. Escopo entregue

### 2.1 Domínio e aplicação

- aggregate `ClientApplication` independente de frameworks;
- UUID opaco, identificador slug imutável e nome de exibição normalizado;
- estado inicial seguro `DRAFT`;
- tempo injetado por `Clock`;
- cadastro e consulta por casos de uso próprios;
- retry com o mesmo UUID e conteúdo retorna o recurso existente;
- UUID reutilizado com outro conteúdo ou identificador atribuído a outra
  aplicação produz conflito;
- corrida concorrente idêntica relê e retorna o vencedor sem duplicar estado.

### 2.2 Persistência

A migration `V2__create_client_application.sql` cria a fonte de verdade no banco
do plano de controle com constraints para UUID, identificador único, formato,
nome e estado. O adapter JDBC executa round-trip por UUID ou identificador e
traduz somente violações de unicidade para conflito de aplicação.

Nenhuma tabela do Keycloak é acessada e nenhum client é projetado no motor nesta
slice.

### 2.3 API administrativa

| Operação | Autorização | Resultado |
|---|---|---|
| `PUT /internal/admin/client-applications/{applicationId}` | `PLATFORM_ADMIN` + TOTP | `201` criado ou `200` replay |
| `GET /internal/admin/client-applications/{applicationId}` | `PLATFORM_ADMIN` ou `PLATFORM_AUDITOR` + TOTP | `200` |

Entradas inválidas, recurso ausente e conflito retornam Problem Details seguros
com `400`, `404` e `409`. Nenhum segredo, token ou contrato do Keycloak aparece
na API.

### 2.4 Auditoria, correlação e métricas

- acessos permitidos e negados registram método, caminho, subject, resultado e
  correlation ID;
- query string, payload, token e headers de autenticação não são auditados;
- `X-Correlation-ID` válido é propagado na resposta;
- a duração do cadastro possui métrica por resultado `created`, `replayed`,
  `conflict`, `invalid` ou `failure`;
- UUID, identificador e nome da aplicação não são tags de métrica.

## 3. Evidência TDD

Os testes falharam antes da implementação por ausência do domínio, casos de uso,
adapter JDBC e endpoints. Uma prova adicional reproduziu deterministicamente a
corrida concorrente de unicidade antes da correção.

Depois da implementação mínima, os testes cobrem:

- limites e normalização dos value objects;
- estado inicial e instante controlado;
- criação, replay, conflitos e ausência;
- constraints e round-trip no PostgreSQL 17;
- contratos HTTP positivos e negativos;
- leitura do auditor e negação de mutação;
- auditoria permitida e negada;
- métricas sem tags de aplicação;
- token administrativo real com senha, TOTP, audience e papel esperados.

## 4. Gates executados

### 4.1 Windows

```text
.\gradlew.bat clean build --console=plain
BUILD SUCCESSFUL in 59s
```

O gate executou compilação, `bootJar`, Checkstyle, testes e JaCoCo. Os contratos
Testcontainers permanecem ignorados no host Windows sem Docker.

### 4.2 WSL com Docker

```text
./gradlew clean build --console=plain
BUILD SUCCESSFUL in 4m 20s
68 testes; 0 ignorados; 0 falhas
```

O cenário real iniciou Keycloak 26.7, PostgreSQL 17 do Keycloak e PostgreSQL 17
do IdentityHub. Um JWT obtido pelo login hospedado com TOTP cadastrou e consultou
uma aplicação pela API, confirmou a linha no banco e a auditoria correlacionada.

## 5. Guardrails e revisão de qualidade

- domínio não importa Spring, Jakarta, adapters, bootstrap ou Keycloak;
- aplicação não depende de adapters ou frameworks;
- adapter HTTP permanece fino e o adapter JDBC não contém regra de negócio;
- bootstrap possui somente composição e segurança;
- packages de topo permanecem sem ciclos;
- Checkstyle, ArchUnit, JaCoCo e empacotamento estão verdes;
- a revisão Clean Code/Harness não encontrou finding significativo aberto.

Durante a slice foi corrigida a regra genérica `out/` do `.gitignore`, que
ocultava os adapters JDBC da `SLICE-000`. A regra agora se limita a `/out/` e os
arquivos de auditoria omitidos foram recuperados em commit separado.

## 6. Fora de escopo

- `ApplicationClient` e projeção no Keycloak;
- outbox e reconciliação;
- usuários, memberships, login social e branding;
- API de aplicação consumidora;
- console administrativo próprio;
- listagem, alteração de estado ou exclusão de `ClientApplication`.

## 7. Harness para revisão manual

O harness versionado em `scripts/local-dev.ps1` e `scripts/local-dev.py` permite
subir PostgreSQL 17 e Keycloak 26.7 em loopback, executar o serviço pelo Gradle,
obter uma sessão administrativa pelo Device Authorization Grant hospedado e
realizar o round-trip `PUT`/`GET` desta slice.

O bootstrap é idempotente, mantém password grant desabilitado no cliente público,
exige cadastro TOTP no primeiro login e nunca imprime o access token. O Compose
usa o plugin local quando disponível ou a imagem oficial fixada
`docker:29.1.3-cli` como fallback. O procedimento está em
`docs/guides/local-development.md`.

Na prova local, os três containers ficaram saudáveis, a segunda execução detectou
o realm existente e a aplicação atingiu readiness `UP` com as duas migrations do
Flyway aplicadas. Três testes do harness passaram, o acesso anônimo retornou `401`
e o endpoint oficial de Device Authorization expôs o contrato esperado. A revisão
humana ainda deve concluir o login TOTP e executar a ação `smoke` antes da
aprovação final.

## 8. Revisão humana

Os gates automatizados estão verdes. A conclusão e o push da `SLICE-001`
dependem da revisão e aprovação humana deste incremento vertical.
