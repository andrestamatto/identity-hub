# SLICE-004B — Provisionar identidade local pendente

> **Status:** completed under autonomous delivery governance
>
> **Data:** 2026-07-31
>
> **Branch:** `feat/slice-004b-pending-local-identity`
>
> **Base:** `develop` em `07a7e81`

## 1. Objetivo

Provar o núcleo seguro do cadastro local de `IH-MVP-003`: após confirmar que a
aplicação habilitou autocadastro, o IdentityHub cria ou reconhece uma identidade
global no Keycloak sem persistir credencial ou perfil humano em seu próprio banco.

## 2. Resultado observável

- e-mail válido possui forma normalizada estável para identidade;
- senha aplica o baseline mínimo antes de alcançar o motor;
- aplicação com política `DISABLED` é negada antes de chamar o Keycloak;
- nova identidade é criada desabilitada e com e-mail não verificado;
- repetição para o mesmo e-mail retorna a mesma referência opaca sem trocar senha;
- service account exclusiva de identidade possui somente permissões de usuários;
- adapter usa apenas Admin REST pública e nunca o schema do Keycloak.

## 3. Invariantes

- Keycloak é proprietário de credencial, perfil operacional e referência global;
- o adaptador envia o valor de contato validado, mas aceita a canonicalização em
  minúsculas realizada pelo Keycloak 26.7;
- IdentityHub trabalha somente com `UserAccountRef` opaca;
- senha em claro existe apenas durante a chamada necessária, não possui `toString`
  significativo e nunca entra em banco, evento, log ou resposta;
- conta pendente não pode autenticar antes da verificação futura;
- e-mail coincidente não altera credencial nem concede membership;
- erro público futuro deverá ser genérico; esta fatia não expõe endpoint público;
- identidade técnica de clients continua sem acesso a usuários.

## 4. Falhas e bordas

- política de autocadastro ausente/desabilitada nega;
- e-mail inválido e senha fraca falham antes do efeito remoto;
- conflito concorrente no Keycloak é relido de forma idempotente;
- múltiplos usuários para o mesmo e-mail normalizado são conflito permanente;
- indisponibilidade, autenticação técnica ou resposta inválida são classificadas
  sem incluir corpo remoto ou credencial no erro.

## 5. Fora de escopo

- endpoint ou formulário público;
- prova e entrega de verificação de e-mail;
- habilitação da conta;
- resposta anti-enumeração e equalização temporal na borda pública;
- rate limit, captcha ou WAF;
- membership, onboarding proof, login ou token;
- alteração e recuperação de senha.

## 6. Evidência

- testes unitários cobrem normalização de e-mail, política de senha, descarte de
  buffers, negação por política, idempotência, falha remota e conflito;
- teste com HTTP falso prova representação desabilitada/não verificada, replay
  sem segundo `POST` e mensagens sem corpo remoto;
- Testcontainers com Keycloak 26.7 e PostgreSQL 17 criou e releu a identidade
  real sem skip e provou isolamento bidirecional das service accounts;
- bootstrap local foi aplicado com sucesso sobre realm preexistente, incluindo
  cliente técnico, roles mínimas e política de senha;
- Windows: `.\gradlew.bat clean build` — verde em 1m17s, incluindo `bootJar`,
  Checkstyle, ArchUnit e regressões; 180 testes, zero falhas/erros e 24
  Testcontainers ignorados por ausência de Docker direto no host;
- Linux/WSL: `./gradlew clean build` — verde em 6m49s com os 180 testes, zero
  falhas, erros ou ignorados, usando Keycloak, PostgreSQL e Mailpit reais;
- harness: `python3 -m unittest scripts/test_local_dev.py` — 3 testes verdes;
- bootstrap local idempotente: `local-env up` verde sobre o realm preexistente.

## 7. Gate autônomo

A fatia satisfaz seu contrato interno e está autorizada para publicação segundo
`docs/autonomous-delivery.md`. Ela não torna o cadastro público: o próximo
incremento deve adicionar prova de e-mail e controles antiabuso antes de expor
qualquer entrada de senha à internet.
