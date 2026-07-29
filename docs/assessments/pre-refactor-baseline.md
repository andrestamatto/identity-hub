# Baseline técnica anterior à refatoração — IdentityHub

> **Status:** approved
>
> **Versão do documento:** 1.0
>
> **Data da avaliação:** 2026-07-29
>
> **Commit avaliado:** `cd6b8bd4fd58cb93e76ca4947b1833e70966a715`
>
> **Errata:** ver seção 20; a suíte executava 91 testes, embora a medição
> original tenha contabilizado somente 78.

## 1. Finalidade

Este documento registra o estado executável e estrutural do IdentityHub antes da
refatoração orientada pela nova especificação.

Ele existe para:

- estabelecer uma referência reproduzível;
- distinguir ativos reaproveitáveis de código pertencente à baseline abandonada;
- impedir que uma suíte aparentemente verde esconda lacunas de validação;
- orientar a futura estratégia de migração;
- fornecer um ponto de comparação para cada fatia incremental.

Esta baseline é uma fotografia técnica, não uma aprovação da implementação atual
nem uma especificação de comportamento futuro.

## 2. Escopo e método

### 2.1 Escopo avaliado

- build Gradle e toolchain Java;
- código de produção e testes;
- recursos, configurações e migração Flyway;
- separação entre domínio, aplicação, interfaces e infraestrutura;
- autenticação, segurança HTTP e custódia de credenciais;
- persistência e propriedade de dados;
- eventos, notificações e tolerância a falhas;
- observabilidade, CI e guardrails de engenharia;
- aderência à documentação aprovada.

### 2.2 Ações executadas

- inventário dos arquivos rastreados;
- inspeção estática de packages, imports, configurações e dependências;
- execução limpa e forçada da suíte padrão;
- comparação entre testes compilados e suites executadas;
- execução explícita de uma classe omitida;
- geração do artefato Spring Boot;
- inspeção da resolução de `spring-security-crypto`;
- revisão orientada por Clean Code, DDD, segurança, eventos e harness.

Nenhum código de produção, teste, build ou configuração foi alterado durante o
diagnóstico. Somente artefatos ignorados em `build/` e caches Gradle foram gerados.

### 2.3 Limites da avaliação

- não foi iniciado PostgreSQL;
- não foi executada uma instância do Keycloak;
- não foram realizadas chamadas reais a SMTP, Twilio ou WhatsApp;
- não houve DAST, SAST, scan de dependências, scan de imagem ou pentest;
- cobertura não pôde ser medida porque o projeto não configura uma ferramenta;
- o diagnóstico não afirma compatibilidade das versões atuais com a arquitetura
  futura;
- vulnerabilidades de versões específicas não foram pesquisadas nesta etapa.

## 3. Resumo executivo

### 3.1 Resultado geral

| Área | Estado | Conclusão |
|---|---|---|
| Compilação | Verde | Produção e testes compilam com Java 21 |
| Suíte padrão | Verde incompleto | 78 testes passam, mas 13 testes existentes não são descobertos |
| Empacotamento | Verde | `bootJar` é gerado |
| Arquitetura interna | Parcialmente saudável | Domínio e aplicação não importam frameworks ou camadas externas |
| Aderência ao produto novo | Baixa | Implementação representa a baseline abandonada |
| Segurança HTTP | Inexistente | Há hashing, mas não há proteção dos endpoints |
| Persistência real | Não verificada | Não existe teste com PostgreSQL/Flyway/JPA integrado |
| Eventos confiáveis | Ausente | Eventos e notificações são somente locais e não duráveis |
| Observabilidade | Inicial | Há SLF4J, mas não há correlação, métricas ou health |
| Guardrails | Insuficientes | Não há CI, lint, cobertura, testes de arquitetura ou instruções para agentes |

### 3.2 Decisão orientadora

A implementação atual não deve ser tratada como fundação funcional do novo
IdentityHub. Ela deve ser usada como:

- fonte de aprendizado;
- catálogo de testes e técnicas possivelmente reaproveitáveis;
- exemplo de portas e adapters;
- referência negativa para riscos já conhecidos.

