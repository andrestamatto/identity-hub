# SLICE-000 — Ambiente e fronteira administrativa

> **Status:** approved
>
> **Data:** 2026-07-29
>
> **Branch:** `feat/slice-000-admin-boundary`
>
> **Base:** `develop` em `41d397d`

## 1. Objetivo

Entregar a primeira fatia vertical do novo IdentityHub:

- plano de controle Spring Boot protegido como OAuth 2.0 Resource Server;
- Keycloak 26.7 como motor interno encapsulado;
- PostgreSQL 17 separado para Keycloak e plano de controle;
- acesso administrativo cotidiano com papel de plataforma e TOTP recente;
- auditoria durável mínima dos acessos administrativos permitidos e negados.

Esta fatia implementa `IH-MVP-022`, o subconjunto inicial verificável de
`IH-MVP-023` e os controles transversais definidos para o `SLICE-000`.

## 2. Escopo entregue

### 2.1 Fronteira administrativa

O plano de controle agora:

- valida assinatura RS256 pelas chaves JWKS configuradas;
- exige issuer exato do ambiente;
- exige o audience administrativo configurado;
- converte somente `PLATFORM_ADMIN` e `PLATFORM_AUDITOR` em authorities;
- reconhece MFA somente quando `amr` contém `totp`;
- permite ao auditor e ao administrador consultar
  `GET /internal/admin/runtime`;
- reserva as demais rotas `/internal/admin/**` para `PLATFORM_ADMIN` com TOTP;
- responde `401` sem autenticação e `403` sem privilégio ou MFA suficientes;
- mantém sessão, login por formulário, HTTP Basic, request cache e logout local
  desabilitados.

Não foi criada uma mutação fictícia de produto. A prova negativa do auditor usa
uma rota inexistente sob a fronteira administrativa; a primeira mutação real
nascerá no `SLICE-001`.

### 2.2 Realm e token reais

O teste descartável cria, por Admin REST suportada:

- realm isolado;
- cliente administrativo distinto;
- papéis `PLATFORM_ADMIN` e `PLATFORM_AUDITOR`;
- mapper de audience administrativo;
- mapper oficial `oidc-amr-mapper`;
- conta administrativa sintética com senha e TOTP;
- referências `pwd` e `totp` nos executores do browser flow.

O fluxo usa Authorization Code com PKCE e percorre os formulários hospedados do
Keycloak. Direct Access Grants permanecem desabilitados no cliente testado.

As referências AMR possuem validade de 300 segundos. O valor `0` foi rejeitado
durante a implementação porque, no Keycloak, ele torna a referência inválida
assim que muda o segundo de autenticação. O intervalo escolhido prova MFA
recente sem transformar `amr` em evidência permanente.

### 2.3 Isolamento e ownership

O teste integrado inicia três dependências descartáveis:

1. PostgreSQL exclusivo do Keycloak;
2. Keycloak 26.7 em modo `start`;
3. PostgreSQL exclusivo do plano de controle.

O schema do plano de controle é criado pelo Flyway do IdentityHub. O teste e a
aplicação não consultam nem modificam tabelas do Keycloak.

Issuer e JWKS são configurações distintas. Isso permite validar o issuer
externo estável e buscar chaves por uma rota interna quando a topologia exigir,
sem aceitar issuer diferente.

### 2.4 Auditoria administrativa

A porta de aplicação aceita um evento mínimo append-only com:

- identificador e instante UTC;
- correlation ID;
- subject do operador, quando autenticado;
- método e caminho sem query string;
- resultado `ALLOWED` ou `DENIED`;
- motivo normalizado.

Tokens, senhas, códigos TOTP, cookies, headers e parâmetros de query não são
persistidos. Falha da persistência é registrada de forma sanitizada e não altera
o resultado da autorização.

A migração `V1__create_administrative_access_event.sql` pertence somente ao
banco do plano de controle.

### 2.5 Saúde e correlação

- liveness depende apenas do estado de vida da aplicação;
- readiness depende da aplicação, do banco e do endpoint JWKS do Keycloak;
- detalhes de health permanecem ocultos;
- o correlation ID validado ou gerado é devolvido na resposta, mantido no MDC e
  disponibilizado à auditoria.

## 3. Evidência TDD

Os seguintes ciclos falharam antes da implementação correspondente:

| Prova RED | Falha esperada |
|---|---|
| resource server administrativo | rotas retornavam a política fail-closed anterior |
| audience e conversão de authorities | tipos ainda não existiam |
| auditoria de aplicação | porta e eventos ainda não existiam |
| persistência PostgreSQL | adapter JDBC e migração ainda não existiam |
| auditoria HTTP | filtro e atributo de correlação ainda não existiam |
| prontidão do Keycloak | health indicator ainda não existia |

