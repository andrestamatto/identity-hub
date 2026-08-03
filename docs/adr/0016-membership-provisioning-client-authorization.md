# ADR-0016: Autorização do cliente de provisionamento de Membership

- **Status:** Accepted
- **Data:** 2026-08-02

## Contexto

Depois de correlacionar uma aquisição ao `sub` obtido por OpenID Connect, o
backend ou BFF consumidor precisa solicitar ao IdentityHub a materialização da
`Membership`. A API deve reconhecer qual aplicação está solicitando o acesso sem
confiar em application id fornecido pelo navegador ou pelo payload.

Também é necessário distinguir clientes de máquina autorizados a provisionar
acesso de outros clientes técnicos da mesma aplicação.

## Decisão

A API de integração do MVP usa:

- OAuth 2.0 Client Credentials;
- audience estável `identityhub-integration-api`;
- scope `membership:write` para concessão, suspensão e remoção de acesso;
- claim `azp` validado para identificar o cliente autorizado.

O IdentityHub resolve internamente o `ApplicationClient` do tipo `MACHINE`
correspondente ao `azp` e obtém dele a `ClientApplication` proprietária. O path,
query e payload da operação não aceitam identificador da aplicação. O payload de
concessão contém somente a referência opaca do usuário e dados futuros que sejam
explicitamente aprovados pelo contrato.

O formato do client id projetado no Keycloak não é público. A tradução de `azp`
para o vínculo interno permanece em adapter próprio e pode mudar com o motor sem
alterar a API consumidora.

Um machine client recebe `membership:write` somente quando configurado
explicitamente. Tokens sem issuer, assinatura, tempo, audience, scope ou vínculo
de cliente esperados falham de forma fechada.

## Consequências positivas

- uma aplicação não escolhe nem falsifica a aplicação alvo da operação;
- uma única API possui audience previsível;
- scope diferencia provisionadores de outros clientes de máquina;
- não é necessário segredo compartilhado entre SaaS;
- o contrato não expõe identificadores privados do Keycloak.

## Consequências negativas

- a projeção do machine client precisa configurar audience e scope;
- o Service Mode precisa manter o vínculo entre `azp` operacional e
  `ApplicationClient`;
- clientes existentes sem a permissão explícita precisam ser reconfigurados para
  provisionar acesso.

As permissões atuais são persistidas em estrutura aditiva própria. A tabela
histórica de scope criada para o onboarding substituído permanece inativa,
conforme ADR-0014, e não é interpretada pelo runtime.

## Alternativas consideradas

### Aplicação informada no payload

Rejeitada porque transforma dado controlado pelo solicitante em limite de
autorização e aumenta o risco de concessão cross-application.

### Audience diferente para cada SaaS

Rejeitada porque obriga a API de integração a aceitar uma lista dinâmica de
audiences e não acrescenta isolamento além do vínculo criptograficamente
autenticado do cliente.

### Permitir provisionamento a todo machine client

Rejeitada por violar privilégio mínimo. Clientes técnicos sem necessidade de
gerenciar acesso não recebem `membership:write`.

## Validação

- token real de machine client autorizado contém a audience e o scope esperados;
- token sem audience ou scope recebe `403` ou `401` sem executar o caso de uso;
- `azp` desconhecido, desabilitado, não projetado ou de outro tipo é rejeitado;
- o contrato HTTP não possui campo de aplicação;
- clientes de duas aplicações só materializam `Memberships` próprias;
- repetição com a mesma idempotency key não duplica acesso.

## Documentos relacionados

- [Especificação do MVP](../identityhub-spec.md)
- [Arquitetura](../architecture.md)
- [Modelo de segurança](../security-model.md)
- [Integration Mode](../integration-mode.md)
- [ADR-0002](0002-global-identity-and-application-membership.md)
- [ADR-0014](0014-standard-oidc-acquisition-correlation.md)
