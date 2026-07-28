# ADR-0009: Snapshot de branding projetado no runtime

- **Status:** Accepted
- **Data:** 2026-07-28

## Contexto

A página hospedada precisa refletir branding e métodos habilitados da aplicação. Uma
chamada síncrona ao plano de controle durante todo login aumentaria latência e faria
a indisponibilidade administrativa impedir autenticação.

Permitir código ou estilos arbitrários fornecidos pelo consumidor criaria risco de
XSS, fraude visual e comprometimento do processo do Keycloak.

## Decisão

O plano de controle mantém a configuração desejada e projeta um snapshot seguro no
runtime de autenticação.

O tema:

- resolve o snapshot pela `ClientApplication`;
- aceita somente propriedades visuais declarativas aprovadas;
- usa URLs de assets controladas;
- utiliza fallback local do IdentityHub;
- não executa HTML, JavaScript ou CSS do consumidor;
- não faz chamada HTTP obrigatória ao plano de controle durante renderização.

O mecanismo concreto de leitura será escolhido por spike:

1. preferir mecanismo suportado pelo Keycloak sem extensão Java;
2. se insuficiente, usar provider interno mínimo;
3. versionar tema, provider e Keycloak em conjunto.

## Consequências positivas

- login continua com último snapshot válido;
- menor latência e acoplamento em runtime;
- personalização limitada e segura;
- fallback preserva fluxo utilizável;
- plano de controle pode reconciliar drift.

## Consequências negativas

- consistência é eventual;
- alteração visual pode não aparecer imediatamente;
- projeção e cache exigem versionamento;
- provider interno, se necessário, aumenta custo de upgrade.

## Alternativas consideradas

### HTTP síncrono por renderização

Rejeitado por latência e dependência de disponibilidade.

### Template completo por consumidor

Rejeitado por execução de conteúdo não confiável.

### Tema compilado separado por SaaS

Rejeitado no MVP por multiplicar artefatos e deploys.

### Sem branding

Rejeitado porque a experiência deve parecer parte do produto consumidor.

## Validação

- indisponibilidade do plano de controle não impede login configurado;
- aplicação nunca recebe branding de outra;
- asset inválido usa fallback;
- snapshot com versão antiga é detectável;
- spike e testes de upgrade validam a versão fixada do Keycloak.

## Documentos relacionados

- [Arquitetura](../architecture.md)
- [Modelo de segurança](../security-model.md)
- [Especificação do MVP](../identityhub-spec.md)
