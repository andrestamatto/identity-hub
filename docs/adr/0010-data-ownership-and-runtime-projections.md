# ADR-0010: Propriedade de dados e projeções operacionais

- **Status:** Accepted
- **Data:** 2026-07-28

## Contexto

Keycloak precisa possuir credenciais, sessões e estruturas necessárias à emissão de
tokens. O IdentityHub precisa possuir conceitos de produto, configuração desejada,
memberships e auditoria suplementar.

Duplicar todos os dados criaria duas fontes de verdade. Acessar diretamente o schema
do Keycloak quebraria encapsulamento e upgrades. Entretanto, parte do estado do
IdentityHub precisa existir no motor para autenticação e emissão.

## Decisão

Cada dado possui proprietário explícito.

### Keycloak

- hashes e credenciais;
- identidades sociais operacionais;
- fatores MFA;
- sessões;
- authorization codes;
- access, ID e refresh tokens;
- chaves de assinatura;
- estado operacional necessário aos protocolos.

### IdentityHub

- `ClientApplication`;
- configuração desejada de clients;
- políticas de cadastro e contato;
- branding;
- intenção e lifecycle de `Membership`;
- definições de roles por aplicação;
- idempotência;
- outbox;
- auditoria de produto e reconciliação.

Estado necessário no Keycloak é uma projeção operacional, não segunda fonte de
verdade do conceito de produto.

Regras:

- bancos, schemas e usuários são separados;
- Flyway do IdentityHub não altera schema do Keycloak;
- mudança local e outbox são atômicas;
- projeção é idempotente;
- falha fica reconciliável;
- acesso somente é considerado emitível após projeção confirmada;
- drift é detectado e corrigido pelo contrato suportado.

## Consequências positivas

- upgrades do Keycloak permanecem suportáveis;
- domínio do produto não depende do modelo interno do motor;
- falhas parciais são visíveis e recuperáveis;
- dados não são duplicados sem finalidade;
- futura troca de motor é possível por adaptador.

## Consequências negativas

- consistência eventual entre plano de controle e motor;
- exige outbox, projeção, reconciliação e estados intermediários;
- operações precisam informar progresso real;
- auditoria é composta entre duas fontes.

## Alternativas consideradas

### Keycloak como fonte de verdade de todo o produto

Rejeitado porque configuração comercial e memberships ficariam presas ao modelo do
motor.

### IdentityHub como fonte de credenciais e sessões

Rejeitado porque duplicaria responsabilidades sensíveis do motor.

### Acesso direto ao banco do Keycloak

Rejeitado por quebrar suporte, segurança e migrações.

### Transação distribuída entre bancos

Rejeitada por complexidade e acoplamento; recuperação explícita é preferível.

## Validação

- testes impedem acesso ao schema do Keycloak;
- falha após commit local é retomada pela outbox;
- repetição não duplica client, role ou membership projetada;
- drift é detectado;
- token não concede acesso antes da projeção confirmada;
- reconciliação produz auditoria correlacionável.

## Documentos relacionados

- [Arquitetura](../architecture.md)
- [Especificação do MVP](../identityhub-spec.md)
- [Modelo de segurança](../security-model.md)
