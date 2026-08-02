# SLICE-006B — Conclusão fail-secure da recuperação de senha

> **Status:** completed under autonomous delivery governance
>
> **Data:** 2026-08-02
>
> **Branch:** `feat/slice-006b-complete-password-recovery`

## 1. Resultado observável

O portador de uma prova de recuperação válida e ativa define uma nova senha pela
borda hospedada do IdentityHub. A prova é consumida uma única vez, todas as sessões
do usuário são revogadas antes da troca da credencial e uma confirmação de
segurança é enfileirada para o e-mail verificado.

## 2. Rastreabilidade

- `identityhub-spec.md`: conclusão de recuperação em `IH-MVP-015`, parte de
  `IH-MVP-014`, `IH-MVP-018` e `IH-MVP-019`;
- `architecture.md`: seções 8.2, 11, 14.4 e 14.5;
- `security-model.md`: seções 11, 13, 21 e 25;
- ADR-0001, ADR-0004 e ADR-0010.

## 3. Critérios de aceitação

- prova válida, ativa e não expirada é consumida antes de qualquer mutação externa;
- prova inválida, expirada, esgotada ou reutilizada falha com resposta idêntica;
- cada tentativa inválida é persistida e a quinta encerra o challenge;
- nova senha obedece à política de 15–64 caracteres e valores comuns bloqueados;
- senha e prova nunca aparecem em URL de requisição, resposta, log, evento,
  exceção ou representação de objeto;
- o adapter confirma que UUID/e-mail ainda correspondem e que existe credencial
  local antes da mutação;
- sessões do usuário são revogadas antes do reset da senha via Admin REST oficial;
- sucesso enfileira confirmação durável ao e-mail verificado;
- evento administrativo do Keycloak registra a mutação sem representação sensível;
- falha do motor depois do consumo não reativa a prova; o usuário deve solicitar
  outra, preservando fail-secure e uso único;
- testes reais demonstram PostgreSQL, Keycloak e Mailpit.

## 4. Limites e falhas distribuídas

Não existe transação atômica entre PostgreSQL e Keycloak. A ordem intencional é:

1. validar e consumir a prova em transação local;
2. revogar sessões no Keycloak;
3. redefinir a senha;
4. persistir a confirmação no outbox.

Se o processo falhar depois do passo 1, a prova permanece usada e uma nova deve
ser solicitada. Se falhar depois do passo 2, sessões continuam revogadas, que é o
resultado seguro. Se houver sucesso no passo 3 e falha local no passo 4, a senha já
mudou e a confirmação deve ser diagnosticada operacionalmente; a fatia não
persiste senha nem tenta repetir uma mutação sem o segredo original.

Ficam fora do escopo a alteração autenticada de senha, página visual, recuperação
administrativa/MFA, desabilitação global e provider SMTP de produção.

## 5. Rollback

Não há nova migration nesta subfatia. Antes de produção, rollback é o revert do
PR; challenges já consumidos não são reativados automaticamente.

## 6. Evidências

- após a recuperação de contexto, as fontes normativas, ADRs, roadmap,
  pendências e estado da branch foram relidos; nenhuma divergência ou decisão
  bloqueante foi encontrada;
- o primeiro teste do caso de uso falhou na compilação pela ausência dos
  contratos de conclusão; o teste HTTP seguinte falhou pela ausência da rota,
  do erro específico de senha e do wiring;
- testes unitários demonstram consumo antes do efeito externo, rejeição genérica,
  limite de tentativas do aggregate, senha inválida sem consumo, redação e
  permanência em `USED` quando o motor falha;
- testes do adapter demonstram a ordem `logout` antes de `reset-password`,
  recusa quando UUID/e-mail/credencial não correspondem e ausência de reset se a
  revogação falhar;
- o primeiro teste real do Keycloak ficou vermelho porque eventos
  administrativos ainda estavam desabilitados; a baseline passou a habilitá-los
  com detalhes de representação desabilitados;
- Keycloak 26.7 real demonstrou rejeição da senha anterior, aceitação da nova,
  falha do refresh token emitido antes da recuperação e evento administrativo
  sem nenhuma das senhas;
- PostgreSQL 17 real demonstrou o challenge persistido como `USED` e a entrega
  `PASSWORD_CHANGED` criada somente depois do reset; a suíte existente com
  Mailpit 1.30.6 preserva a entrega SMTP real da confirmação;
- `python3 -m unittest scripts/test_local_dev.py`: cinco testes verdes no WSL;
- `.\gradlew.bat clean build`: verde em 1m48s no Windows, incluindo `bootJar`,
  Checkstyle, JaCoCo, ArchUnit e regressões, com suites de container ignoradas
  pela ausência de Docker direto no host;
- `./gradlew clean build`: verde em 10m18s no Linux/WSL com 253 testes, zero
  falhas, erros ou ignorados, usando PostgreSQL, Keycloak e Mailpit reais;
- `git diff --check` ficou verde e os artefatos `bootJar` e JAR foram produzidos
  fora do versionamento.

## 7. Segurança, operação e próximo passo

Não há transação distribuída nem armazenamento da nova senha. O endpoint somente
retorna sucesso após o reset e o registro durável da confirmação. Se uma falha
ocorrer depois do consumo, a prova não é reativada; nova solicitação é necessária.
Isso privilegia uso único, revogação e falha fechada em vez de retry inseguro com
credencial humana.

Nenhuma migration foi adicionada. Antes de produção, rollback é o revert do PR;
challenges consumidos não são reativados. `PD-001`, `PD-002` e `PD-003`
permanecem adiáveis e não bloqueiam esta prova local. A próxima subfatia de
lifecycle deve ser definida pela sequência aprovada sem antecipar alteração
autenticada de senha, página visual ou recuperação de MFA administrativo.
