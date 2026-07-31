# SLICE-002B — SPA pública e PKCE

> **Status:** awaiting human review
>
> **Data:** 2026-07-31
>
> **Branch:** `feat/slice-002b-spa-client-projection`
>
> **Base:** `develop` em `5b9e26d`

## 1. Objetivo

Entregar o segundo incremento interno de `IH-MVP-002`: um `PLATFORM_ADMIN`
configura um `ApplicationClient` do tipo SPA e o IdentityHub projeta no Keycloak
um cliente público com Authorization Code e PKCE `S256`.

Esta fatia reutiliza a outbox, o worker e a reconciliação da `SLICE-002A`. Ela
não implementa ainda o login do usuário final nem a emissão pública de tokens.

## 2. Contrato administrativo

O mesmo recurso administrativo agora usa um discriminador explícito:

```json
{
  "type": "SPA",
  "key": "auto-radar-web",
  "redirectUris": ["https://app.example.com/auth/callback"],
  "webOrigins": ["https://app.example.com"]
}
```

Clientes `API` exigem `audience` e rejeitam os campos de SPA, mesmo quando
vazios. Clientes `SPA` exigem as duas listas e rejeitam `audience`. Isso evita
payloads ambíguos e mantém um único endereço idempotente por client.

## 3. Invariantes locais

- uma SPA não possui audience nem segredo;
- cada lista contém de 1 a 10 valores únicos e preserva a ordem declarada;
- redirect URIs são absolutas, usam `http` ou `https`, têm host e não aceitam
  wildcard, userinfo ou fragment;
- web origins não aceitam wildcard, userinfo, path, query ou fragment;
- todo origin de redirect deve estar declarado em `webOrigins`;
- produção aceita somente HTTPS;
- desenvolvimento aceita HTTP somente em `localhost`, `127.0.0.1` ou `::1`;
- UUID, chave e replay idempotente seguem as invariantes da `SLICE-002A`.

As configurações específicas são um tipo selado de domínio. API protegida e SPA
não podem formar combinações inválidas dentro de `ApplicationClient`.

## 4. Persistência e projeção

A migration `V5__add_spa_application_client.sql` torna `audience` condicional ao
tipo e cria tabelas filhas ordenadas para redirects e origins. Configuração,
endpoints e operação `PENDING` são gravados na mesma transação.

O adapter Keycloak cria `ih-spa-{applicationClientId}` com:

- `publicClient=true` e nenhum segredo usado pelo IdentityHub;
- Authorization Code habilitado;
- PKCE obrigatório com método `S256`;
- implicit, direct access, service account, authorization services e full scope
  desabilitados;
- redirect URIs e web origins exatos;
- marcas de ownership e tipo gerenciadas pelo IdentityHub.

A reaplicação não duplica o cliente e o reconciliador repara drift nos campos
gerenciados. Projetos consumidores continuam sem contrato direto com Keycloak.

## 5. Evidência automatizada

Os testes cobrem:

- transporte por ambiente, endpoints exatos, cardinalidade e unicidade;
- criação, replay e conflito entre tipos;
- contrato HTTP positivo e rejeição de campos incompatíveis;
- round-trip ordenado em PostgreSQL real;
- representação idempotente e segura no adapter;
- projeção das propriedades de segurança contra Keycloak 26.7 real;
- fluxo vertical Admin API → PostgreSQL/outbox → worker → Keycloak → `APPLIED`;
- regras de arquitetura e regressão da API protegida.

### 5.1 Windows

```text
.\gradlew.bat clean build --console=plain
BUILD SUCCESSFUL in 1m
116 testes identificados; 16 integrações Docker ignoradas; 0 falhas
```

### 5.2 WSL com Docker

```text
./gradlew clean build --no-daemon --console=plain
BUILD SUCCESSFUL in 6m 1s
116 testes; 0 ignorados; 0 falhas
```

A prova focada do caminho vertical da SPA também passou separadamente:

```text
./gradlew :identityhub-service:test \
  --tests '*KeycloakAdminTokenIntegrationTest' \
  --rerun-tasks --no-daemon --console=plain
BUILD SUCCESSFUL in 3m 47s
```

Os três testes rápidos do harness Python passaram no WSL.

## 6. Harness para revisão manual

Após `local-env run` e `local-env token`, a ação abaixo valida os dois tipos:

```sh
local-env smoke
```

O smoke cadastra uma aplicação, projeta e reconcilia a API protegida, projeta
uma SPA pública em loopback e aguarda ambos os estados `APPLIED`.

## 7. Fora de escopo

- jornada de login do usuário final e tela hospedada;
- troca real do Authorization Code por tokens;
- armazenamento browser-side, refresh token e chamada à API;
- BFF/web confidencial e cliente de máquina;
- roles, memberships e autorização de negócio;
- exclusão ou desabilitação de clients.

## 8. Revisão humana

Pendente. Nenhum push será realizado antes da aprovação da fatia completa.
