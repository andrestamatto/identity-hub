# IdentityHub - Especificação de Domínio e Regras de Negócio

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
- `UsernameType`: tipo de identificador de login suportado pelo core (`EMAIL`, `PHONE`, `EXTERNAL_ID`).
- `PasswordHash`: senha armazenada somente em formato seguro.
- `UserStatus`: estado da conta (`ACTIVE`, `LOCKED`, `DISABLED`, `PENDING_VERIFICATION`).
- `Role`: papel de acesso (ex.: `ADMIN`, `USER`).
- `Permission`: capacidade granular (ex.: `user:read`, `user:write`).
- `AuthSession`: sessão/autenticação ativa com ciclo de vida próprio.
- `LoginAttempt`: evento de tentativa de login (sucesso/falha) para auditoria.

## 4. Regras de Domínio

### 4.1 Usuário
1. Todo `User` possui identidade única estável (`UserId`).
2. `username` é obrigatório e único no contexto configurado do consumidor.
3. `password` nunca é persistida em texto puro; somente `PasswordHash`.
4. Apenas usuários `ACTIVE` podem autenticar.
5. `DISABLED` nunca autentica até reativação explícita.

### 4.1.1 Regras de Identificador (`UsernameType`)
1. O core suporta nativamente `EMAIL`, `PHONE` e `EXTERNAL_ID`.
2. O projeto consumidor deve configurar quais `UsernameType` são permitidos na aplicação.
3. A validação de formato do identificador é responsabilidade de domínio e atualmente está encapsulada em `UsernameType`.
4. `EXTERNAL_ID` representa identificadores externos/custom do consumidor, com validação mínima de não nulo e não vazio.

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
2. Se o status inicial for `PENDING_VERIFICATION`, o consumidor deve prover fluxo de verificação compatível com os `UsernameType` permitidos.
3. Se o status inicial for `ACTIVE`, o fluxo de verificação pode ser ignorado por configuração.

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

### IH-002 - Autenticação Base
- Valida credenciais e status do usuário.
- Se status do usuário é PENDING_VERIFICATION (recém registrado) → envia código de validação (EMAIL ou PHONE):
- Se status do usuário é ACTIVE, prossegue para validação de senha.
- Critérios:
  - sucesso retorna tokens;
  - usuário inexistente ou senha inválida retorna falha genérica.

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
