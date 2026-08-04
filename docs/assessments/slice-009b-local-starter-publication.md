# SLICE-009B — Publicação local do starter para o checkpoint

> **Status:** complete
>
> **Data:** 2026-08-03
>
> **Branch:** `chore/checkpoint-starter-publication`

## 1. Resultado observável

O módulo `identityhub-spring-boot-starter` pode ser instalado explicitamente no
Maven Local do desenvolvedor como
`br.dev.andrestamatto:identityhub-spring-boot-starter:0.4.0-SNAPSHOT`, incluindo
JAR e POM com as dependências públicas necessárias. Assim, o Auto Radar pode
validar a integração real em seu próprio repositório antes da próxima capacidade
funcional do IdentityHub.

## 2. Requisitos e decisões relacionados

- `IH-MVP-020`, Integration Mode para API Java;
- `migration-strategy.md`, seções 10.1 e 11;
- `integration-mode.md`, seções 4, 5 e 6;
- `architecture.md`, seções 7 e 17;
- ADR-0003, ADR-0007 e ADR-0012.

## 3. Dentro do escopo

- configurar a publicação Maven local do único artefato Java já aprovado;
- fixar o `artifactId` documentado no POM gerado;
- documentar o uso local, explícito e temporário para o checkpoint;
- verificar o artefato e o POM gerados em Windows e WSL;
- manter as regras ArchUnit existentes viáveis no WSL sem reduzir a detecção de
  dependências diretas proibidas.

## 4. Fora do escopo

- publicar em Maven Central, GitHub Packages, repositório privado ou outro
  serviço externo;
- configurar credenciais, assinatura, deploy ou CI de publicação;
- editar, migrar ou executar build no repositório do Auto Radar;
- iniciar sessão, refresh token, logout, revogação ou qualquer capacidade após o
  checkpoint;
- declarar suporte a uma nova linha de Spring Boot;
- reduzir regras arquiteturais, ignorar dependências diretas ou remover testes
  para compensar desempenho.

## 5. Invariantes

- a coordenada pública permanece
  `br.dev.andrestamatto:identityhub-spring-boot-starter`;
- publicação local não altera a política de segurança do starter nem o contrato
  de JWT;
- nenhum segredo, token ou endpoint de repositório remoto entra no build;
- `mavenLocal()` é permitido somente para o checkpoint de desenvolvimento, não
  para CI ou produção;
- o Auto Radar continua fora deste repositório e deve realizar sua integração por
  seu próprio processo e agente.

## 6. Testes e evidências

- O estado anterior não aplicava `maven-publish`. A primeira publicação após a
  configuração mínima falhou corretamente porque dependências gerenciadas não
  possuíam versão publicável. A correção foi declarar a BOM oficial do Spring
  Boot como plataforma `api`, em vez de suprimir a validação do Gradle.
- A inspeção seguinte encontrou a BOM duplicada no POM pela combinação da
  plataforma com o plugin antigo. O starter passou a usar somente a plataforma
  Gradle. Uma nova falha controlada revelou que os annotation processors precisam
  da mesma plataforma; a configuração foi adicionada sem fixar versões manuais.
- `:identityhub-spring-boot-starter:publishToMavenLocal` passou no Windows em
  30 segundos e no WSL em 48 segundos. O Maven Local contém JAR, POM e metadata
  na coordenada esperada; o POM contém uma única importação da BOM `4.1.0` e não
  declara `org.keycloak`.
- A execução canônica inicial pelo checkout montado em `/mnt/c` não terminou em
  30 minutos. O thread dump mostrou `FoundationArchitectureTest` resolvendo
  recursivamente classes externas apenas para aplicar regras baseadas em nomes de
  pacote. A importação passou a manter as dependências presentes no bytecode sem
  resolver o classpath externo. Uma fixture negativa comprova que uma dependência
  direta de Spring ainda gera `AssertionError`; o teste focado passou no Windows
  em 1m09s e no WSL nativo em 38s.
- `./gradlew clean build` passou em uma worktree temporária no filesystem nativo
  do WSL em 8m49s, com `339` testes, `0` skips, `0` falhas e `0` erros. A
  worktree usou o mesmo commit-base e as mesmas alterações de build e teste desta
  fatia; ela foi usada apenas para evitar o I/O patológico do mount Windows e
  removida após a evidência.
- `./gradlew.bat clean build` passou no checkout principal em 3m25s, com `339`
  testes, `55` skips esperados por indisponibilidade de Docker no Windows, `0`
  falhas e `0` erros. Ambos os gates executaram Checkstyle, JaCoCo e `bootJar`.

## 7. Handoff do checkpoint externo

Depois do merge desta fatia, o Auto Radar deverá, no próprio repositório:

1. concluir sua migração para Spring Boot 4.1, conforme ADR-0012;
2. consumir a versão local fixa do starter somente em desenvolvimento;
3. configurar issuer e audience reais do ambiente de desenvolvimento;
4. compilar e iniciar uma API protegida;
5. demonstrar rejeição sem Bearer ou com audience incorreta e aceitação de access
   token válido;
6. confirmar que não há dependência, configuração ou chamada direta ao Keycloak.

Essa evidência é o checkpoint de integração previsto na estratégia. Ela não é
substituída pelos testes internos deste repositório. Até a evidência existir, a
próxima capacidade de sessão não será iniciada.

## 8. Migration, observabilidade e rollback

Não há migration de dados, alteração de banco, chamada de runtime ou nova
superfície pública. O rollback é reverter o PR; artefatos no Maven Local são
cache local descartável e não são parte do repositório.

## 9. Recuperação de contexto

Após compactação de contexto em 2026-08-03, foram relidos estado Git, última
fatia completa, `pending-decisions.md`, `autonomous-delivery.md`, sequência de
migração, `identityhub-spec.md`, `architecture.md`, `security-model.md`,
`integration-mode.md`, roadmap e ADR-0012. A revisão confirmou que o checkpoint
do primeiro SaaS é obrigatório antes de ampliar o escopo e que o Auto Radar não
pode ser alterado neste repositório. A publicação Maven Local é o menor preparo
interno para tornar esse checkpoint executável; não altera contrato público nem
postura de segurança.
