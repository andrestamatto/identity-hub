# MIG-002 — Compatibilidade da plataforma

> **Status:** approved
>
> **Data:** 2026-07-29
>
> **Branch:** `spike/mig-002-supported-toolchain`

## 1. Objetivo

Selecionar uma baseline suportada para a primeira release do IdentityHub e
demonstrar, antes do reset estrutural, que seus componentes centrais podem operar
sem dependências privadas do Keycloak.

Este assessment registra evidências. A decisão arquitetural proposta está no
[ADR-0012](../adr/0012-supported-platform-baseline.md).

## 2. Escopo deste ciclo

Incluído:

- Java, Gradle, Spring Boot e Spring Security;
- Flyway, driver PostgreSQL e Testcontainers gerenciados;
- Keycloak e PostgreSQL em containers reais;
- inicialização do Keycloak em modo de produção com PostgreSQL;
- autenticação na Admin REST API;
- criação de realm, client e protocol mapper por API pública;
- configuração de política TOTP;
- inicialização de Keycloak e PostgreSQL por Testcontainers;
- identidade técnica administrativa com privilégio mínimo;
- jornada hospedada de configuração e autenticação com TOTP;
- rejeição de reutilização do mesmo código TOTP;
- definição do primeiro consumidor e de sua linha Spring Boot.

Ainda não comprovado:

- comportamento de refresh token e detecção de replay;
- theme e leitura do snapshot de branding;
- compatibilidade binária do Integration Mode com o primeiro SaaS.

Refresh token e branding podem permanecer para os spikes que precedem suas
respectivas fatias. A compatibilidade executável do starter será comprovada no
checkpoint de integração com o Auto Radar, depois de sua migração independente
para Spring Boot 4.1.

## 3. Ambiente observado

| Item | Valor |
|---|---|
| Host | Windows 11 amd64 |
| Runtime de containers | Docker Engine 29.1.3 no WSL2 Ubuntu |
| Java no Windows | Azul Zulu OpenJDK 21.0.10 LTS |
| Java no WSL | OpenJDK 21.0.11 |
| Gradle legado | 9.3.0 |
| Spring Boot legado | 3.3.11 |
| Spring Security Crypto legado | override explícito 6.3.1 |
| Flyway legado | override explícito 10.20.1 |
| PostgreSQL JDBC legado | 42.7.5 |

O Docker não está disponível diretamente no host Windows. Os probes de containers
foram executados dentro do WSL como `root`. O ambiente deve receber uma forma
reproduzível e não privilegiada de executar Testcontainers antes do uso cotidiano.

## 4. Matriz oficial

| Componente | Candidato | Evidência oficial |
|---|---:|---|
| Java | 21 LTS | Suportado por Spring Boot, Gradle e Keycloak |
| Spring Boot | 4.1.0 | GA atual; suporta Java 17–26 e Gradle 8.14+ ou 9.x |
| Spring Security | 7.1.0 | Gerenciado pelo BOM do Spring Boot 4.1.0 |
| Gradle | 9.6.1 | Versão estável atual; executa em Java 17–26 |
| Keycloak | 26.7.0 | Release estável publicada em 2026-07-09 |
| PostgreSQL | 17.10 | Major suportado pelo Keycloak e pelo PostgreSQL até 2029; também é o default do Supabase self-hosted |
| Flyway | 12.4.0 | Gerenciado pelo BOM do Spring Boot 4.1.0 |
| PostgreSQL JDBC | 42.7.11 | Gerenciado pelo BOM do Spring Boot 4.1.0 |
| Testcontainers | 2.0.5 | Gerenciado pelo BOM do Spring Boot 4.1.0 |

Fontes:

- [Spring Boot 4.1 — requisitos](https://docs.spring.io/spring-boot/system-requirements.html);
- [Spring Boot 4.1 — dependências gerenciadas](https://docs.spring.io/spring-boot/appendix/dependency-versions/coordinates.html);
- [Gradle — matriz de compatibilidade](https://docs.gradle.org/current/userguide/compatibility.html);
- [Keycloak 26.7.0 — release](https://www.keycloak.org/2026/07/keycloak-2670-released);
- [Keycloak — bancos suportados](https://www.keycloak.org/server/db);
- [PostgreSQL — política de versões](https://www.postgresql.org/support/versioning/);
- [Supabase self-hosted — PostgreSQL 17](https://supabase.com/docs/guides/self-hosting/postgres-upgrade-17);
- [Testcontainers — PostgreSQL](https://java.testcontainers.org/modules/databases/postgres/);
- [Testcontainers — JUnit Jupiter](https://java.testcontainers.org/test_framework_integration/junit_5/).

### 4.1 Spring Boot 3.5 rejeitado para a nova baseline

Spring Boot 3.5.16 é a última release da linha 3.5 e encerrou suporte OSS em
2026-06-25. Além disso, sua documentação declara Gradle 7.6.4+ ou 8.4+, não Gradle
9.x.

Ele pode aparecer temporariamente como stack de consumidor, mas não deve iniciar
o novo Service Mode.

Fonte: [Spring Boot 3.5.16 — fim do suporte OSS](https://spring.io/blog/2026/06/25/spring-boot-3-5-16-available-now/).

## 5. Probes executados

Os fontes descartáveis ficaram sob `build/spikes/` e não fazem parte do diff.

### 5.1 Aplicação mínima

Uma aplicação mínima utilizou:

- Java 21;
- Spring Boot 4.1.0;
- starters Web MVC, OAuth2 Resource Server, JPA e Flyway;
- PostgreSQL JDBC;
- Spring Boot Test;
- Testcontainers PostgreSQL e JUnit Jupiter.

Resultados:

| Verificação | Gradle 9.3.0 | Gradle 9.6.1 |
|---|---:|---:|
| Compilação | verde | verde |
| Contexto Spring | verde | verde |
| `bootJar` | verde | verde |
| Dependências gerenciadas | verde | verde |

O cache global do Gradle apresentou corrupção após uma execução interrompida. Os
probes seguintes usaram `GRADLE_USER_HOME` isolado sob `build/`, sem apagar o cache
do usuário.

### 5.2 Resolução de dependências

Sem overrides:

| Dependência | Versão resolvida |
|---|---:|
| Spring Security Core | 7.1.0 |
| Flyway Core | 12.4.0 |
| PostgreSQL JDBC | 42.7.11 |
| Testcontainers | 2.0.5 |

Isso elimina o desalinhamento observado no legado, no qual uma dependência
explícita rebaixava silenciosamente a versão de Spring Security gerenciada.

### 5.3 Keycloak em modo de produção

Foram iniciadas as imagens:

- `quay.io/keycloak/keycloak:26.7.0`;
- `postgres:17.10`.

O Keycloak usou `start`, HTTP habilitado somente para o probe local e PostgreSQL
pela configuração pública. O cenário comprovou:

- inicialização sobre PostgreSQL;
- discovery OIDC;
- obtenção de token administrativo bootstrap;
- criação e leitura de realm pela Admin REST API;
- criação e leitura de client confidencial;
- criação e leitura de `oidc-usermodel-attribute-mapper`;
- política TOTP SHA-1, seis dígitos e período de 30 segundos.

O uso de HTTP e da conta bootstrap é exclusivo do spike. Produção requer HTTPS,
imagem otimizada, bootstrap efêmero e identidade técnica de privilégio mínimo.

### 5.4 Identidade técnica com privilégio mínimo

O probe criou um client confidencial com service account e atribuiu somente:

- `manage-clients`;
- `view-clients`;
- `query-clients`.

Essa identidade criou um client e seu protocol mapper. Com o mesmo token, a
consulta de usuários e a alteração do realm retornaram `403`. A conta bootstrap
não participou dessas operações cotidianas.

Essa lista é a menor permissão demonstrada pelo cenário atual, não uma concessão
permanente para todos os casos futuros. Cada novo caso administrativo deve ampliar
o contrato e a suite antes de ampliar privilégios.

### 5.5 Testcontainers

Testcontainers 2.0.5 iniciou:

- PostgreSQL 17.10, validado por conexão JDBC e consulta de versão;
- Keycloak 26.7.0, validado pelo discovery OIDC.

No filesystem montado do WSL, o Keycloak ultrapassou uma vez o timeout padrão do
teste. Com timeout explícito de três minutos, o cenário passou. A saída também
incluiu thread dump durante attach dinâmico de Mockito/Byte Buddy, sem falha final.

Consequências para o harness futuro:

- testes de container devem executar no filesystem Linux do CI sempre que possível;
- startup timeout deve ser explícito e medido;
- a imagem usada nos testes deve ser a mesma linha da produção;
- não depender de módulo comunitário específico de Keycloak quando
  `GenericContainer` atender ao contrato.

## 6. API administrativa e acoplamento

A integração comprovada utilizou HTTP e a Admin REST API documentada. Nenhuma
classe Java, tabela ou schema privado do Keycloak foi acessado.

O servidor 26.7.0 e o `keycloak-admin-client` publicado não compartilham
necessariamente a mesma versão. Para reduzir acoplamento e evitar que tipos do
motor atravessem a aplicação, a proposta é:

- adapter de saída usando cliente HTTP do Spring;
- DTOs privados e mínimos no adapter;
- testes de contrato contra a imagem fixada;
- nenhum `org.keycloak` no domínio, aplicação, contratos públicos ou starter.

A Admin API v2 anunciada no Keycloak 26.7 permanece experimental e não integra o
MVP.

Fontes:

- [Keycloak Admin REST API](https://www.keycloak.org/docs-api/latest/rest-api/index.html);
- [Protocol mappers](https://www.keycloak.org/admin-api/protocol-mappers);
- [Container e modo de produção](https://www.keycloak.org/server/containers).

## 7. TOTP

A política configurada usa TOTP, HMAC-SHA1, seis dígitos, período de 30 segundos e
`otpPolicyCodeReusable=false`, combinação interoperável com Google Authenticator.

O probe percorreu os formulários hospedados do Keycloak com cookies e redirects
reais e demonstrou:

1. administrador configura TOTP pela required action `CONFIGURE_TOTP`;
2. gerador RFC 6238 compatível produz código aceito;
3. novo login exige senha e TOTP;
4. o código já usado é rejeitado na mesma janela;
5. o código da janela seguinte é aceito;
6. eventos `UPDATE_TOTP`, `LOGIN_ERROR` e `LOGIN` ficam observáveis.

Segredo, senha e códigos são exclusivamente sintéticos e descartáveis. Nenhum
deles foi gravado nos documentos ou na saída do probe. A integração de automação
visual com navegador não estava disponível na sessão; por isso, a comprovação
funcional usou diretamente as páginas hospedadas, e não APIs privadas ou gravação
visual.

Fonte: [Keycloak Server Administration Guide — OTP](https://www.keycloak.org/docs/26.7.0/server_admin/).

## 8. Primeiro SaaS e Integration Mode

O Auto Radar foi confirmado pelo responsável do produto como primeiro consumidor.
Foi observado em seu backend:

- Java 21;
- Spring Boot 3.5.3;
- Gradle 8.14.3.

Foi aprovada sua migração para Spring Boot 4.1, conduzida no próprio repositório
e pelo contexto de desenvolvimento do Auto Radar. Assim, o IdentityHub mantém
uma única linha principal no MVP. Isso oferece:

- linha com suporte OSS;
- mesma geração de Spring Framework e Spring Security;
- uma matriz principal de compatibilidade;
- menor custo contínuo do starter.

O custo aceito é migrar o SaaS antes do checkpoint de integração. O repositório
do IdentityHub não alterará fontes do Auto Radar.

Não haverá suporte simultâneo a Spring Boot 3.5 e 4.1 no starter do MVP. O starter
deve permanecer fino, mas “fino” não prova compatibilidade: depois da migração, o
checkpoint deve compilar e iniciar o Auto Radar com o artefato real.

## 9. Recomendação

Fixar, após os gates pendentes:

```text
Java                 21 LTS
Spring Boot          4.1.0
Spring Security      7.1.0, gerenciado
Gradle Wrapper       9.6.1
Keycloak             26.7.0
PostgreSQL           17.10
Flyway               12.4.0, gerenciado
PostgreSQL JDBC      42.7.11, gerenciado
Testcontainers       2.0.5, gerenciado
```

Regras:

- versões de frameworks e bibliotecas vêm do BOM do Spring Boot;
- override exige incompatibilidade comprovada, justificativa e teste;
- imagens usam tag imutável e digest antes de produção;
- minor do PostgreSQL acompanha correções da major 17;
- patch de Keycloak exige suite de contrato antes da promoção;
- o Integration Mode não força a mesma versão do Service Mode sem necessidade
  técnica demonstrada.

## 10. Gates pendentes

| Gate | Estado |
|---|---|
| Aplicação mínima inicia | concluído |
| Keycloak em produção inicia com PostgreSQL | concluído |
| Testcontainers inicia dependências reais | concluído |
| Admin REST API pública funciona | concluído com bootstrap |
| Protocol mapper suportado funciona | concluído |
| Ausência de acesso ao schema privado | concluído |
| TOTP end-to-end e não reutilização | concluído |
| Identidade administrativa de privilégio mínimo | concluído |
| Primeiro SaaS e linha suportada | concluído: Auto Radar migrará para Boot 4.1 |
| Compatibilidade executável do starter com o consumidor | adiada até o checkpoint pós-migração |
| Refresh token | adiado até a fatia de sessão |
| Theme e branding | adiado até a fatia de experiência |

As evidências necessárias para decidir a baseline foram concluídas. A
compatibilidade executável permanece um gate de integração, não uma razão para
manter duas majors de Spring Boot no MVP.