Os núcleos de identidade, credenciais, acesso, persistência de usuários e segurança
devem ser substituídos conforme os limites aprovados. Tentar evoluir diretamente o
aggregate `User` e os endpoints atuais criaria uma migração mais complexa que uma
nova fundação vertical.

### 3.3 Bloqueadores antes da primeira fatia

1. corrigir a descoberta silenciosamente incompleta da suíte;
2. fixar versões compatíveis de Java, Spring Boot, Gradle, Keycloak e PostgreSQL;
3. configurar gates mínimos de análise estática, testes e arquitetura;
4. criar convenções específicas do repositório para desenvolvimento humano e
   assistido por agentes;
5. definir na estratégia de migração se a nova fundação substituirá o source set
   atual de uma vez ou coexistirá temporariamente.

## 4. Identificação da baseline

| Item | Valor observado |
|---|---|
| Branch de origem | `develop` |
| Commit | `cd6b8bd` |
| Sistema operacional | Windows 11 amd64 |
| Java do launcher | Azul Zulu OpenJDK 21.0.10 LTS |
| Java toolchain | 21 |
| Gradle Wrapper | 9.3.0 |
| Spring Boot plugin | 3.3.11 |
| Spring dependency management | 1.1.7 |
| Spring Cloud BOM | 2023.0.6 |
| Flyway declarado | 10.20.1 |
| Projeto Gradle | único, `identity-hub` |
| Versão do artefato | `0.0.1-SNAPSHOT` |

As versões acima são dados observados. A seleção definitiva pertence a ADR
específico e a spikes de compatibilidade.

## 5. Inventário do repositório

### 5.1 Dimensão

| Métrica | Valor |
|---|---:|
| Arquivos rastreados | 189 |
| Classes Java de produção | 115 |
| Linhas Java de produção | 2.635 |
| Arquivos Java em testes | 33 |
| Linhas Java de testes | 1.854 |
| Recursos de produção rastreados | 9 |
| Dependências `implementation` diretas | 13 |
| Dependências `runtimeOnly` diretas | 1 |
| Dependências `testImplementation` diretas | 1 |

Dos 33 arquivos Java em `src/test`, 30 são classes de teste e três são suportes de
dados.

### 5.2 Organização atual

```text
br.dev.andrestamatto.identityhub
├── application
│   ├── events
│   ├── exceptions
│   ├── ports
│   └── usecase
├── domain
│   ├── entities
│   ├── exceptions
│   └── valueobjects
├── infrastructure
│   ├── apis
│   ├── decorator
│   ├── events
│   ├── media
│   ├── messaging
│   ├── policy
│   ├── repository
│   ├── security
│   ├── support
│   └── usecase
└── interfaces
    └── rest
```

A organização é por camadas técnicas globais. A arquitetura aprovada exige
organização principal por capacidades:

- `clientapplication`;
- `identity`;
- `access`;
- `notification`;
- `audit`;
- `administration`;
- `sharedkernel` mínimo, quando inevitável.

### 5.3 Capacidades implementadas

- cadastro com e-mail ou telefone;
- senha própria com pepper e BCrypt;
- confirmação de cadastro por código;
- persistência opcional JPA ou em memória;
- migração inicial da tabela `users`;
- eventos Spring após cadastro e confirmação;
- notificações por e-mail, SMS e WhatsApp;
- templates Thymeleaf;
- retry imediato para SMTP;
- normalização de telefone;
- dois endpoints REST de usuário.

Não existem no código atual:

- `ClientApplication`;
- identidade global separada de acesso;
- `Membership`;
- `OnboardingIdentityProof`;
- Keycloak ou adapter de motor;
- OAuth 2.0, OpenID Connect, JWT, issuer ou audience;
- `SecurityFilterChain`;
- roles administrativas da plataforma;
- outbox, reconciliação ou projeções;
- Integration Mode;
- console local;
- experiência hospedada com branding;
- separação executável entre ambientes;
- auditoria de produto ou segurança.

## 6. Arquitetura e domínio atuais

### 6.1 Aspectos positivos