Os mesmos testes ficaram verdes após cada implementação mínima.

## 4. Provas de segurança e integração

### 4.1 Testes rápidos

Os testes verificam:

- token sem autenticação recebe `401`;
- papel administrativo sem `amr=totp` recebe `403`;
- auditor com TOTP consulta dados operacionais;
- auditor não alcança mutação administrativa;
- audience de consumidor é rejeitado;
- claims e papéis desconhecidos não criam privilégio;
- query string não entra na auditoria;
- falha ou indisponibilidade do Keycloak torna readiness `DOWN` sem detalhes.

### 4.2 Contrato real descartável

Executado dentro do WSL, onde o Docker está disponível:

```text
./gradlew :identityhub-service:test \
  --tests *KeycloakAdminTokenIntegrationTest \
  --tests *JdbcAdministrativeAccessEventRepositoryTest
```

O contrato comprova:

- Keycloak 26.7 iniciado sobre PostgreSQL 17;
- plano de controle iniciado sobre outro PostgreSQL 17;
- Flyway executado somente no banco do plano de controle;
- login hospedado com senha e TOTP;
- Authorization Code com PKCE;
- token assinado com audience administrativo, papel e `amr` esperados;
- rejeição do mesmo token para outro issuer ou audience;
- chamada HTTP real ao plano de controle;
- persistência dos acessos `ALLOWED` e `DENIED`.

### 4.3 Limitação local conhecida

O Docker não está exposto ao host Windows. Por isso, os testes Testcontainers
usam `disabledWithoutDocker=true`: são ignorados no build Windows e executados
de verdade no WSL ou CI com Docker.

Essa condição não produz falso positivo na evidência: o resultado Linux é
registrado separadamente do build canônico Windows.

### 4.4 Gates completos

No Windows:

```text
.\gradlew.bat clean build --no-daemon --console=plain
BUILD SUCCESSFUL in 2m 44s
29 testes; 2 testes Testcontainers ignorados sem Docker
```

Na cópia Linux temporária sob WSL:

```text
./gradlew clean build --no-daemon --console=plain
BUILD SUCCESSFUL in 5m 45s
29 testes; 0 ignorados; 0 falhas
```

Ambos executaram compilação, empacotamento, Checkstyle, testes e JaCoCo. O build
WSL também executou integralmente os contratos reais de Keycloak e PostgreSQL.

## 5. Guardrails arquiteturais

- aplicação de auditoria não depende de Spring, Jakarta ou adapters;
- classes Keycloak não atravessam o adapter futuro autorizado;
- nenhuma dependência `org.keycloak` foi adicionada;
- composição do adapter JDBC permanece explícita no bootstrap;
- packages de topo permanecem sem ciclos;
- não há segredo real nem valor secreto padrão na configuração;
- o console administrativo do Keycloak não é publicado por contrato do
  IdentityHub nem pelo cenário descartável.

## 6. Fora de escopo

Permanecem fora desta fatia:

- `ClientApplication` e `ApplicationClient`;
- memberships e usuários finais;
- login social;
- console administrativo próprio;
- deploy Coolify de produção;
- `BREAK_GLASS_ADMIN` operacional;
- recuperação completa de acesso;
- acesso direto de consumidores ou operadores ao console do Keycloak.

## 7. Aprovação

Os três gates automatizados ficaram verdes:

1. build canônico Windows;
2. build integral WSL com os contratos reais;
3. diff sem segredo ou contrato privado do Keycloak.

O escopo e as decisões desta fatia foram aprovados pelo responsável pelo produto
em 2026-07-30.

## 8. Referências primárias

- [Spring Security — OAuth 2.0 Resource Server JWT](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)
- [Keycloak 26.7 — Server Administration Guide](https://www.keycloak.org/docs/26.7.0/server_admin/)
- [Keycloak — Admin REST API](https://www.keycloak.org/docs-api/latest/rest-api/index.html)
- [Keycloak 26.7 — AMR protocol mapper](https://github.com/keycloak/keycloak/blob/26.7.0/services/src/main/java/org/keycloak/protocol/oidc/mappers/AmrProtocolMapper.java)
- [Keycloak 26.7 — AMR reference validity](https://github.com/keycloak/keycloak/blob/26.7.0/services/src/main/java/org/keycloak/protocol/oidc/utils/AmrUtils.java)
- [Testcontainers — JUnit 5](https://java.testcontainers.org/test_framework_integration/junit_5/)
- [Testcontainers — PostgreSQL](https://java.testcontainers.org/modules/databases/postgres/)
