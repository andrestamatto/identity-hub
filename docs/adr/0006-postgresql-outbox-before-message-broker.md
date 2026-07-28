# ADR-0006: PostgreSQL outbox antes de message broker

- **Status:** Accepted
- **Data:** 2026-07-28

## Contexto

Notificações e projeções para o motor exigem trabalho assíncrono, retry,
idempotência e rastreabilidade. Falha depois da persistência local não pode perder a
operação.

O volume inicial é desconhecido e a VPS já executa outras aplicações. Adicionar
RabbitMQ ou Kafka agora elevaria custo operacional sem evidência de necessidade.

## Decisão

O MVP utiliza Transactional Outbox no PostgreSQL do IdentityHub e workers internos.

Na mesma transação que altera o estado:

- o fato ou comando durável é persistido na outbox;
- worker reserva itens com concorrência segura;
- processamento é idempotente;
- retry é limitado e usa backoff;
- falha permanente permanece diagnosticável e reprocessável;
- correlação e tentativas são observáveis.

Não haverá broker no MVP.

Broker será reavaliado somente pelos gates do roadmap. A outbox continuará sendo a
fronteira transacional mesmo se um broker for introduzido.

## Consequências positivas

- não perde intenção entre banco e efeito externo;
- menor número de componentes;
- operação e backup simplificados;
- retry e reprocessamento rastreáveis;
- migração futura para broker permanece possível.

## Consequências negativas

- polling adiciona carga ao PostgreSQL;
- throughput e fan-out são limitados;
- retenção exige limpeza explícita;
- workers compartilham inicialmente ciclo de deploy do serviço.

## Alternativas consideradas

### Publicação direta após commit

Rejeitada porque uma falha entre commit e chamada externa perde a operação.

### RabbitMQ no MVP

Adiado até existir necessidade de filas independentes, isolamento ou throughput.

### Kafka no MVP

Adiado porque retenção, replay e volume ainda não justificam sua operação.

### Transação distribuída

Rejeitada por acoplamento e suporte limitado entre dependências externas.

## Validação

- teste interrompe processamento entre persistência e efeito externo;
- retry não duplica notificação nem projeção;
- dois workers não processam indevidamente o mesmo item;
- falha permanente fica disponível a admin e auditor;
- métricas cobrem backlog, idade, tentativa e resultado.

## Documentos relacionados

- [Arquitetura](../architecture.md)
- [Roadmap](../roadmap.md)
