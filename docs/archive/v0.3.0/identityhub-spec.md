# IdentityHub - Especificação de Domínio e Regras de Negócio

> Documento histórico da baseline `v0.3.0`. Não representa a especificação vigente.

## 1. Objetivo
IdentityHub é um sistema de autenticação e autorização para APIs, com foco em segurança, rastreabilidade e evolução incremental guiada por TDD.

## 1.1 Modos de Distribuição
- `Embedded Mode` (prioritário no MVP): IdentityHub como biblioteca/starter incorporada ao projeto consumidor.
- `Service Mode` (fase posterior): IdentityHub como microsserviço independente com contratos de integração estáveis.

## 2. Escopo Inicial (MVP - Embedded Mode)
- Cadastro de usuário.
- Autenticação por credenciais.
- Bloqueio por tentativas inválidas.
- Autorização baseada em roles e permissions.
- Emissão de token de acesso.
- Refresh token e logout seguro.
- Configuração mínima via `application.yml` no projeto consumidor.

## 3. Linguagem Ubíqua
- `User`: conta autenticável do sistema.
- `Credentials`: dados de login (identificador + segredo em claro no input).
- `Username`: identificador principal usado para autenticação/login.
- `UsernameType`: tipo real do identificador principal de login suportado pelo core (`EMAIL`, `PHONE`).
- `Contact`: meio de contato associado ao usuário para notificações e recuperação de conta, distinto do `Username`.
- `ContactType`: tipo de contato (`EMAIL`, `PHONE`).
- `PasswordHash`: senha armazenada somente em formato seguro.
- `UserStatus`: estado da conta (`ACTIVE`, `LOCKED`, `DISABLED`, `PENDING_VERIFICATION`).
- `Role`: papel de acesso (ex.: `ADMIN`, `USER`).
- `Permission`: capacidade granular (ex.: `user:read`, `user:write`).
- `AuthSession`: sessão/autenticação ativa com ciclo de vida próprio.
- `LoginAttempt`: evento de tentativa de login (sucesso/falha) para auditoria.
- `VerificationToken`: código temporário usado para confirmar registro antes da ativação da conta.
- `NotificationMethod`: meio lógico usado para enviar notificações de verificação (`EMAIL`, `SMS`, `BOTH`).
- `NotificationChannel`: canal lógico de entrega de mensagem (`EMAIL`, `SMS`, `WHATSAPP`).
- `Outbox`: registro persistente de mensagem/evento pendente de entrega, usado para retry e rastreabilidade.

## 4. Regras de Domínio

### 4.1 Usuário
1. Todo `User` possui identidade única estável (`UserId`).
2. `username` é obrigatório e único no contexto configurado do consumidor.
3. `password` nunca é persistida em texto puro; somente `PasswordHash`.
4. Apenas usuários `ACTIVE` podem autenticar.
5. `DISABLED` nunca autentica até reativação explícita.

### 4.1.1 Regras de Identificador (`UsernameType`)
1. O core suporta nativamente `EMAIL` e `PHONE` como tipos reais de `Username`.
2. `UsernameType` representa o tipo identificado e persistido do `Username`, não uma política de aceite.
3. A resolução de input bruto para `Username` ocorre por porta de aplicação (`UsernameResolver`) e adapters de infraestrutura.
4. O domínio não deve depender de bibliotecas externas de parsing/normalização de email ou telefone.
5. Identificadores externos/federados devem ser modelados futuramente como conceito próprio (ex.: `ExternalIdentity`), não como `UsernameType`.

### 4.1.2 Contatos do Usuário (Feature Futura)
1. `Username` e `Contact` são conceitos distintos:
   - `Username` identifica o usuário para autenticação/login;
   - `Contact` representa meios pelos quais o usuário pode ser contactado/notificado.
2. Um `User` pode ter zero ou mais contatos adicionais além do `Username`.
3. Contatos sensíveis devem possuir estado de verificação antes de serem usados para notificações de segurança.
4. Deve ser possível registrar, no futuro, mais de um contato por tipo (ex.: email principal/secundário, telefone para SMS/WhatsApp).
5. O `Username` usado no registro inicial pode originar um `Contact` verificado ou pendente de verificação, conforme o fluxo de confirmação aplicável.
6. Notificações futuras devem poder selecionar canal e contato de destino com base em contatos cadastrados, verificados e preferenciais.
7. Essa feature não faz parte do escopo atual de `IH-001`/`IH-002`.

### 4.2 Senha
1. Hash deve usar algoritmo forte (BCrypt ou equivalente).
2. Comparação de senha sempre ocorre via `PasswordEncoder`.
3. Alteração de senha atualiza `passwordChangedAt`.