- código de domínio não importa Spring, Jakarta, JPA ou infraestrutura;
- código de aplicação não importa interfaces ou infraestrutura;
- dependências externas são majoritariamente injetadas por construtor;
- tempo é injetado por `Clock` nos casos de uso críticos;
- repositório, hashing, resolução de username e publicação de eventos possuem
  portas;
- controllers permanecem pequenos;
- a persistência usa mapper explícito;
- a migração de schema usa Flyway;
- testes unitários cobrem value objects e casos de uso centrais.

Esses padrões podem orientar o novo código, mesmo quando as classes concretas não
forem preservadas.

### 6.2 Aggregate atual

`User` concentra:

- identidade local;
- senha codificada;
- status;
- roles e permissions globais;
- tentativas e bloqueio de login;
- timestamps;
- token de verificação.

O aggregate tem 16 componentes e um builder interno. Parte de seu estado não é
inicializada nos fluxos atuais e não é persistida pelo adapter JPA.

Esse modelo conflita com as decisões novas:

- Keycloak possui credenciais, sessões, MFA e identidades sociais operacionais;
- IdentityHub possui a identidade orquestrada e os conceitos de produto;
- acesso ocorre por `Membership` isolada por aplicação;
- roles pertencem à aplicação destinatária;
- tokens de verificação operacionais não devem recriar um motor paralelo.

### 6.3 Persistência atual

O schema `users` armazena:

- identificador;
- username e tipo;
- senha codificada;
- status;
- timestamps;
- código, método e expiração da verificação.

Não há banco separado para Keycloak porque ele ainda não faz parte da
implementação. A migração `V1__create-table-users.sql` não deve ser tomada como
primeira migração do modelo novo.

O profile JPA contém URL local e username de desenvolvedor concretos. A senha é
externa, mas URL e usuário também precisam tornar-se configuração de ambiente.

## 7. Diferenças para a arquitetura aprovada

| Decisão aprovada | Estado atual | Tratamento esperado |
|---|---|---|
| Keycloak como motor interno | Ausente | Introduzir por adapter e contratos |
| Identidade global por ambiente | `User` local único | Substituir modelo |
| Acesso por `Membership` | Ausente | Implementar nova capacidade |
| Aplicações consumidoras | Ausente | Primeira capacidade candidata |
| Modular monolith por capacidade | Packages por camada | Reorganizar nova fundação |
| PostgreSQL outbox | Eventos Spring locais | Substituir antes de notificações críticas |
| Claims públicos estáveis | Sem token | Implementar por contrato |
| Console local | Ausente | Entregar no Integration Mode |
| Branding projetado | Ausente | Implementar após spike |
| Propriedade explícita dos dados | Credenciais no IdentityHub | Transferir ao Keycloak |
| Administração com MFA | Ausente | Implementar no escopo previsto |
| Autorização do SaaS no consumidor | Roles globais no `User` | Remover ambiguidade |

## 8. Build e dependências

### 8.1 Comandos reproduzidos

```powershell
.\gradlew.bat clean test --rerun-tasks --no-daemon
.\gradlew.bat bootJar --no-daemon
.\gradlew.bat dependencyInsight `
  --dependency spring-security-crypto `
  --configuration runtimeClasspath `
  --no-daemon
