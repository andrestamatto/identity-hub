# SLICE-004A — Política de autocadastro por aplicação

> **Status:** completed under autonomous delivery governance
>
> **Data:** 2026-07-31
>
> **Branch:** `feat/slice-004a-registration-policy`
>
> **Base:** `develop` em `6ce33fb`

## 1. Objetivo

Entregar o primeiro incremento de identidade local de `IH-MVP-003`: cada
`ClientApplication` declara explicitamente se o autocadastro está habilitado.
Ausência de configuração mantém a aplicação fechada por padrão.

## 2. Resultado observável

- nova aplicação nasce com `SelfRegistrationPolicy.DISABLED`;
- `PLATFORM_ADMIN` com TOTP configura `ENABLED` ou `DISABLED` por API;
- replay idêntico não produz alteração adicional;
- auditor com TOTP consulta a política, mas não pode alterá-la;
- PostgreSQL preserva a política por aplicação após reinício;
- configuração de uma aplicação não altera outra.

## 3. Invariantes

- default deny: somente `ENABLED` permitirá iniciar cadastro em fatia posterior;
- política pertence ao IdentityHub e não ao schema privado do Keycloak;
- nenhum usuário, senha, membership ou prova é criado nesta fatia;
- valores desconhecidos ou ausentes são rejeitados;
- a mutação administrativa permanece sob MFA, correlação e auditoria existentes.

## 4. Falhas e bordas

- aplicação inexistente retorna diagnóstico sanitizado;
- auditor, admin sem TOTP e anônimo não mutam;
- concorrência não pode misturar políticas entre aplicações;
- migration atribui `DISABLED` a todas as aplicações existentes.

## 5. Persistência e rollback

Flyway V9 adiciona coluna obrigatória com default explícito `DISABLED` e
constraint de allowlist. Antes de produção, rollback é revert do PR e recriação
do ambiente descartável; migrations aplicadas continuam forward-only.

## 6. Fora de escopo

- formulário ou endpoint público de cadastro;
- recebimento ou política de senha;
- criação de usuário no Keycloak;
- verificação de e-mail e emissão de prova;
- política de telefone, login social, membership ou branding.

## 7. Evidência

Os testes cobrem:

- default `DISABLED` no aggregate e na migration;
- transição explícita para `ENABLED` e replay sem nova escrita;
- rejeição de valor desconhecido ou ausente;
- persistência e isolamento entre duas aplicações no PostgreSQL 17 real;
- resposta administrativa com a política atual;
- permissão de mutação para admin com TOTP e negação para auditor;
- smoke local habilitando autocadastro antes de configurar os clients.

Gates executados em 2026-07-31:

- Windows: `.\gradlew.bat clean build` — verde em 1m11s, incluindo `bootJar`,
  Checkstyle, ArchUnit e regressões;
- Linux/WSL: `./gradlew clean build` — verde em 6m37s com 169 testes, sem
  falhas, erros ou ignorados, usando infraestrutura real via Testcontainers;
- harness: `python3 -m unittest scripts/test_local_dev.py -v` — 3 testes verdes.

## 8. Gate autônomo

A fatia satisfaz o contrato e está autorizada para publicação segundo
`docs/autonomous-delivery.md`. O próximo incremento pode usar somente aplicações
com `SelfRegistrationPolicy.ENABLED` para iniciar o cadastro local.
