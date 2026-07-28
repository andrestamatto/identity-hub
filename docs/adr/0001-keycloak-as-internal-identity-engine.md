# ADR-0001: Keycloak como motor interno de identidade

- **Status:** Accepted
- **Data:** 2026-07-28

## Contexto

O IdentityHub precisa oferecer OAuth 2.0, OpenID Connect, credenciais, login social,
sessões, tokens, logout e MFA com segurança suficiente para produtos reais.

Construir e manter esses mecanismos diretamente atrasaria o primeiro SaaS e
transferiria ao projeto riscos de protocolo, criptografia e compatibilidade que já
possuem soluções consolidadas.

Ao mesmo tempo, o IdentityHub deve permanecer um produto próprio, com contratos,
experiência, regras de acesso e possibilidade futura de comercialização.

## Decisão

Keycloak será o motor interno de identidade do IdentityHub.

Ele será encapsulado pelo Service Mode. Aplicações consumidoras conhecerão somente:

- issuer e endpoints OAuth/OIDC publicados pelo IdentityHub;
- claims públicos do IdentityHub;
- APIs de integração e administração do IdentityHub;
- tema hospedado;
- Integration Mode.

Consumidores não utilizarão Admin REST API, classes, banco, configurações privadas
ou claims privados do Keycloak.

O IdentityHub pode ser comercializado usando Keycloak como componente interno,
desde que licenças, avisos e distribuição aplicáveis sejam respeitados.

## Consequências positivas

- menor tempo para uma solução segura;
- protocolos e mecanismos maduros;
- login social, MFA e gestão de sessão disponíveis;
- atualizações de segurança concentradas;
- possibilidade de trocar o motor sem quebrar contratos públicos compatíveis.

## Consequências negativas

- operação e atualização de um componente adicional;
- necessidade de testes de compatibilidade por versão;
- tema e eventuais providers internos exigem revisão a cada upgrade;
- algumas capacidades dependem do comportamento real do Keycloak.

## Alternativas consideradas

### Servidor próprio com Spring Authorization Server

Rejeitado para o MVP pelo tempo, risco e responsabilidade de operar corretamente
protocolos e fluxos de identidade.

### Integração direta dos SaaS com Keycloak

Rejeitada porque expõe detalhes do motor, multiplica configuração e dificulta
evolução do IdentityHub como produto.

### ZITADEL ou serviço externo gerenciado

Alternativas válidas, mas não escolhidas para a primeira implementação. A decisão
prioriza controle operacional, aderência à stack e capacidade de hospedar o motor
na infraestrutura disponível.

## Validação

- teste arquitetural impede dependência `org.keycloak` no starter e no domínio;
- contrato testa discovery, claims, erros e upgrades;
- versão do Keycloak é fixada e passa pelos spikes do modelo de segurança;
- consumidor integra sem chamar API administrativa do motor.

## Documentos relacionados

- [Visão do produto](../product-vision.md)
- [Arquitetura](../architecture.md)
- [Modelo de segurança](../security-model.md)
- [Repositório e licença do Keycloak](https://github.com/keycloak/keycloak)
