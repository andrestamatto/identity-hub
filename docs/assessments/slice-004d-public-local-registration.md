# SLICE-004D — Borda pública de cadastro local

> **Status:** completed under autonomous delivery governance
>
> **Data:** 2026-08-01
>
> **Branch:** `feat/slice-004d-public-local-registration`
>
> **Base:** `develop` em `09e999a`

## 1. Objetivo

Expor ao navegador a menor borda HTTP segura para iniciar o cadastro local e
confirmar a verificação de e-mail já implementados em `IH-MVP-003` e
`IH-MVP-004`, sem antecipar o tema hospedado, branding, `Membership` ou prova de
onboarding.

## 2. Contrato observável

- `POST /public/v1/applications/{applicationIdentifier}/local-registrations`
  recebe somente `email` e `password` e responde `202 Accepted` com mensagem
  genérica quando a solicitação puder ser processada;
- a aplicação é selecionada por seu identificador lógico público e o UUID
  interno não atravessa a borda;
- conta nova e conta preexistente produzem exatamente o mesmo status, corpo e
  cabeçalhos públicos;
- senha fora da política produz erro corrigível sem ecoar senha ou e-mail;
- aplicação inexistente ou sem autocadastro produz a mesma resposta
  genérica de cadastro indisponível;
- `POST /public/v1/email-verifications` recebe somente o token no corpo e não em
  parâmetros de URL; sucesso responde sem dados da conta e qualquer prova
  inválida produz a mesma falha genérica;
- o link entregue por e-mail mantém a prova no fragmento, que não atravessa a
  requisição HTTP; a futura página hospedada deve removê-lo do histórico antes
  de submetê-lo no corpo;
- nenhuma operação cria `Membership`, emite token ou devolve referências de
  usuário e challenge.

Os endpoints são uma borda destinada à futura experiência hospedada do
IdentityHub, não um contrato para que aplicações consumidoras coletem senhas.
O formulário e o tema permanecem em fatia própria, conforme `IH-MVP-008`.

## 3. Proteção contra abuso

- cadastro é limitado inicialmente a 20 solicitações em 15 minutos por endereço
  remoto, além das três solicitações em 15 minutos por destino já impostas pelo
  núcleo;
- verificação preserva o máximo de cinco tentativas por challenge já imposto
  pelo domínio;
- a chave de IP permanece somente em memória e não vira dado persistente;
- o limite é configurável, mas possui valores seguros e validação de startup;
- `Retry-After` é estável e não revela existência de conta;
- o serviço utiliza o endereço remoto entregue pelo servidor HTTP e não confia
  diretamente em `X-Forwarded-For`;
- a topologia de proxy confiável será configurada no ambiente antes da exposição
  produtiva; até lá, o comportamento conservador agrega requisições pelo proxy
  em vez de aceitar um cabeçalho falsificável.

## 4. Anti-enumeração e falhas

- respostas públicas nunca incluem causa do Keycloak, IDs internos, e-mail,
  token, stack trace ou estado da conta;
- o caminho de cadastro aplica duração pública mínima comum para reduzir a
  diferença observável entre identidade nova e existente;
- indisponibilidade transitória retorna resposta sanitizada e não é apresentada
  como cadastro concluído;
- corpo acima do limite, JSON inválido, campo ausente, campo duplicado ou campo
  desconhecido falha antes do caso de uso;
- o correlation ID validado acompanha a operação sem incorporar dado sensível.

## 5. Limites arquiteturais

- `identity.adapter.in.http` possui apenas tradução HTTP e controles próprios da
  borda;
- resolução de `ApplicationIdentifier` usa contrato público pequeno do módulo
  `clientapplication`;
- invariantes de senha, política, challenge e verificação continuam no domínio e
  na aplicação;
- Keycloak permanece escondido atrás de seu adapter e não aparece no contrato;
- o default-deny do Spring Security abre somente métodos e caminhos explícitos.

## 6. Fora de escopo

- página, tema, branding e acessibilidade visual;
- login e emissão de tokens;
- `OnboardingIdentityProof` e `Membership`;
- telefone e provedores sociais;
- CAPTCHA, WAF e rate limiting distribuído;
- confiança automática em cabeçalhos de proxy;
- provedor SMTP de produção (`PD-001`).

## 7. Evidência

Os testes devem demonstrar:

- igualdade de resposta entre conta nova e existente;
- ausência de IDs e dados sensíveis na resposta e em representações textuais;
- validação estrita de schema e tamanho;
- limite por IP, `Retry-After`, expiração da janela e isolamento entre IPs;
- endereço forjado em `X-Forwarded-For` sem efeito por padrão;
- senha inválida corrigível e cadastro desabilitado sanitizado;
- confirmação válida, inválida, expirada e replay com respostas seguras;
- autorização negativa de todos os demais métodos e caminhos públicos;
- regressão completa, `bootJar`, PostgreSQL, Keycloak e Mailpit reais.

O ciclo TDD apresentou vermelho intencional para resolução por identificador,
rate limiting, contrato HTTP, timing mínimo, configuração segura e retirada do
token da query string antes das respectivas implementações.

Gates executados em 2026-08-01:

- Windows: `.\gradlew.bat clean build` — verde em 1m27s, incluindo `bootJar`,
  Checkstyle, ArchUnit, JaCoCo e regressões; 221 testes, zero falhas/erros e 30
  Testcontainers ignorados por ausência de Docker direto no host;
- Linux/WSL: `./gradlew clean build` — verde em 9m56s com 221 testes, zero
  falhas, erros ou ignorados, usando PostgreSQL 17.10, Keycloak 26.7.0 e Mailpit
  1.30.6 reais;
- harness: `python3 -m unittest scripts/test_local_dev.py` — quatro testes
  verdes;
- bootstrap local: `local-env up` e aplicação até Flyway V10 ficaram verdes;
  o smoke administrativo não foi repetido porque a sessão TOTP local estava
  expirada. Não houve bypass: o percurso real equivalente permaneceu coberto
  pelo gate WSL/Testcontainers.

O diff não contém migration. A borda permanece desabilitada por padrão e exige
habilitação explícita. `PD-002` registra a configuração adiável do proxy
confiável antes de qualquer exposição produtiva.

## 8. Rollback

Antes de produção, rollback é revert do PR. A fatia não adicionará migration nem
limpeza de dados: desafios e mensagens já persistidos pela V10 permanecem sob as
regras existentes.

Rollback também desabilita imediatamente a borda por
`IDENTITYHUB_PUBLIC_IDENTITY_ENABLED=false`, antes do revert, sem alterar dados.
