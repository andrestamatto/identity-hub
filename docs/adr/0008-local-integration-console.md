# ADR-0008: Console local incorporado ao Integration Mode

- **Status:** Accepted
- **Data:** 2026-07-28

## Contexto

Projetos consumidores precisam declarar, validar, comparar e aplicar configuração
sem depender de conhecimento operacional do Keycloak. Uma experiência semelhante a
ferramentas como Swagger UI reduz fricção para o desenvolvedor.

Uma interface administrativa exposta na instância produtiva do SaaS ampliaria a
superfície de ataque. Um serviço frontend separado também criaria uma quarta unidade
de entrega antes de haver necessidade.

## Decisão

O starter incorpora um console local em `/identity-hub-config`.

Características:

- React e TypeScript no build;
- assets estáticos dentro do JAR;
- nenhum Node.js no runtime consumidor;
- configuração separada em manifesto sem segredos;
- validate offline;
- diff autenticado;
- apply explícito, idempotente e protegido contra lost update;
- autenticação administrativa por Authorization Code com PKCE;
- callback em loopback;
- token administrativo no backend local e em memória volátil;
- CSRF, cookie protegido e CSP;
- console desabilitado por padrão;
- `server.address` restrito explicitamente a loopback.

O console nunca é servido pela instância produtiva do SaaS.

Uma execução local pode consultar produção, mas apply permanece bloqueado por padrão
e exige opt-in, MFA, autenticação recente, confirmação textual, novo diff e auditoria
destacada.

## Consequências positivas

- configuração guiada e próxima ao projeto consumidor;
- feedback antes de mutação;
- branding local pode ser enviado como arquivo;
- uma única distribuição do starter;
- não expõe APIs ou conceitos do Keycloak.

## Consequências negativas

- starter inclui bundle frontend;
- callback e loopback exigem spike de compatibilidade;
- Docker local precisa tratamento específico;
- processo consumidor local passa a custodiar temporariamente token administrativo;
- alterações de produção exigem controles reforçados.

## Alternativas consideradas

### Console público na aplicação produtiva

Rejeitado por superfície de ataque.

### Aplicação frontend separada no MVP

Rejeitada por nova unidade de build, deploy e autenticação.

### Aplicação automática no startup

Rejeitada porque uma inicialização não deve reconfigurar produção silenciosamente.

### Arquivo com secrets

Rejeitado por risco de versionamento e vazamento.

## Validação

- console não inicia sem opt-in e loopback;
- requisição externa é rejeitada;
- auditor não aplica;
- admin sem MFA ou autenticação recente não aplica;
- drift invalida diff antigo;
- segredo não é persistido;
- browser E2E cobre CSRF, sessão, diff e apply.

## Documentos relacionados

- [Integration Mode](../integration-mode.md)
- [Modelo de segurança](../security-model.md)
- [Especificação do MVP](../identityhub-spec.md)
