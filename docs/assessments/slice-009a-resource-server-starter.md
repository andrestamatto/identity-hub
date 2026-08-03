# SLICE-009A — Starter Resource Server Servlet

> **Status:** complete
>
> **Data:** 2026-08-03
>
> **Branch:** `feat/slice-009a-resource-server-starter`

## 1. Resultado observável

Uma API Servlet Java 21 que adiciona
`identityhub-spring-boot-starter` e informa issuer e audience passa a proteger
todas as suas rotas por padrão. Ela aceita somente access tokens JWT `RS256`
válidos para a sua audience, emitidos pelo issuer configurado, dentro da janela
de tempo aceita. Scopes e roles públicos viram authorities previsíveis; claims
privados do Keycloak não concedem nenhuma authority.

## 2. Requisitos e decisões relacionados

- `IH-MVP-020`, incremento inicial de Integration Mode;
- `IH-MVP-012`, contrato público de access token já comprovado;
- ADR-0003, ADR-0007, ADR-0012 e ADR-0018;
- `integration-mode.md`, seções 8 a 10, 18 e 20;
- `security-model.md`, seções 8, 10.4 e 29.2.

## 3. Dentro do escopo

- subprojeto Gradle publicável `identityhub-spring-boot-starter`;
- auto-configuração Servlet padrão do Spring Boot, sem component scanning;
- propriedades validadas `identityhub.security` para issuer, audience, clock
  skew, prefixes de authority e exceção explícita de issuer HTTP em loopback;
- `JwtDecoder` com discovery/JWKS, algoritmo permitido `RS256`, validação de
  issuer, audience, timestamps e claims públicos obrigatórios;
- conversor de access token que lê somente `scope` e `roles` públicos;
- `SecurityFilterChain` Bearer stateless, default-deny, sem CORS global e sem
  method security implícito, criada apenas quando o consumidor não fornecer
  uma cadeia própria;
- respostas padrão Bearer `401` e `403` sem detalhe sensível;
- testes unitários, de contexto e HTTP para configuração, validação e recuo.

## 4. Fora do escopo

- WebFlux;
- cliente tipado de machine-to-machine;
- manifesto, validate/diff/apply e console local;
- health contributor, correlação em chamadas de saída e cache persistente de
  JWKS;
- alteração do Service Mode, contratos OIDC hospedados ou temas;
- papéis de negócio, refresh, logout e revogação.

## 5. Invariantes

- o starter não importa nem expõe tipos ou claims privados de Keycloak;
- falta de issuer ou audience com segurança habilitada impede startup;
- HTTP somente é aceito para issuer de loopback mediante opt-in explícito;
- token sem audience esperada, com issuer, assinatura, algoritmo ou tempo
  inválidos é rejeitado;
- `realm_access`, `resource_access`, grupos e PII nunca viram authority;
- cadeia fornecida pelo consumidor não é duplicada nem reordenada pelo starter;
- acesso anônimo é negado por padrão na cadeia fornecida pelo starter.

## 6. Falhas e bordas

- discovery ou JWKS indisponível no startup impede inicialização segura;
- `kid` desconhecido pode provocar uma atualização de JWKS; persistindo a
  ausência de chave confiável, a requisição é negada;
- claim `roles` vazia é válida nesta etapa; papéis de negócio continuam fora do
  escopo;
- o consumidor que desabilitar explicitamente a segurança sai do caminho
  suportado seguro e isso deve ficar visível em teste e documentação;
- configuração HTTP fora de loopback é rejeitada mesmo com opt-in.

## 7. Testes e evidências de implementação

- TDD: testes de propriedades e validadores ficaram vermelhos inicialmente por
  ausência das classes; a compilação/execução verdes só ocorreram após a
  implementação mínima. Um teste de decoder revelou clock divergente entre
  `iat` e expiração; o `JwtTimestampValidator` passou a receber o mesmo `Clock`
  injetável antes de o teste ficar verde.
- propriedades: issuer ausente, URI HTTP fora de loopback, audience inválida,
  prefixos vazios e clock skew negativo são rejeitados; defaults de binding são
  `enabled=true`, `60s`, `ROLE_` e `SCOPE_`;
- auto-configuração: `ApplicationContextRunner` prova os beans padrão, recuo
  diante de `JwtDecoder` e de `SecurityFilterChain` do consumidor, além da
  inatividade explícita com `enabled=false`; discovery disponível com JWKS
  `503` falha o contexto pela indisponibilidade efetiva da chave pública. Esse
  último teste foi vermelho antes do preflight, pois o decoder padrão adiava a
  leitura do JWKS para o primeiro token. O gate Linux revelou também que o
  timeout interno de `500ms` do Nimbus torna esse preflight frágil; o starter
  passou a fornecer `identityHubJwtRestOperations` com `2s` de conexão e `5s`
  de leitura, comprovado pelas mesmas cadeias HTTP e pelo decoder no WSL;
- JWT: issuer, audience, expiração, `iat` futuro, `kid` desconhecido, assinatura
  fora da chave confiável e algoritmo `HS256` são rejeitados por discovery/JWKS
  e validação local;
- HTTP: cadeia padrão responde `401` sem Bearer e para audience errada; token
  correto recebe `200`; cadeia consumidora produz `403` para scope/role ausente
  e reconhece somente `scope` e `roles` públicos;
- arquitetura: ArchUnit proíbe dependência em `org.keycloak` no starter;
- integração real: `KeycloakResourceServerIntegrationTest` executou sem skip
  no WSL/Docker em 2026-08-03, iniciou Keycloak `26.7.0` por Testcontainers,
  importou realm descartável, obteve token `RS256` por Client Credentials e o
  validou por discovery e JWKS, inclusive após a pré-verificação de startup. O
  relatório registrou `tests=1`, `skipped=0`, `failures=0` e `errors=0`.

O Windows atual não possui CLI/daemon Docker visível; por isso a mesma classe é
marcada como skipped nesse processo e não conta como evidência de integração.
O gate real é executado no WSL, onde Docker `29.1.3` está disponível.

- gates finais: `./gradlew clean build` no WSL concluiu em 18m06s com `338`
  testes, `0` skips, `0` falhas e `0` erros; `./gradlew.bat clean build` no
  Windows concluiu em 2m49s. Ambos executaram Checkstyle, JaCoCo e o `bootJar`
  do Service Mode; o JAR do starter contém `AutoConfiguration.imports` e
  `spring-configuration-metadata.json`.

## 8. Migration, observabilidade e rollback

Não há migration nem mudança no runtime do Service Mode. O artefato ainda não é
publicado em repositório externo; rollback é remover a dependência ou reverter o
PR. Logs de startup podem informar somente issuer normalizado, audience, versão e
recursos ativados, nunca token, header, segredo ou JWKS completo.

## 9. Recuperação de contexto

Após a compactação da conversa em 2026-08-03, foram relidos o estado Git, a última
fatia completa, `pending-decisions.md`, `autonomous-delivery.md`, a sequência de
migração, `IH-MVP-020`, Integration Mode, o modelo de segurança e ADRs 0003,
0007, 0012 e 0018 antes de qualquer alteração. Não havia divergência normativa ou
pendência bloqueante para esta fatia.

Após nova compactação na mesma data, essa recuperação foi repetida antes dos
gates finais. A revisão identificou a diferença entre discovery no startup e
leitura preguiçosa de JWKS do decoder do Spring; o preflight e sua prova negativa
foram incluídos para cumprir o contrato já aprovado, sem mudar o contrato público.
