# SLICE-009A — Starter Resource Server Servlet

> **Status:** contract defined; implementation pending
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

## 7. Testes planejados

- propriedades: ausência, URI insegura, audience/prefixes/clock skew inválidos;
- auto-configuração: beans padrão, recuo para decoder, conversor e cadeia do
  consumidor;
- HTTP: `401` sem Bearer, `403` por authority ausente, token correto aceito;
- JWT: assinatura `RS256`, issuer, audience, expiração, algoritmo e claims
  obrigatórios inválidos são rejeitados;
- conversão: `scope` e `roles` públicos são mapeados, claims privados não;
- arquitetura: o starter não possui import `org.keycloak`;
- integração real com discovery/JWKS do Keycloak 26.7 quando a configuração
  básica estiver verde.

## 8. Migration, observabilidade e rollback

Não há migration nem mudança no runtime do Service Mode. O artefato ainda não é
publicado em repositório externo; rollback é remover a dependência ou reverter o
PR. Logs de startup podem informar somente issuer normalizado, audience, versão e
recursos ativados, nunca token, header, segredo ou JWKS completo.
