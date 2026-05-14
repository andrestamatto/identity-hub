# IdentityHub - Especificação de Domínio e Regras de Negócio

## 1. Objetivo
IdentityHub é um sistema de autenticação e autorização para APIs, com foco em segurança, rastreabilidade e evolução incremental guiada por TDD.

## 2. Escopo Inicial (MVP)
- Cadastro de usuário.
- Autenticação por credenciais.
- Bloqueio por tentativas inválidas.
- Autorização baseada em roles e permissions.
- Emissão de token de acesso.
- Refresh token e logout seguro.

## 3. Linguagem Ubíqua
- `User`: conta autenticável do sistema.
- `Credentials`: dados de login (identificador + segredo em claro no input).
- `PasswordHash`: senha armazenada somente em formato seguro.
- `UserStatus`: estado da conta (`ACTIVE`, `LOCKED`, `DISABLED`, `PENDING_VERIFICATION`).
- `Role`: papel de acesso (ex.: `ADMIN`, `USER`).
- `Permission`: capacidade granular (ex.: `user:read`, `user:write`).
- `AuthSession`: sessão/autenticação ativa com ciclo de vida próprio.
- `LoginAttempt`: evento de tentativa de login (sucesso/falha) para auditoria.

## 4. Regras de Domínio

### 4.1 Usuário
1. Todo `User` possui identidade única estável (`UserId`).
2. `email` é obrigatório e único.
3. `password` nunca é persistida em texto puro; somente `PasswordHash`.
4. Apenas usuários `ACTIVE` podem autenticar.
5. `DISABLED` nunca autentica até reativação explícita.

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

### 4.6 Auditoria e Segurança
1. Registrar `LoginAttempt` com timestamp UTC (`Instant`) e resultado.
2. Não expor detalhes sensíveis em mensagens de erro de autenticação.
3. Mensagens externas devem ser genéricas (ex.: "credenciais inválidas").

## 5. Invariantes do Agregado User
- `failedLoginCount >= 0`.
- `lockedUntil` só pode estar no futuro quando status de bloqueio estiver ativo.
- `passwordHash` sempre presente para usuários autenticáveis.
- `roles` não pode ser vazio para usuários ativos (regra de autorização mínima).

## 6. Estratégia de Implementação por Features

### Feature 1 - Registro de Usuário
- Criar usuário ativo com email único e senha hasheada.
- Critérios:
  - rejeita email duplicado;
  - rejeita senha inválida pelo domínio.

### Feature 2 - Autenticação Base
- Valida credenciais e status do usuário.
- Critérios:
  - sucesso retorna tokens;
  - usuário inexistente ou senha inválida retorna falha genérica.

### Feature 3 - Bloqueio por Tentativas
- Aplica política de lock por falhas consecutivas na janela.
- Critérios:
  - bloqueia no limite;
  - impede login durante lock;
  - desbloqueia após expiração.

### Feature 4 - Autorização RBAC
- Restringe endpoints por role/permission.
- Critérios:
  - sem token -> 401;
  - sem permissão -> 403;
  - com permissão -> acesso liberado.

### Feature 5 - Refresh e Logout
- Renovação de acesso e revogação de sessão.
- Critérios:
  - refresh válido gera novo access token;
  - refresh revogado/expirado falha;
  - logout invalida refresh ativo.

## 7. Estratégia TDD (por feature)
1. Escrever testes de domínio (RED).
2. Implementar mínimo para passar (GREEN).
3. Refatorar sem alterar comportamento (REFACTOR).
4. Subir para testes de caso de uso.
5. Subir para testes de integração (infra/security).

## 8. Decisões Técnicas de Domínio
- Datas e timestamps: `Instant` (UTC).
- Entidade pode depender de VO.
- `LoginAttempt` é registro de auditoria; controle de lock no `User` usa estado mínimo.
- Evitar acoplamento de domínio com anotações/frameworks do Spring.

## 9. Fora do Escopo do MVP
- MFA.
- OAuth2/OpenID Connect completos.
- Gestão multi-tenant avançada.
- Device fingerprinting e risk engine.

