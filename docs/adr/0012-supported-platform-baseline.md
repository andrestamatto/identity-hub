# ADR-0012: Baseline suportada da plataforma

- **Status:** Accepted
- **Data:** 2026-07-29

## Contexto

O IdentityHub substituirá a implementação abandonada antes da primeira fatia
funcional. A nova fundação precisa usar uma combinação suportada, segura e
reproduzível, sem herdar overrides ou versões escolhidas pelo legado.

O Service Mode, o Keycloak e o Integration Mode possuem ciclos distintos:

- o Service Mode é uma aplicação Spring Boot;
- o Keycloak é um runtime encapsulado e versionado por imagem;
- o Integration Mode precisa funcionar na stack do projeto consumidor.

Usar a mesma versão de framework em todas as unidades não é objetivo por si só. A
compatibilidade deve seguir o contrato de cada unidade.

## Decisão proposta

### Service Mode

Fixar:

- Java 21 LTS;
- Spring Boot 4.1.0;
- Gradle Wrapper 9.6.1.

Usar o BOM do Spring Boot sem overrides para:

- Spring Security 7.1.0;
- Flyway 12.4.0;
- PostgreSQL JDBC 42.7.11;
- Testcontainers 2.0.5;
- demais dependências cobertas pelo BOM.

Override de versão gerenciada exige incompatibilidade reproduzida, justificativa
documentada e teste de regressão.

### Infraestrutura

Fixar:

- Keycloak 26.7.0;
- PostgreSQL 17, inicialmente no minor 17.10.

Imagens de release usam tag explícita e digest validado. Patches do Keycloak
passam pela suite de contrato. O PostgreSQL permanece na major 17 e recebe minors
corretivos sem migration de aplicação.

Keycloak e IdentityHub usam databases ou schemas e credenciais separados, conforme
ADR-0010. Flyway do IdentityHub nunca modifica objetos do Keycloak.

### Integração administrativa

O adapter do Service Mode usa a Admin REST API documentada do Keycloak por cliente
HTTP do Spring e modelos privados mínimos.

Não usar `keycloak-admin-client` no MVP enquanto HTTP atender ao contrato. Não
expor tipos `org.keycloak` fora do adapter. Não usar Admin API v2 enquanto estiver
experimental.

A identidade técnica terá privilégio mínimo e não será a conta bootstrap. A conta
bootstrap existe apenas durante provisionamento inicial e deve ser invalidada ou
inutilizada no fluxo cotidiano.

### Integration Mode

O Auto Radar é o primeiro consumidor e será migrado para Spring Boot 4.1 em seu
próprio repositório antes do checkpoint de integração.

O starter do MVP mantém uma única linha principal em Spring Boot 4.1. Não declara
compatibilidade com Spring Boot 3.5. A compatibilidade com o consumidor deve ser
comprovada por teste executável após a migração, sem alterar o Auto Radar a partir
do repositório do IdentityHub.

## Evidências

O [assessment MIG-002](../assessments/mig-002-platform-compatibility.md) comprovou:

- aplicação mínima, contexto e `bootJar`;
- resolução do BOM sem downgrade;
- Keycloak em modo de produção sobre PostgreSQL;
- discovery OIDC;
- autenticação e escrita pela Admin REST API;
- realm, client, política TOTP e protocol mapper;
- service account limitada a clients e protocol mappers;
- negação de consulta de usuários e alteração de realm para essa identidade;
- configuração hospedada de TOTP compatível com RFC 6238;
- rejeição do mesmo código TOTP e aceitação do código seguinte;
- eventos de auditoria de configuração, sucesso e falha;
- Keycloak e PostgreSQL iniciados por Testcontainers.

## Condições para aceitação

As condições técnicas foram satisfeitas e a decisão foi aceita após revisão
humana.

Refresh token e branding permanecem como spikes obrigatórios antes das fatias de
sessão e experiência, respectivamente.

A compilação e inicialização do starter dentro do Auto Radar permanecem como gate
do checkpoint de integração posterior à migração daquele projeto.

## Consequências positivas

- baseline com suporte OSS atual;
- Java LTS comum ao serviço e ao Keycloak;
- versões transitivas governadas por um único BOM;
- ausência de acoplamento Java ao motor;
- PostgreSQL compatível com Keycloak e alinhado ao default self-hosted atual do
  Supabase;
- Testcontainers reproduzindo dependências reais;
- upgrades do motor protegidos por contratos.

## Consequências negativas

- migração do legado para Spring Boot 4 é uma quebra deliberada;
- o Auto Radar precisa migrar antes da integração;
- cliente HTTP exige DTOs privados para a Admin REST API;
- Keycloak adiciona uma matriz própria de upgrade;
- Docker no host atual depende do WSL e ainda não é uma experiência não
  privilegiada.

## Alternativas consideradas

### Spring Boot 3.5

Rejeitada para o Service Mode porque encerrou suporte OSS e não declara suporte ao
Gradle 9.

### Manter Gradle 9.3

Compatível, mas não preferido para a nova fundação porque 9.6.1 está estável e
passou pelo mesmo probe.

### Java 25

Suportado pelos componentes candidatos, mas rejeitado inicialmente. Java 21 já é
LTS, está instalado, é suficiente e reduz mudança simultânea no primeiro release.

### PostgreSQL 18

Suportado pelo Keycloak, mas rejeitado inicialmente. PostgreSQL 17 está alinhado ao
default self-hosted atual do Supabase, possui suporte até 2029 e reduz novidade
operacional. A versão efetiva do projeto Supabase hospedado deve ser inspecionada
antes da configuração de qualquer ambiente.

### `keycloak-admin-client`

Não escolhido para o MVP. O cliente publicado adiciona tipos do motor e seu ciclo
de versão não acompanha necessariamente cada release do servidor. A Admin REST API
atende ao adapter com fronteira menor.

### Módulo comunitário de Testcontainers para Keycloak

Não necessário no spike. `GenericContainer` iniciou a imagem oficial e preservou
controle explícito de versão e readiness.

## Validação contínua

- build e testes executam com Java e Gradle fixados;
- dependency insight impede downgrade silencioso;
- integração usa PostgreSQL e Keycloak reais;
- contrato cobre Admin REST, OIDC discovery e claims;
- teste arquitetural proíbe `org.keycloak` fora do adapter permitido;
- smoke test valida imagem por digest;
- upgrade de Keycloak exige revisão das release notes e execução da suite;
- starter é testado contra cada linha de Spring Boot declarada como suportada.

## Documentos relacionados

- [Estratégia de migração](../migration-strategy.md)
- [Arquitetura](../architecture.md)
- [Modelo de segurança](../security-model.md)
- [ADR-0001 — Keycloak como motor interno](0001-keycloak-as-internal-identity-engine.md)
- [ADR-0010 — propriedade de dados](0010-data-ownership-and-runtime-projections.md)
- [Spring Boot — requisitos](https://docs.spring.io/spring-boot/system-requirements.html)
- [Keycloak — release 26.7.0](https://www.keycloak.org/2026/07/keycloak-2670-released)
- [Keycloak — bancos suportados](https://www.keycloak.org/server/db)
- [PostgreSQL — política de versões](https://www.postgresql.org/support/versioning/)
