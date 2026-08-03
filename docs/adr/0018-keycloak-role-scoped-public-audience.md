# ADR-0018: Audience pública condicionada por role scope no Keycloak

- **Status:** Accepted
- **Data:** 2026-08-03

## Contexto

O access token humano deve receber audience de API somente quando existir
`Membership` ativa, sem expor grupos, client roles nativas ou PII. Um audience
mapper fixo seria incondicional. O scope `roles` padrão do realm poderia expor
papéis de outras aplicações. Criar provider próprio aumentaria operação e
acoplamento antes de existir necessidade concreta.

## Decisão

O MVP usa somente recursos OIDC e Admin REST suportados pelo Keycloak:

- cada API gerenciada recebe uma role operacional privada de acesso;
- o grupo técnico da Membership recebe essa role;
- um client scope técnico por `ClientApplication` limita seu role scope às
  roles operacionais das APIs da mesma aplicação;
- o Audience Resolve mapper inclui como audience somente clientes para os quais
  o usuário possui role disponível naquele scope;
- SPA e BFF gerenciados recebem esse scope como default e não conservam client
  scopes herdados do realm;
- um mapper público produz `roles` vazio nesta fatia; papéis de negócio serão
  adicionados somente quando existirem no domínio do IdentityHub;
- claims nativos de roles, grupos e PII não são mapeados.

As audiences de todas as APIs protegidas da aplicação podem coexistir no token,
pois a `Membership` concede acesso à `ClientApplication` lógica. Separação de
privilégio dentro do SaaS pertence aos papéis de negócio, não a Memberships
duplicadas.

## Consequências positivas

- ausência de Membership implica ausência de audience de negócio;
- role scope impede vazamento cross-application;
- configuração usa recursos nativos e atualizáveis do motor;
- o contrato público permanece engine-neutral;
- nenhuma nova dependência, credencial ou extensão Java é necessária.

## Consequências negativas

- existe uma role e um client scope técnicos por aplicação;
- cliente criado depois de Membership ativa exige reconciliação segura;
- os papéis públicos ficam vazios até seu incremento próprio;
- o adapter precisa validar ownership de todos os artefatos gerenciados.

## Alternativas consideradas

### Audience mapper fixo

Rejeitado porque emitiria audience mesmo sem Membership.

### Scope `roles` padrão do realm

Rejeitado porque pode incluir `realm_access`, `resource_access` e audiences de
outras aplicações do mesmo realm.

### Protocol mapper customizado

Rejeitado porque exige provider Java e ciclo de atualização próprio sem benefício
necessário no MVP.

### Um realm por aplicação

Rejeitado pelo ADR-0004 e por fragmentar a identidade global.

## Validação

- usuário com Membership A recebe audience A e não B;
- usuário sem Membership não recebe audience de negócio;
- token público não contém role operacional, grupo, claims nativos ou PII;
- replay não duplica role, scope, mapper ou mapping;
- ownership divergente falha fechado;
- configuração é exercitada no Keycloak 26.7 real.

## Referências

- [Documentação oficial de audience do Keycloak](https://www.keycloak.org/docs/latest/server_admin/#_audience)
- [Documentação oficial de role scope mappings](https://www.keycloak.org/docs/latest/server_admin/#_role_scope_mappings)
- [ADR-0004](0004-single-realm-per-environment.md)
- [ADR-0007](0007-public-access-token-claims.md)
- [ADR-0010](0010-data-ownership-and-runtime-projections.md)
- [ADR-0017](0017-keycloak-membership-projection-marker.md)
