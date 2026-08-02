# SLICE-006A — Solicitação durável de recuperação de senha

> **Status:** completed under autonomous delivery governance
>
> **Data:** 2026-08-02
>
> **Branch:** `feat/slice-006a-password-recovery-request`

## 1. Resultado observável

Uma pessoa pode solicitar recuperação a partir da experiência hospedada de uma
aplicação. A resposta pública não revela se a conta existe. Para uma identidade
local habilitada, com e-mail verificado e pertencente ao ambiente, o IdentityHub
registra uma prova de finalidade única e uma entrega de e-mail na mesma transação.

## 2. Rastreabilidade

- `identityhub-spec.md`: início de `IH-MVP-015` e `IH-MVP-018`;
- `architecture.md`: módulos `identity`/`communication` e fluxo 14.5;
- `security-model.md`: seções 11, 20, 21.1, 21.2 e 23;
- ADR-0001, ADR-0002, ADR-0004 e ADR-0010.

## 3. Critérios de aceitação

- conta elegível e conta inexistente recebem exatamente o mesmo status, corpo e
  cabeçalhos públicos;
- somente identidade local habilitada e com e-mail verificado gera recuperação;
- a prova usa 256 bits aleatórios; o challenge persiste apenas SHA-256, expira em
  15 minutos e possui finalidade exclusiva; o link fica transitoriamente no
  outbox somente enquanto a entrega está pendente;
- nova solicitação invalida a prova ativa anterior;
- destino aceita no máximo três solicitações em 15 minutos e a borda aceita no
  máximo vinte por IP em 15 minutos, sem revelar a causa;
- challenge e entrega de e-mail são persistidos atomicamente;
- o link usa somente o domínio público configurado e mantém a prova no fragmento;
- segredo utilizável não aparece em resposta, log, auditoria ou representação de
  objetos e é apagado do outbox depois da entrega;
- falha transitória de SMTP permanece recuperável pelo worker já existente;
- testes reais usam PostgreSQL, Keycloak e Mailpit descartáveis.

## 4. Limites

Esta fatia não aceita a nova senha, não altera credencial, não revoga sessão e não
implementa alteração autenticada de senha. Esses efeitos exigem validação e consumo
atômico da prova e serão tratados na próxima subfatia. Também não implementa página
visual, CAPTCHA, proxy confiável ou provedor SMTP de produção.

## 5. Segurança, simplicidade e rollback

O SaaS consumidor continua sem receber senha ou prova. O fluxo não cria um
protocolo de autenticação: ele implementa a capacidade de recuperação definida no
produto, delegando a credencial ao Keycloak e a entrega ao outbox já existente.
Não será criada abstração genérica de challenge antes de existir duplicação estável.

Rollback antes de produção é o revert do PR. A migration será somente aditiva;
nenhuma tabela, conta, sessão ou credencial existente será removida.

## 6. Evidências

- o primeiro teste de domínio falhou por inexistência de
  `PasswordRecoveryChallenge` antes da implementação;
- o primeiro teste HTTP do novo caminho falhou sob o default-deny antes da
  inclusão exclusiva do método e rota permitidos;
- testes focados no Windows demonstraram domínio, orquestração, lookup Keycloak,
  anti-enumeração, limites, validação estrita e segurança HTTP;
- PostgreSQL 17 e Mailpit 1.30.6 reais demonstraram hash persistido, transação
  challenge/outbox, rollback integral, supersessão, três solicitações por destino,
  entrega SMTP e remoção do link após sucesso;
- Keycloak 26.7 real demonstrou que somente conta habilitada, verificada e com
  credencial `password` é elegível; a busca resumida foi usada apenas para obter o
  UUID e a decisão foi tomada a partir dos endpoints oficiais de usuário completo
  e credenciais;
- `./gradlew.bat clean build --console=plain`: verde em 1m35s, incluindo
  `bootJar`, Checkstyle, JaCoCo e regressões; 239 testes, zero falhas/erros e 37
  Testcontainers ignorados por ausência de Docker direto no host;
- `./gradlew clean build --console=plain`: verde em 10 minutos no Linux/WSL com
  239 testes, zero falhas, erros ou ignorados, usando PostgreSQL, Keycloak e
  Mailpit reais;
- `git diff --check` ficou verde; a varredura da árvore ativa não encontrou
  material de chave ou token real, e o diff não contém ambiente local, log, IDE
  ou artefato gerado.

## 7. Operação, rollback e próximo passo

A V14 é aditiva. Antes de produção, rollback é o revert do PR. Depois de aplicada,
a tabela pode permanecer inativa até correção posterior; removê-la exigiria
inspeção explícita dos ambientes e autorização destrutiva, não concedida por este
mandato.

`PD-001` continua impedindo envio produtivo até a escolha do SMTP, e `PD-002`
continua exigindo proxy confiável/rate limiting de edge em staging. Nenhuma delas
bloqueia a prova local. A próxima subfatia deve validar e consumir a prova, alterar
a credencial no Keycloak, revogar sessões e enfileirar a confirmação de segurança;
esta fatia deliberadamente não aceita nova senha.
