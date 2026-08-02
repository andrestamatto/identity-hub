# ADR-0013: Sessão de onboarding iniciada pelo backend consumidor

- **Status:** Accepted
- **Data:** 2026-08-01

## Contexto

Uma pessoa pode autenticar-se no IdentityHub antes de possuir `Membership` para o
SaaS que está adquirindo. A autenticação não pode conceder acesso, mas o backend
do SaaS precisa correlacionar a identidade comprovada com sua própria aquisição
sem receber senha, token humano ou detalhes internos do motor de identidade.

Permitir que o navegador declare livremente a aplicação ou uma referência de
aquisição possibilitaria substituição de contexto, replay entre produtos e
provisionamento indevido. Também não é suficiente devolver uma identidade sem
vínculo verificável com a transação que iniciou o fluxo.

## Decisão

O backend do SaaS inicia uma `OnboardingSession` antes da autenticação humana:

1. autentica-se por Client Credentials em um cliente de máquina da própria
   `ClientApplication`;
2. chama a API de integração com scope `onboarding:write`, idempotency key,
   referência opaca da aquisição, browser client, redirect URI exata e PKCE S256;
3. o IdentityHub deriva a aplicação do cliente autenticado, valida todos os
   vínculos e devolve um identificador opaco temporário da sessão;
4. o navegador inicia a experiência hospedada com a sessão, `state`, `nonce` e
   PKCE;
5. somente após autenticação e verificações obrigatórias o IdentityHub pode emitir
   uma `OnboardingIdentityProof` vinculada à sessão e à aquisição;
6. após sua decisão comercial, o backend consome a prova em uma solicitação
   idempotente de `Membership`.

A `OnboardingSession` inicial não contém usuário, não prova identidade e não
autoriza API de negócio. Seu prazo inicial é dez minutos. O identificador da
sessão não é uma credencial suficiente: a continuação e a conclusão exigem os
bindings de navegador e PKCE registrados.

A audience estável da API é `identityhub-integration-api`. O client scope
`onboarding:write` é projetado somente em clientes de máquina explicitamente
autorizados e inclui essa audience. Os client scopes gerenciados fazem parte do
bootstrap interno do realm e são verificados pela projeção; consumidores não
configuram o Keycloak diretamente.

O corpo nunca aceita `ClientApplication` ou cliente de máquina como autoridade.
A referência de aquisição é um identificador opaco do SaaS, não payload de
pagamento, plano ou assinatura. Referência e idempotency key são persistidas como
digests quando seu valor original não for necessário.

## Consequências positivas

- impede que o navegador escolha outra aplicação ou aquisição;
- preserva a senha e os tokens humanos fora do SaaS;
- vincula a futura prova a uma intenção criada por canal autenticado;
- permite retries seguros antes e depois da autenticação;
- mantém pagamento, plano e assinatura no domínio consumidor;
- conserva Keycloak como detalhe interno substituível.

## Consequências negativas

- o SaaS precisa de um backend para iniciar aquisições, mesmo quando sua interface
  principal for uma SPA;
- a configuração do cliente de máquina passa a incluir scopes explícitos;
- o realm precisa dos client scopes e mappers internos antes de projetar o
  cliente;
- o protocolo possui dois artefatos temporários distintos: sessão antes da
  identidade e prova depois das verificações.

## Alternativas consideradas

### Navegador inicia a aquisição sem backend

Rejeitada porque aplicação, retorno e aquisição poderiam ser manipulados antes de
existir um vínculo autenticado com o SaaS.

### Emitir prova apenas com aplicação e usuário

Rejeitada porque não vincula a identidade à aquisição que será confirmada pelo
backend e amplia replay.

### Criar `Membership` no login ou cadastro

Rejeitada porque autenticação não representa pagamento nem decisão comercial.

### Enviar credenciais humanas ao SaaS

Rejeitada por violar a fronteira de identidade e ampliar drasticamente a
superfície de ataque.

## Validação

- token de máquina exige issuer, audience, assinatura, validade e scope exatos;
- cliente autenticado só inicia sessão para sua própria aplicação;
- browser client e redirect URI de outra aplicação são rejeitados;
- replay idêntico é estável e colisão semântica é rejeitada;
- sessão expirada ou binding incompatível não pode gerar prova;
- nenhuma criação de sessão produz `Membership` ou token de negócio;
- testes de contrato executam contra PostgreSQL e Keycloak reais.

## Documentos relacionados

- [Especificação do MVP](../identityhub-spec.md)
- [Arquitetura](../architecture.md)
- [Modelo de segurança](../security-model.md)
- [Integration Mode](../integration-mode.md)
- [ADR-0002 — identidade global e membership](0002-global-identity-and-application-membership.md)
- [ADR-0007 — claims públicos](0007-public-access-token-claims.md)

