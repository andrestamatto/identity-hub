# ADR-0004: Um realm por ambiente

- **Status:** Accepted
- **Data:** 2026-07-28

## Contexto

O IdentityHub precisa manter identidade global dentro de cada ambiente, isolar
desenvolvimento de produção e permitir múltiplas aplicações sem duplicar usuários.

Um realm por SaaS facilitaria algum isolamento físico, mas fragmentaria identidade,
SSO e administração. Um único realm compartilhado entre desenvolvimento e produção
misturaria chaves, usuários e risco.

## Decisão

Cada ambiente do IdentityHub utiliza um realm próprio.

No MVP:

- desenvolvimento usa realm independente;
- produção usa realm independente;
- todas as `ClientApplications` do mesmo ambiente compartilham seu realm;
- isolamento entre SaaS usa clients, audiences, memberships, client roles e escopos;
- usuários, chaves, sessões, providers e credenciais não atravessam ambientes;
- token de desenvolvimento é rejeitado em produção e vice-versa.

## Consequências positivas

- identidade global viável dentro do ambiente;
- menos duplicação de usuários e configuração;
- experiência integrada futura;
- separação forte entre desenvolvimento e produção;
- operação inicial compatível com equipe pequena.

## Consequências negativas

- erro de escopo no realm pode afetar várias aplicações;
- exige testes rigorosos de audience e role;
- clientes externos futuros podem demandar isolamento adicional;
- alterações de realm possuem blast radius maior.

## Alternativas consideradas

### Realm por SaaS

Rejeitado como padrão por fragmentar identidade e ampliar operação.

### Realm único para todos os ambientes

Rejeitado por risco de vazamento, confusão de issuer e uso cruzado de credenciais.

### Realm por cliente comercial

Não adotado no MVP. Pode ser reavaliado para requisitos regulatórios, isolamento ou
escala de clientes externos.

## Validação

- issuers de desenvolvimento e produção são distintos;
- API exige issuer e audience exatos;
- role de um client não aparece em token destinado a outro;
- testes usam ao menos duas aplicações no mesmo realm;
- configuração ou token de desenvolvimento falha em produção.

## Documentos relacionados

- [Especificação do MVP](../identityhub-spec.md)
- [Arquitetura](../architecture.md)
- [Modelo de segurança](../security-model.md)