```

### 8.2 Resultado

| Verificação | Resultado |
|---|---|
| `compileJava` | Sucesso |
| `compileTestJava` | Sucesso |
| `clean test` forçado | Sucesso em aproximadamente 2m03s |
| Testes reportados | 78, sem falha, erro ou skip |
| `bootJar` | Sucesso em aproximadamente 54s |
| Artefato | `identity-hub-0.0.1-SNAPSHOT.jar` |
| Tamanho observado | 73.469.280 bytes |

Tempos são locais e servem apenas como referência inicial.

### 8.3 Resolução de Spring Security

O projeto declara explicitamente:

```groovy
implementation 'org.springframework.security:spring-security-crypto:6.3.1'
```

O dependency insight demonstrou que essa declaração substitui a versão `6.3.10`
solicitada pelo grafo gerenciado. Não foi avaliado se existe vulnerabilidade
associada; o problema objetivo é o desalinhamento manual com o BOM.

Não há `spring-boot-starter-security`, resource server OAuth2 ou integração com
Keycloak. A única capacidade de Spring Security presente é criptografia.

### 8.4 Guardrails de build ausentes

- análise estática Java;
- formatação verificável;
- cobertura;
- testes de arquitetura;
- scan de dependências e secrets;
- dependency locking ou verification metadata;
- pipeline CI;
- build de container;
- Testcontainers.

## 9. Baseline de testes

### 9.1 Resultado padrão

A suíte padrão reporta:

| Métrica | Valor |
|---|---:|
| Suites executadas | 22 |
| Testes executados | 78 |
| Falhas | 0 |
| Erros | 0 |
| Ignorados | 0 |
| Tempo somado das suites | aproximadamente 28,3s |

Distribuição observada:

- dois contextos Spring Boot, totalizando seis testes;
- um slice MVC com sete testes;
- 65 testes unitários ou de componentes leves;
- nenhum teste com PostgreSQL real;
- nenhum teste de segurança HTTP;
- nenhum teste de contrato OAuth/OIDC ou Keycloak;
- nenhum teste end-to-end;
- nenhum teste de arquitetura.

### 9.2 Falha de descoberta

O source set contém 91 métodos anotados como teste. Treze métodos distribuídos em
oito classes compiladas não aparecem no resultado padrão:

| Classe omitida | Métodos |
|---|---:|
| `SmtpEmailDeliveryRetryTest` | 1 |
| `DefaultWhatsappDeliveryTest` | 3 |
| `DefaultWhatsappSenderTest` | 1 |
| `TemplatedEmailRendererTest` | 3 |
| `UserVerificationCodeEmailTemplateTest` | 1 |
| `UserWelcomeEmailTemplateTest` | 1 |
| `TemplatedWhatsappRendererTest` | 2 |
| `UserVerificationCodeWhatsappTemplateTest` | 1 |

`DefaultWhatsappDeliveryTest` foi selecionado isoladamente e seus três testes
passaram. Isso confirma que ao menos essa classe é executável, mas ignorada pela
descoberta padrão.

A causa ainda não foi determinada. Até sua correção, “build verde” significa
somente que os 78 testes descobertos passaram.

### 9.3 Cobertura funcional

Há boa cobertura unitária relativa da baseline antiga para:

- value objects de username e senha;
- geração e validação de código;
- cadastro e confirmação;
- mapeamento REST;
- templates e roteamento de notificação;
- retry SMTP;
- normalização de telefone.

Faltam verificações decisivas para qualquer reaproveitamento:

- round-trip JPA com PostgreSQL e Flyway;
- constraints e concorrência de cadastro;
- atomicidade entre persistência e eventos;
- queda do processo após commit;
- retry e idempotência duráveis;
- endpoints protegidos;
- rate limiting e abuso;
- protocolos, tokens, sessões e logout;
- isolamento entre aplicações;
- reconciliação com motor;
- observabilidade e auditoria.

## 10. Segurança

### 10.1 Estado observado

O sistema:

- aplica SHA-256 com segredo global antes do BCrypt;
- impede segredo vazio durante criação das propriedades;
- não armazena senha em texto puro;
- evita registrar username completo nos casos de uso principais.

Entretanto:

- não configura autenticação ou autorização HTTP;
- expõe cadastro e confirmação sem `SecurityFilterChain`;
- envia username e código de confirmação em query string por `GET`;
- retorna respostas diferentes para usuário inexistente, duplicado e estado
  incompatível;
- mantém credenciais no banco do IdentityHub;
- não possui MFA, sessão, token, logout ou revogação;
- não possui headers, CORS ou CSRF explicitamente configurados;
- não possui rate limiting ou defesa de brute force;
- não possui scan automatizado de segurança.

Se a implementação atual fosse exposta, o código de confirmação poderia aparecer
em histórico, logs de proxy e observabilidade de URLs. O fluxo não deve ser levado
à produção e será substituído pela experiência hospedada e pelos contratos
aprovados.

### 10.2 Conclusão de segurança

O package chamado `security` fornece hashing, não uma fronteira de segurança web.
A existência de BCrypt não deve ser interpretada como implementação de
autenticação ou autorização.

A transferência de credenciais para o Keycloak elimina a razão de preservar:

- `BCryptPasswordHasher`;
- `EncodedPassword`;
- pepper global do IdentityHub;
- coluna `encoded_password`;
- fluxos REST próprios de confirmação.

## 11. Eventos e notificações

### 11.1 Fluxo atual

1. caso de uso salva `User`;
2. publica evento pelo `ApplicationEventPublisher`;
3. `@TransactionalEventListener(AFTER_COMMIT)` recebe o evento;
4. `@Async` executa notificação;
5. falha final é registrada e descartada.

### 11.2 Riscos

- evento existe somente em memória;
- queda após commit perde a intenção;
- não há outbox;
- não há idempotência;
- não há estado de entrega;
- não há reprocessamento administrativo;
- não há DLQ;
- exceção de notificação é capturada sem novo agendamento;
- retry SMTP é imediato e local ao processo;
- SMS e WhatsApp estão implementados antes de serem prioridade do MVP novo.

Esse desenho confirma a necessidade do ADR de outbox. Ele não deve ser migrado
como mecanismo de entrega confiável.

### 11.3 Ativos possíveis

Podem ser usados como referência, após simplificação:

- separação entre renderização e entrega;
- templates de e-mail;
- contratos pequenos para provider;
- testes de renderização;
- propriedades de timeout SMTP;
- ideia de escolher canal por política explícita.

Twilio, WhatsApp e a taxonomia atual de ports devem permanecer fora da primeira
fatia, salvo consumidor real.

## 12. Observabilidade e operação

### 12.1 Existente

- SLF4J nos casos de uso e adapters relevantes;
- níveis `info`, `debug`, `warn` e `error`;
- contexto parcial como `userId`, tipo de username e canal;
- stack trace em falha de notificação.

### 12.2 Ausente

- correlation ID;
- propagação de trace;
- métricas;
- health, liveness e readiness;
- auditoria persistida;
- alertas;
- dashboard;
- política de redaction central;
- CI;
- Dockerfile ou Compose;
- scripts de implantação;
- procedimento de backup ou restore.

Logs locais não satisfazem os requisitos de auditoria e diagnóstico da nova
especificação.

## 13. Clean Code Review

Files reviewed: 115 | Findings: 2 (High: 2, Medium: 0, Low: 0)

### Finding 1

- Severity: high
- Rule: single-responsibility
- Location: `src/main/java/br/dev/andrestamatto/identityhub/domain/entities/User.java:10`
- Problem: o record reúne identidade, credencial, acesso global, tentativas de
  login, bloqueio e verificação em 16 componentes.
- Why it matters: mistura responsabilidades que agora pertencem a Keycloak,
  `identity` e `access`, dificultando invariantes e migração.
- Suggested fix: não ampliar o aggregate; criar os modelos novos nos módulos
  proprietários e remover o modelo antigo quando a fatia substituta estiver
  validada.

### Finding 2

- Severity: high
- Rule: aggregate-integrity-bypass
- Location: `src/main/java/br/dev/andrestamatto/identityhub/infrastructure/repository/mapper/UserEntityMapper.java:13`
- Problem: o mapper persiste somente parte de `User`; roles, permissions,
  tentativas e bloqueio não participam do round-trip.
- Why it matters: um aggregate reconstruído não preserva todo o estado aceito por
  seu contrato público.
- Suggested fix: não corrigir o schema abandonado antecipadamente; substituir o
  modelo por aggregates com propriedade e persistência explícitas e testar cada
  round-trip com PostgreSQL.

Aspecto positivo: não foram encontradas dependências de domínio ou aplicação para
frameworks e camadas externas.

## 14. Harness Engineering Review

Files reviewed: 148 | Findings: 3 (High: 2, Medium: 1, Low: 0)

### Finding 1

- Severity: high
- Rule: missing-test-baseline
- Location: `build.gradle:55`
- Problem: a task `test` reporta sucesso executando 78 de 91 métodos anotados; 13
  testes compilados são omitidos.
- Why it matters: regressões nas capacidades omitidas podem passar pelo gate
  padrão.
- Suggested fix: diagnosticar descoberta, garantir que toda classe esperada seja
  executada e adicionar uma verificação de contagem ou suites até o gate ser
  confiável.

### Finding 2

- Severity: high
- Rule: no-correlation-id
- Location: `src/main/java/br/dev/andrestamatto/identityhub/interfaces/rest/UserController.java:30`
- Problem: requests entram sem identificador de correlação extraído, criado ou
  propagado.
- Why it matters: cadastro, evento e notificação não podem ser relacionados
  confiavelmente durante um incidente.
- Suggested fix: introduzir correlação na nova fundação e propagá-la por HTTP,
  aplicação, outbox, workers e auditoria.

### Finding 3

- Severity: medium
- Rule: no-lint-baseline
- Location: `build.gradle:1`
- Problem: o build não configura análise estática, cobertura ou testes de
  arquitetura.
- Why it matters: o modular monolith e as regras de dependência não terão
  enforcement mecânico.
- Suggested fix: selecionar o conjunto mínimo de ferramentas na estratégia de
  migração e fazê-lo bloquear a primeira fatia.

## 15. Agentic Engineering Review

Files / artefacts reviewed: 9 | Findings: 3 (High: 2, Medium: 1, Low: 0)

### Finding 1

- Severity: high
- Rule: no-agent-persona-file
- Location: repositório raiz
- Problem: não existe `AGENTS.md`, `CLAUDE.md` ou equivalente com convenções
  específicas de implementação e validação.
- Why it matters: agentes podem reproduzir a arquitetura abandonada ou interpretar
  de forma diferente os documentos aprovados.
- Suggested fix: criar instruções concisas antes da primeira alteração de código,
  apontando para especificação, arquitetura, segurança, ADRs, TDD e workflow.

### Finding 2

- Severity: high
- Rule: missing-test-baseline
- Location: task Gradle `test`
- Problem: a baseline verde não executa todos os testes existentes.
- Why it matters: não existe comparação confiável antes e depois de código gerado
  ou refatorado.
- Suggested fix: corrigir o gate antes da primeira fatia e registrar o comando
  canônico no `AGENTS.md`.

### Finding 3

- Severity: medium
- Rule: no-lint-baseline
- Location: `build.gradle:1`
- Problem: saída assistida por IA não pode ser validada por lint ou regra
  arquitetural do projeto.
- Why it matters: revisão humana fica responsável por detectar mecanicamente
  imports, ciclos e padrões proibidos.
- Suggested fix: adicionar validação mínima e reproduzível, sem criar uma coleção
  redundante de ferramentas.

## 16. Classificação para migração

### 16.1 Preservar

Preservar não significa congelar versões:

- Gradle Wrapper como mecanismo de build;
- Java 21 como baseline candidata, sujeita ao ADR de compatibilidade;
- disciplina de constructor injection;
- `Clock` como seam de tempo;
- testes de value objects e casos de uso como exemplos de estilo;
- SLF4J;
- uso de Flyway para o schema pertencente ao IdentityHub;
- arquivos de documentação e archive `v0.3.0`.

### 16.2 Adaptar

- convenções de testes e test data;
- separação entre ports e adapters, aplicada dentro de cada módulo;
- normalização de e-mail e telefone, respeitando que telefone de contato não é
  username;
- templates e entrega de e-mail;
- configuração tipada com `@ConfigurationProperties`;
- tratamento de erros REST, substituindo mensagens inseguras;
- logging com contexto, adicionando correlação e redaction.

### 16.3 Substituir

- package-per-layer global por package-per-capability;
- `User` por modelos com propriedade explícita;
- JPA `users` e seu mapper;
- cadastro e confirmação próprios por orquestração do Keycloak;
- hashing próprio;
- roles e permissions globais por membership e roles da aplicação;
- eventos Spring pós-commit por mudança local mais outbox;
- configuração de banco presa à máquina;
- endpoints REST da especificação abandonada.

### 16.4 Não migrar inicialmente

- SMS;
- WhatsApp;
- Twilio;
- tipos de IP e login attempt sem comportamento usado;
- roles e permissions antigas;
- suporte in-memory como modo alternativo de produção;
- Thymeleaf como decisão automática para a nova experiência hospedada;
- abstrações de canal não requeridas pelo e-mail do MVP.

Esses itens permanecem recuperáveis pela tag `v0.3.0` e pelo histórico Git.

## 17. Riscos para a estratégia de migração

| Risco | Impacto | Tratamento a definir |
|---|---|---|
| Refatorar `User` em vez de substituir | Alto | Nova fundação por capacidade |
| Preservar schema antigo por apego ao código | Alto | Nova baseline Flyway |
| Introduzir Keycloak em toda parte | Alto | Anti-corruption layer |
| Implementar infraestrutura horizontal primeiro | Alto | Fatias `IH-MVP-*` |
| Manter eventos locais como confiáveis | Alto | Outbox desde a primeira necessidade |
| Carregar SMS/WhatsApp para o MVP | Médio | E-mail primeiro |
| Fixar versões sem spike | Alto | ADR de compatibilidade |
| Confiar no teste verde atual | Alto | Corrigir descoberta antes de código |
| Criar muitos módulos Gradle prematuramente | Médio | Modularidade por packages primeiro |
| Migrar dados sem necessidade real | Médio | Confirmar se existe dado relevante |

## 18. Entradas para `migration-strategy.md`

A estratégia deverá decidir explicitamente:

1. se existe dado local que precise ser migrado ou se o banco pode recomeçar;
2. se a nova fundação será construída no mesmo projeto Gradle;
3. quais packages antigos podem coexistir temporariamente;
4. qual requisito `IH-MVP-*` inaugura a implementação;
5. quais versões serão fixadas após spike;
6. quais ferramentas formam o gate mínimo;
7. como o adapter Keycloak será testado com Testcontainers;
8. quando a outbox entra sem criar infraestrutura prematura;
9. em qual fatia o código antigo é removido;
10. como cada PR demonstra comportamento e rollback.

Recomendação inicial: a primeira fatia deve estabelecer um caminho vertical pequeno
de `ClientApplication`, incluindo domínio, persistência PostgreSQL, API
administrativa protegida e teste com Testcontainers. Ela não deve começar por
cadastro de usuário, porque isso favoreceria reutilizar indevidamente o motor
antigo.

Essa recomendação será debatida e aprovada no documento de estratégia; não autoriza
implementação.

## 19. Critério de encerramento da baseline

Esta baseline estará aprovada quando:

- números e comandos forem reproduzíveis;
- divergências relevantes estiverem registradas;
- a classificação de reaproveitamento for aceita;
- nenhuma conclusão for confundida com autorização de implementação;
- os bloqueadores forem incorporados à estratégia de migração.

Após aprovação, o documento deve ser marcado como `approved`. Seus resultados não
devem ser reescritos para acompanhar o código futuro; novas medições devem ser
comparadas com esta fotografia.

## 20. Errata identificada no MIG-001

Esta seção corrige uma interpretação da avaliação original sem substituir seus
registros históricos.

O comando `clean test` já executava as 30 classes e os 91 métodos de teste no
commit avaliado. A contagem original considerou somente arquivos de relatório
compatíveis com `TEST-*.xml`.

No Windows, o Gradle encurtou o nome de oito relatórios XML para manter seus
caminhos dentro do limite aceito pelo sistema de arquivos. Esses arquivos passaram
a começar com `__` e ficaram fora do filtro, embora suas suites tivessem sido
executadas normalmente.

A medição correta usa todos os arquivos `*.xml` de
`build/test-results/test/`. Ela encontrou:

| Métrica corrigida | Valor |
|---|---:|
| Suites executadas | 30 |
| Testes executados | 91 |
| Relatórios com nome encurtado | 8 |
| Falhas, erros ou ignorados | 0 |

Consequências:

- não havia defeito de descoberta no JUnit;
- as oito classes listadas na seção 9.2 não estavam omitidas;
- o risco real era a ausência de um gate explícito e a medição dependente do nome
  físico dos relatórios;
- o MIG-001 deve fortalecer o harness e documentar a medição correta, sem alterar
  testes ou dependências.
