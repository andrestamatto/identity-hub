# ADR-0007: Contrato público de claims do access token

- **Status:** Accepted
- **Data:** 2026-07-28

## Contexto

Keycloak possui representação própria de realm roles e client roles. Se consumidores
dependerem diretamente desses claims, uma atualização ou troca do motor exigirá
mudança em cada SaaS.

APIs também precisam distinguir emissor, destinatário, cliente, escopos e papéis sem
receber dados pessoais desnecessários.

## Decisão

Access tokens emitidos pelo IdentityHub expõem contrato público estável:

- `iss`;
- `sub` opaco;
- `aud`;
- `exp`;
- `iat`;
- `jti`;
- `azp`, quando aplicável;
- `scope`;
- `roles`;
- `sid`, quando aplicável.

`roles` contém somente papéis da aplicação destinatária. `scope` contém somente
escopos concedidos ao cliente.

O Integration Mode converte:

- scope em `SCOPE_<scope>`;
- role em `ROLE_<role>`.

Consumidores não interpretam `realm_access`, `resource_access` ou outro claim
privado do Keycloak.

PII não entra no access token por padrão. Claims adicionais exigem finalidade,
scope, contrato e análise de privacidade.

## Consequências positivas

- contrato independente do motor;
- validação consistente nas APIs Java;
- token menor e com menos PII;
- isolamento por audience e aplicação;
- evolução controlada por versão.

## Consequências negativas

- Service Mode precisa configurar e testar protocol mappers;
- claims duplicam ou transformam representação interna;
- alteração incompatível exige versionamento;
- consumidores não acessam automaticamente todo atributo do usuário.

## Alternativas consideradas

### Claims nativos do Keycloak

Rejeitados como contrato público por acoplamento.

### Realm roles compartilhadas

Rejeitadas para autorização de SaaS porque ampliam privilégios entre aplicações.

### Introspecção em cada request

Rejeitada no caminho comum por latência e dependência síncrona.

### PII ampla no token

Rejeitada por minimização, exposição em logs e dificuldade de revogação de dados.

## Validação

- teste de contrato verifica shape e semântica;
- token de aplicação A não autoriza B;
- starter ignora claims privados do Keycloak;
- ausência de claim obrigatório falha fechada;
- e-mail e telefone não aparecem sem scope aprovado.

## Documentos relacionados

- [Modelo de segurança](../security-model.md)
- [Integration Mode](../integration-mode.md)
- [Especificação do MVP](../identityhub-spec.md)