### 4.3 Tentativas de Login e Bloqueio
1. A cada credencial inválida, registrar falha de autenticação.
2. Ao atingir `MAX_FAILED_ATTEMPTS` dentro de `FAILED_WINDOW`, o usuário é bloqueado.
3. Bloqueio dura `LOCK_DURATION`.
4. Enquanto `lockedUntil > now`, autenticação deve falhar por bloqueio.
5. Login com sucesso zera o contador de falhas e remove bloqueio ativo.

> Parâmetros iniciais sugeridos:
>- `MAX_FAILED_ATTEMPTS = 5`
>- `FAILED_WINDOW = 10 minutos`
>- `LOCK_DURATION = 15 minutos`

### 4.4 Autorização
1. Toda autorização é derivada do conjunto de `roles` e `permissions` do usuário.
2. Endpoints protegidos devem negar acesso sem autenticação.
3. Endpoints com regra de autorização devem validar permissões mínimas exigidas.

### 4.5 Sessão e Token
1. Autenticação válida gera `AccessToken` com expiração curta.
2. `RefreshToken` tem ciclo de vida separado e pode ser revogado.
3. Logout deve invalidar sessão/token de refresh ativo.
4. Token expirado ou inválido nunca concede acesso.
5. No `Embedded Mode`, o projeto consumidor define parâmetros de expiração por configuração.

### 4.6 Auditoria e Segurança
1. Registrar `LoginAttempt` com timestamp UTC (`Instant`) e resultado.
2. Não expor detalhes sensíveis em mensagens de erro de autenticação.
3. Mensagens externas devem ser genéricas (ex.: "credenciais inválidas").

### 4.7 Política de Ativação Inicial
1. O status inicial de registro deve ser configurável pelo projeto consumidor.
2. Se o status inicial for `PENDING_VERIFICATION`, o consumidor deve prover fluxo de verificação compatível com o `UsernameType` resolvido.
3. Se o status inicial for `ACTIVE`, o fluxo de verificação pode ser ignorado por configuração.

### 4.8 Verificação de Registro (IH-002)
1. Quando o usuário é registrado com `PENDING_VERIFICATION`, um `VerificationToken` deve ser gerado e associado ao usuário.
2. `VerificationToken` ativo contém: `code`, `method` (`EMAIL`/`SMS`) e `expiresAt`.
3. O endpoint de confirmação valida `username` + `verificationCode`.
4. Na confirmação bem-sucedida:
   - o status do usuário deve ser atualizado para `ACTIVE`;
   - o `verificationToken` ativo deve ser removido (`null`);
   - um evento de domínio/aplicação de confirmação deve ser publicado;
   - a API deve retornar o usuário ativado em uma resposta `UserResponse`.
5. Usuário inexistente, status incompatível, token expirado ou código divergente devem resultar em falha de confirmação.
6. A geração de token não deve depender de `Instant.now()` dentro do VO; tempo e aleatoriedade devem ser fornecidos por portas/adapters para manter previsibilidade de testes.
7. Quando o token de verificação usa `EMAIL`, o sistema deve enviar um e-mail com o código de confirmação.
8. Após confirmação bem-sucedida, o sistema deve enviar um e-mail de boas-vindas ao usuário ativado.
9. As notificações atuais são assíncronas e executadas após commit da transação.
10. SMS existe como contrato/canal de domínio, mas envio real por SMS permanece fora do escopo atual do IH-002.
11. Falhas assíncronas de notificação são registradas em log no escopo atual; retry/outbox é tratado como feature futura.

### 4.9 Entrega Confiável de Notificações (Feature Futura)
1. O sistema deve registrar notificações/eventos pendentes em uma outbox persistente antes da tentativa de entrega externa.
2. Cada item de outbox deve conter estado de processamento (`PENDING`, `SENT`, `FAILED`, `RETRYING`), número de tentativas, próxima tentativa e última falha conhecida.
3. Falhas temporárias de provedor (`timeout`, indisponibilidade, erro de rede) devem ser elegíveis para retry.
4. Falhas permanentes devem ser registradas e expostas para observabilidade/administração sem bloquear a confirmação do usuário já persistida.
5. A entrega deve ser idempotente para evitar envio duplicado quando houver retry.
6. Essa feature deve substituir o tratamento atual baseado apenas em log de exceção assíncrona.

## 5. Invariantes do Agregado User
- `failedLoginCount >= 0`.
- `lockedUntil` só pode estar no futuro quando status de bloqueio estiver ativo.
- `passwordHash` sempre presente para usuários autenticáveis.
- `roles` não pode ser vazio para usuários ativos (regra de autorização mínima).

