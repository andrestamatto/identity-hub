# ADR-0003: Modos de distribuição

- **Status:** Accepted
- **Data:** 2026-07-28

## Contexto

O IdentityHub precisa operar centralmente e, ao mesmo tempo, ser simples de adotar
em APIs Java. A ideia inicial de um servidor completo incorporado a cada consumidor
duplicaria usuários, chaves, sessões e atualizações.

Também existe interesse em melhorar futuramente o desenvolvimento local sem mudar
os contratos utilizados em produção.

## Decisão

O produto possui três modos conceituais:

### Service Mode

Modo central, obrigatório e primário. Opera identidade, protocolos, configuração,
acesso, tema, notificações e auditoria.

### Integration Mode

Starter leve incorporado a aplicações Java/Spring. Valida tokens, mapeia authorities,
oferece cliente tipado e console local, sem emitir tokens ou armazenar identidade.

### Local Development Mode

Evolução futura para desenvolvimento descartável ou simulado. Deve preservar os
contratos do Service Mode e não transforma o starter em servidor de autorização.

As unidades de entrega iniciais são:

- `identityhub-service`;
- `identityhub-spring-boot-starter`;
- `identityhub-keycloak-theme`.

## Consequências positivas

- identidade e operação permanecem centralizadas;
- consumidor Java recebe integração conveniente;
- contratos independem do motor;
- desenvolvimento local pode evoluir sem alterar produção.

## Consequências negativas

- Service Mode é dependência necessária para autenticação e administração;
- starter e serviço exigem matriz de compatibilidade;
- Local Development Mode não está disponível no MVP;
- três artefatos precisam ser versionados e testados em conjunto.

## Alternativas consideradas

### Embedded Mode completo

Rejeitado por duplicação, superfície de ataque e divergência operacional.

### Somente APIs sem starter

Rejeitado porque repetiria configuração sensível em cada API Java.

### Microsserviço para cada capacidade

Rejeitado por complexidade sem escala ou equipes que a justifiquem.

## Validação

- starter não armazena usuário nem emite token;
- aplicação inicia com Integration Mode sem dependência de classe do Keycloak;
- Service Mode funciona como unidade implantável central;
- Local Development Mode futuro passa pelos mesmos testes de contrato.

## Documentos relacionados

- [Arquitetura](../architecture.md)
- [Integration Mode](../integration-mode.md)
- [Roadmap](../roadmap.md)
