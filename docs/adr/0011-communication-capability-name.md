# ADR-0011: `communication` como nome canônico da capacidade

- **Status:** Accepted
- **Data:** 2026-07-29
- **Supersede parcialmente:** ADR-0005, somente quanto ao nome `notification`

## Contexto

O ADR-0005 nomeou como `notification` o módulo responsável por comunicações. A
arquitetura detalhada refinou essa capacidade para incluir:

- solicitações de comunicação;
- templates transacionais;
- entrega de e-mail;
- prova mínima de posse de telefone;
- tentativas, backoff e falha permanente;
- idempotência de entrega.

O nome `notification` favorece a interpretação de um roteador universal de canais,
semântica que a arquitetura rejeita. E-mail e verificação telefônica possuem
finalidades, políticas e contratos diferentes.

## Decisão

`communication` é o nome canônico da capacidade e do package correspondente no
plano de controle.

A capacidade:

- coordena comunicações transacionais pertencentes ao IdentityHub;
- preserva contratos específicos quando os canais possuem semânticas diferentes;
- não cria uma abstração universal de mensageria;
- não incorpora comunicação de marketing;
- não recebe SMS ou WhatsApp no MVP sem necessidade real aprovada.

Esta decisão substitui somente o nome `notification` na lista de módulos do
ADR-0005. A decisão pelo modular monolith e todas as demais regras daquele ADR
permanecem vigentes.

## Consequências positivas

- linguagem consistente com `architecture.md`;
- menor incentivo a generalização prematura;
- separação mais clara entre comunicação operacional, prova de contato e
  marketing;
- nome compatível com a evolução futura sem prometer canais.

## Consequências negativas

- referências antigas a `notification` precisam ser interpretadas pelo contexto;
- observabilidade pode continuar usando o termo “notification” para o evento
  concreto entregue;
- documentação e packages novos precisam aplicar a nomenclatura de forma
  consistente.

## Alternativas consideradas

### Manter `notification`

Rejeitada porque sugere escopo mais genérico que o modelo aprovado.

### Criar módulos `email`, `sms` e `whatsapp`

Rejeitada porque organiza por tecnologia e antecipa canais fora do MVP.

### Criar somente `email`

Rejeitada como nome de capacidade porque prova de contato e futuras comunicações
operacionais não são necessariamente e-mail.

## Validação

- código novo usa o package `communication`;
- nenhum módulo vazio é criado antes de comportamento real;
- testes de arquitetura impedem retorno acidental ao package `notification`;
- documentação normativa usa `communication` ao nomear a capacidade;
- contratos específicos permanecem separados quando suas invariantes diferirem.

## Documentos relacionados

- [Arquitetura](../architecture.md)
- [Estratégia de migração](../migration-strategy.md)
- [ADR-0005](0005-modular-monolith-control-plane.md)