## 6. Estratégia de Implementação por Features

### IH-001 - Registro de Usuário
- Criar usuário ativo com email único e senha hasheada.
- Critérios:
  - rejeita email duplicado;
  - rejeita senha inválida pelo domínio.
  - usuário registrado deve apresentar status PENDING_VERIFICATION.

### IH-002 - Confirmação de Registro por Código
- Fluxo inicial de confirmação de registro por código.
- Se status do usuário é `PENDING_VERIFICATION`, deve existir token válido para confirmação.
- Registro pendente por `EMAIL` envia e-mail assíncrono com código de confirmação.
- Confirmação válida ativa usuário, invalida token ativo e retorna `UserResponse` com status `ACTIVE`.
- Confirmação válida publica evento de usuário confirmado.
- Usuário confirmado recebe e-mail assíncrono de boas-vindas.
- O envio real de SMS fica fora do escopo desta feature, apesar de o contrato de canal existir.
- Retry/outbox para notificações assíncronas fica fora do escopo desta feature.
- Critérios:
  - confirma usuário quando código é válido e não expirado;
  - rejeita confirmação para usuário inexistente;
  - rejeita confirmação para status incompatível;
  - rejeita confirmação para token expirado ou código inválido.
  - envia código de confirmação por e-mail quando o método é `EMAIL`;
  - envia código de confirmação por SMS quando método é `PHONE`;
  - envia e-mail de boas-vindas após confirmação bem-sucedida;
  - retorna usuário ativo após confirmação bem-sucedida.

### IH-003 - Bloqueio por Tentativas
- Aplica política de lock por falhas consecutivas na janela.
- Critérios:
  - bloqueia no limite;
  - impede login durante lock;
  - desbloqueia após expiração.

### IH-004 - Autorização RBAC
- Restringe endpoints por role/permission.
- Critérios:
  - sem token -> 401;
  - sem permissão -> 403;
  - com permissão -> acesso liberado.

### IH-005 - Refresh e Logout
- Renovação de acesso e revogação de sessão.
- Critérios:
  - refresh válido gera novo access token;
  - refresh revogado/expirado falha;
  - logout invalida refresh ativo.

### IH-006 - Entrega Confiável de Notificações
- Implementa outbox/retry para notificações assíncronas.
- Critérios:
  - registra notificação pendente antes da entrega externa;
  - reprocessa falhas temporárias com política de retry;
  - marca sucesso/falha final de entrega;
  - evita envio duplicado em reprocessamentos.

### IH-007 - Contatos do Usuário
- Introduz contatos verificáveis associados ao usuário, separados do identificador principal de login.
- Critérios:
  - permite cadastrar contatos do tipo `EMAIL` e `PHONE`;
  - impede uso de contatos não verificados para notificações sensíveis;
  - permite marcar contato principal/preferencial por tipo ou canal;
  - permite notificar por SMS/WhatsApp usuários cujo `Username` seja email, desde que exista telefone verificado;
  - mantém `Username` como identificador principal de autenticação, sem transformá-lo em lista genérica de contatos.

## 7. Estratégia TDD (por feature)
1. Selecionar a feature por ID (ex.: `IH-001`).
2. Escrever testes de domínio/caso de uso (RED).
3. Implementar mínimo para passar (GREEN).
4. Refatorar sem alterar comportamento (REFACTOR).
5. Subir para testes de integração (infra/security).

## 8. Decisões Técnicas de Domínio
- Datas e timestamps: `Instant` (UTC).
- Entidade pode depender de VO.
- `LoginAttempt` é registro de auditoria; controle de lock no `User` usa estado mínimo.
- Evitar acoplamento de domínio com anotações/frameworks do Spring.
- Core de domínio/aplicação desacoplado de infraestrutura (ports/adapters).
- O projeto consumidor é fonte de verdade de usuários no MVP.
- Configuração local de SMTP/Mailpit deve seguir o guia em `guides/mailpit.md`.

## 9. Evolução por Versão
- `v0.x`: Embedded Mode com JWT, RBAC, lockout, refresh/logout e starter Spring Boot.
- `v1.x`: evolução da biblioteca com OAuth2/OIDC básico, observabilidade e contratos estáveis.
- `v2.x`: Service Mode para execução independente (REST/gRPC), incluindo estratégia de storage para sessão/revogação/auditoria.
- `v3.x`: plataforma completa com painel administrativo e recursos avançados (MFA e federação).

## 10. Fora do Escopo do MVP
- MFA.
- OAuth2/OpenID Connect completos.
- Gestão multi-tenant avançada.
- Device fingerprinting e risk engine.
