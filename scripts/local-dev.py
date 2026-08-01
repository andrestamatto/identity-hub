#!/usr/bin/env python3
"""Local IdentityHub harness. It never prints credentials or bearer tokens."""

from __future__ import annotations

import argparse
import json
import os
import pathlib
import secrets
import subprocess
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid


REALM = "identityhub-development"
ADMIN_CLIENT = "identityhub-local-admin"
ADMIN_AUDIENCE = "identityhub-admin-api"
MANAGEMENT_CLIENT = "identityhub-management"
ADMIN_ROLES = ("PLATFORM_ADMIN", "PLATFORM_AUDITOR")


def read_env(path: pathlib.Path) -> dict[str, str]:
    values: dict[str, str] = {}
    for number, raw_line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        line = raw_line.strip()
        if not line or line.startswith("#"):
            continue
        if line.startswith("export "):
            line = line[7:].strip()
        if "=" not in line:
            raise RuntimeError(f"Linha inválida no arquivo de ambiente: {number}")
        key, value = line.split("=", 1)
        value = value.strip()
        if len(value) >= 2 and value[0] == value[-1] and value[0] in "\"'":
            value = value[1:-1]
        values[key.strip()] = value
    return values


def required(env: dict[str, str], name: str) -> str:
    value = env.get(name, "").strip()
    if not value:
        raise RuntimeError(f"Variável obrigatória ausente: {name}")
    return value


def run(command: list[str], *, cwd: pathlib.Path, env: dict[str, str]) -> None:
    subprocess.run(command, cwd=cwd, env=env, check=True)


def compose_command(args: argparse.Namespace, env: dict[str, str]) -> list[str]:
    probe = subprocess.run(
        ["docker", "compose", "version"],
        env=env,
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL,
        check=False,
    )
    if probe.returncode == 0:
        return ["docker", "compose"]
    return [
        "docker",
        "run",
        "--rm",
        "-v",
        "/var/run/docker.sock:/var/run/docker.sock",
        "-v",
        f"{args.repository / 'compose.local.yml'}:/workspace/compose.local.yml:ro",
        "-v",
        f"{args.env_file}:/workspace/local.env:ro",
        "docker:29.1.3-cli",
        "docker",
        "compose",
    ]


def compose(action: list[str], args: argparse.Namespace, env: dict[str, str]) -> None:
    command = compose_command(args, env)
    if command[:2] == ["docker", "compose"]:
        configuration = ["--env-file", str(args.env_file), "-f", "compose.local.yml"]
    else:
        configuration = ["--env-file", "/workspace/local.env", "-f", "/workspace/compose.local.yml"]
    run(
        [*command, *configuration, *action],
        cwd=args.repository,
        env=env,
    )


def request_json(
    url: str,
    *,
    method: str = "GET",
    body: dict | list | None = None,
    token: str | None = None,
    form: dict[str, str] | None = None,
    expected: tuple[int, ...] = (200,),
) -> tuple[int, dict | list | None]:
    headers = {"Accept": "application/json"}
    data = None
    if body is not None:
        headers["Content-Type"] = "application/json"
        data = json.dumps(body).encode()
    elif form is not None:
        headers["Content-Type"] = "application/x-www-form-urlencoded"
        data = urllib.parse.urlencode(form).encode()
    if token:
        headers["Authorization"] = f"Bearer {token}"
    request = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(request, timeout=10) as response:
            payload = response.read()
            status = response.status
    except urllib.error.HTTPError as error:
        payload = error.read()
        status = error.code
    parsed = json.loads(payload) if payload else None
    if status not in expected:
        detail = parsed.get("error", "unexpected response") if isinstance(parsed, dict) else "unexpected response"
        raise RuntimeError(f"HTTP {status}: {detail}")
    return status, parsed


def keycloak_url(env: dict[str, str]) -> str:
    return f"http://127.0.0.1:{required(env, 'IDENTITYHUB_KEYCLOAK_PORT')}"


def wait_for_keycloak(env: dict[str, str]) -> None:
    endpoint = f"{keycloak_url(env)}/realms/master/.well-known/openid-configuration"
    deadline = time.monotonic() + 180
    while time.monotonic() < deadline:
        try:
            request_json(endpoint)
            return
        except (OSError, RuntimeError, json.JSONDecodeError):
            time.sleep(2)
    raise RuntimeError("Keycloak não ficou pronto dentro de 180 segundos.")


def bootstrap_token(env: dict[str, str]) -> str:
    _, response = request_json(
        f"{keycloak_url(env)}/realms/master/protocol/openid-connect/token",
        method="POST",
        form={
            "grant_type": "password",
            "client_id": "admin-cli",
            "username": required(env, "IDENTITYHUB_KEYCLOAK_BOOTSTRAP_ADMIN_USERNAME"),
            "password": required(env, "IDENTITYHUB_KEYCLOAK_BOOTSTRAP_ADMIN_PASSWORD"),
        },
    )
    assert isinstance(response, dict)
    return str(response["access_token"])


def realm_representation(env: dict[str, str]) -> dict:
    return {
        "realm": REALM,
        "enabled": True,
        "sslRequired": "none",
        "otpPolicyType": "totp",
        "otpPolicyAlgorithm": "HmacSHA1",
        "otpPolicyDigits": 6,
        "otpPolicyPeriod": 30,
        "roles": {"realm": [{"name": role} for role in ADMIN_ROLES]},
        "clients": [
            management_client_representation(env),
            {
                "clientId": ADMIN_CLIENT,
                "name": "IdentityHub local administration",
                "enabled": True,
                "publicClient": True,
                "standardFlowEnabled": True,
                "directAccessGrantsEnabled": False,
                "fullScopeAllowed": True,
                "attributes": {"oauth2.device.authorization.grant.enabled": "true"},
                "protocolMappers": [
                    {
                        "name": "identityhub-admin-audience",
                        "protocol": "openid-connect",
                        "protocolMapper": "oidc-audience-mapper",
                        "config": {
                            "included.custom.audience": ADMIN_AUDIENCE,
                            "access.token.claim": "true",
                        },
                    },
                    {
                        "name": "authentication-method-reference",
                        "protocol": "openid-connect",
                        "protocolMapper": "oidc-amr-mapper",
                        "config": {"access.token.claim": "true", "id.token.claim": "true"},
                    },
                ],
            }
        ],
        "users": [
            {
                "username": required(env, "IDENTITYHUB_LOCAL_ADMIN_USERNAME"),
                "enabled": True,
                "email": "platform-admin@identityhub.local",
                "emailVerified": True,
                "firstName": "Local",
                "lastName": "Administrator",
                "realmRoles": ["PLATFORM_ADMIN"],
                "requiredActions": ["CONFIGURE_TOTP"],
                "credentials": [
                    {
                        "type": "password",
                        "value": required(env, "IDENTITYHUB_LOCAL_ADMIN_PASSWORD"),
                        "temporary": False,
                    }
                ],
            }
        ],
    }


def management_secret_file() -> pathlib.Path:
    return pathlib.Path.home() / ".local" / "state" / "identityhub" / "management-client.secret"


def management_secret() -> str:
    path = management_secret_file()
    if not path.exists():
        path.parent.mkdir(mode=0o700, parents=True, exist_ok=True)
        path.write_text(secrets.token_urlsafe(48), encoding="utf-8")
        path.chmod(0o600)
    return path.read_text(encoding="utf-8").strip()


def management_client_representation(env: dict[str, str]) -> dict:
    return {
        "clientId": MANAGEMENT_CLIENT,
        "name": "IdentityHub internal management",
        "enabled": True,
        "publicClient": False,
        "clientAuthenticatorType": "client-secret",
        "secret": required(env, "IDENTITYHUB_KEYCLOAK_MANAGEMENT_CLIENT_SECRET"),
        "standardFlowEnabled": False,
        "directAccessGrantsEnabled": False,
        "serviceAccountsEnabled": True,
        "fullScopeAllowed": False,
    }


def client_uuid(base: str, client_id: str, token: str) -> str | None:
    _, clients = request_json(
        f"{base}/clients?clientId={urllib.parse.quote(client_id)}&exact=true",
        token=token,
    )
    assert isinstance(clients, list)
    return str(clients[0]["id"]) if clients else None


def configure_management_client(env: dict[str, str], token: str) -> None:
    base = f"{keycloak_url(env)}/admin/realms/{REALM}"
    management_uuid = client_uuid(base, MANAGEMENT_CLIENT, token)
    representation = management_client_representation(env)
    if management_uuid is None:
        request_json(
            f"{base}/clients",
            method="POST",
            token=token,
            body=representation,
            expected=(201,),
        )
        management_uuid = client_uuid(base, MANAGEMENT_CLIENT, token)
    else:
        request_json(
            f"{base}/clients/{management_uuid}",
            method="PUT",
            token=token,
            body=representation,
            expected=(204,),
        )
    if management_uuid is None:
        raise RuntimeError("O cliente interno de gerenciamento não foi criado.")

    _, service_account = request_json(
        f"{base}/clients/{management_uuid}/service-account-user", token=token
    )
    assert isinstance(service_account, dict)
    realm_management_uuid = client_uuid(base, "realm-management", token)
    if realm_management_uuid is None:
        raise RuntimeError("O cliente realm-management não foi encontrado.")
    _, manage_clients_role = request_json(
        f"{base}/clients/{realm_management_uuid}/roles/manage-clients", token=token
    )
    assert isinstance(manage_clients_role, dict)
    role_mapping = [manage_clients_role]
    request_json(
        f"{base}/users/{service_account['id']}/role-mappings/clients/{realm_management_uuid}",
        method="POST",
        token=token,
        body=role_mapping,
        expected=(204,),
    )
    request_json(
        f"{base}/clients/{management_uuid}/scope-mappings/clients/{realm_management_uuid}",
        method="POST",
        token=token,
        body=role_mapping,
        expected=(204,),
    )


def configure_amr(env: dict[str, str], token: str) -> None:
    base = f"{keycloak_url(env)}/admin/realms/{REALM}"
    _, executions = request_json(
        f"{base}/authentication/flows/browser/executions", token=token
    )
    assert isinstance(executions, list)
    references = {
        "auth-username-password-form": "pwd",
        "auth-conditional-otp-form": "totp",
        "auth-otp-form": "totp",
    }
    configured = 0
    for execution in executions:
        reference = references.get(execution.get("providerId"))
        if not reference:
            continue
        configured += 1
        if execution.get("authenticationConfig"):
            continue
        request_json(
            f"{base}/authentication/executions/{execution['id']}/config",
            method="POST",
            token=token,
            body={
                "alias": f"identityhub-amr-{reference}",
                "config": {
                    "default.reference.value": reference,
                    "default.reference.maxAge": "300",
                },
            },
            expected=(201,),
        )
    if configured < 2:
        raise RuntimeError("O fluxo do Keycloak não expôs execuções suficientes para AMR.")


def bootstrap(env: dict[str, str]) -> None:
    wait_for_keycloak(env)
    token = bootstrap_token(env)
    realm_admin_url = f"{keycloak_url(env)}/admin/realms/{REALM}"
    status, _ = request_json(realm_admin_url, token=token, expected=(200, 404))
    if status == 404:
        request_json(
            f"{keycloak_url(env)}/admin/realms",
            method="POST",
            token=token,
            body=realm_representation(env),
            expected=(201,),
        )
        configure_amr(env, token)
        print("Keycloak local configurado; o TOTP será cadastrado no primeiro login.")
    else:
        configure_amr(env, token)
        print("Keycloak local já estava configurado.")
    configure_management_client(env, token)


def up(args: argparse.Namespace, env: dict[str, str]) -> None:
    compose(["up", "-d", "identityhub-database", "keycloak", "mailpit"], args, env)
    bootstrap(env)
    print(
        "Infraestrutura local pronta. Mailpit: "
        f"http://127.0.0.1:{required(env, 'IDENTITYHUB_MAILPIT_HTTP_PORT')}"
    )


def token_file(args: argparse.Namespace) -> pathlib.Path:
    del args
    return pathlib.Path.home() / ".local" / "state" / "identityhub" / "local-admin.token"


def acquire_token(args: argparse.Namespace, env: dict[str, str]) -> None:
    bootstrap(env)
    protocol = f"{keycloak_url(env)}/realms/{REALM}/protocol/openid-connect"
    _, device = request_json(
        f"{protocol}/auth/device",
        method="POST",
        form={"client_id": ADMIN_CLIENT, "scope": "openid"},
    )
    assert isinstance(device, dict)
    print("Abra no navegador:", device.get("verification_uri_complete", device["verification_uri"]))
    print("Código de usuário:", device["user_code"])
    print("No primeiro acesso, cadastre o TOTP no Google Authenticator.")
    deadline = time.monotonic() + int(device["expires_in"])
    interval = int(device.get("interval", 5))
    while time.monotonic() < deadline:
        time.sleep(interval)
        status, response = request_json(
            f"{protocol}/token",
            method="POST",
            form={
                "grant_type": "urn:ietf:params:oauth:grant-type:device_code",
                "client_id": ADMIN_CLIENT,
                "device_code": str(device["device_code"]),
            },
            expected=(200, 400),
        )
        assert isinstance(response, dict)
        if status == 200:
            path = token_file(args)
            path.parent.mkdir(mode=0o700, parents=True, exist_ok=True)
            path.write_text(str(response["access_token"]), encoding="utf-8")
            path.chmod(0o600)
            print(f"Sessão salva com permissão restrita em {path}; o token não foi exibido.")
            return
        error = response.get("error")
        if error == "slow_down":
            interval += 5
        elif error not in ("authorization_pending", "slow_down"):
            raise RuntimeError(f"Login administrativo falhou: {error}")
    raise RuntimeError("O código de login expirou.")


def smoke(args: argparse.Namespace, env: dict[str, str]) -> None:
    path = token_file(args)
    if not path.exists():
        raise RuntimeError("Sessão ausente. Execute primeiro a ação 'token'.")
    token = path.read_text(encoding="utf-8").strip()
    application_id = uuid.uuid4()
    application_identifier = f"local-smoke-{application_id.hex[:12]}"
    endpoint = f"http://127.0.0.1:8080/internal/admin/client-applications/{application_id}"
    headers = {
        "Accept": "application/json",
        "Content-Type": "application/json",
        "Authorization": f"Bearer {token}",
        "X-Correlation-ID": str(uuid.uuid4()),
    }
    body = json.dumps(
        {"identifier": application_identifier, "displayName": "Local Smoke App"}
    ).encode()
    put_status, created = put_application(endpoint, headers, body)
    get_request = urllib.request.Request(endpoint, headers={"Authorization": f"Bearer {token}"})
    try:
        with urllib.request.urlopen(get_request, timeout=10) as response:
            found = json.loads(response.read())
    except urllib.error.HTTPError as error:
        raise RuntimeError(f"Consulta falhou com HTTP {error.code}.") from error
    replay_status, replayed = put_application(endpoint, headers, body)
    if (
        put_status != 201
        or replay_status != 200
        or created.get("applicationId") != str(application_id)
        or found != created
        or replayed != created
    ):
        raise RuntimeError("O round-trip da aplicação não preservou o contrato esperado.")
    registration_policy_status, registration_policy = put_application(
        f"{endpoint}/registration-policy",
        headers,
        json.dumps({"selfRegistration": "ENABLED"}).encode(),
    )
    if (
        registration_policy_status != 200
        or registration_policy.get("selfRegistration") != "ENABLED"
    ):
        raise RuntimeError("A política explícita de autocadastro não foi aplicada.")
    client_id = uuid.uuid4()
    client_endpoint = f"{endpoint}/clients/{client_id}"
    client_body = json.dumps(
        {
            "type": "API",
            "key": "local-smoke-api",
            "audience": f"local-smoke-{client_id}",
        }
    ).encode()
    client_status, configured = put_application(client_endpoint, headers, client_body)
    if client_status != 201 or configured.get("projectionState") != "PENDING":
        raise RuntimeError("A configuração inicial da API protegida não foi aceita.")
    projected = wait_for_projection(client_endpoint, token, "APPLIED")
    reconcile_request = urllib.request.Request(
        f"{client_endpoint}/projection/reconcile",
        data=b"",
        headers={"Authorization": f"Bearer {token}"},
        method="POST",
    )
    with urllib.request.urlopen(reconcile_request, timeout=10) as response:
        reconciled = json.loads(response.read())
        if response.status != 202 or reconciled.get("projectionState") != "PENDING":
            raise RuntimeError("A reconciliação da projeção não foi aceita.")
    wait_for_projection(client_endpoint, token, "APPLIED")
    spa_id = uuid.uuid4()
    spa_endpoint = f"{endpoint}/clients/{spa_id}"
    spa_body = json.dumps(
        {
            "type": "SPA",
            "key": "local-smoke-web",
            "redirectUris": ["http://127.0.0.1:5173/auth/callback"],
            "webOrigins": ["http://127.0.0.1:5173"],
        }
    ).encode()
    spa_status, spa_configured = put_application(spa_endpoint, headers, spa_body)
    if spa_status != 201 or spa_configured.get("projectionState") != "PENDING":
        raise RuntimeError("A configuração inicial da SPA pública não foi aceita.")
    projected_spa = wait_for_projection(spa_endpoint, token, "APPLIED")
    bff_id = uuid.uuid4()
    bff_endpoint = f"{endpoint}/clients/{bff_id}"
    bff_body = json.dumps(
        {
            "type": "BFF",
            "key": "local-smoke-bff",
            "redirectUris": [
                "http://127.0.0.1:8081/login/oauth2/code/identityhub"
            ],
        }
    ).encode()
    bff_status, bff_configured = put_application(bff_endpoint, headers, bff_body)
    if bff_status != 201 or bff_configured.get("projectionState") != "PENDING":
        raise RuntimeError("A configuração inicial do BFF confidencial não foi aceita.")
    projected_bff = wait_for_projection(bff_endpoint, token, "APPLIED")
    _, credential = request_json(
        f"{bff_endpoint}/credentials/client-secret",
        method="POST",
        token=token,
    )
    if not isinstance(credential, dict) or not credential.get("clientSecret"):
        raise RuntimeError("A credencial de uso único do BFF não foi emitida.")
    credential = None
    machine_id = uuid.uuid4()
    machine_endpoint = f"{endpoint}/clients/{machine_id}"
    machine_body = json.dumps(
        {
            "type": "MACHINE",
            "key": "local-smoke-membership-provisioner",
        }
    ).encode()
    machine_status, machine_configured = put_application(
        machine_endpoint, headers, machine_body
    )
    if (
        machine_status != 201
        or machine_configured.get("projectionState") != "PENDING"
    ):
        raise RuntimeError("A configuração inicial do cliente de máquina não foi aceita.")
    projected_machine = wait_for_projection(machine_endpoint, token, "APPLIED")
    _, credential = request_json(
        f"{machine_endpoint}/credentials/client-secret",
        method="POST",
        token=token,
    )
    if not isinstance(credential, dict) or not credential.get("clientSecret"):
        raise RuntimeError("A credencial de uso único da máquina não foi emitida.")
    credential = None
    print(
        "Smoke test aprovado: aplicação idempotente, autocadastro habilitado, "
        "API protegida projetada "
        "e reconciliada, SPA pública, BFF confidencial e máquina projetados no "
        "Keycloak; credenciais confidenciais emitidas sem exibição; estados "
        f"{projected['projectionState']}, {projected_spa['projectionState']} e "
        f"{projected_bff['projectionState']}, {projected_machine['projectionState']}."
    )


def wait_for_projection(endpoint: str, token: str, expected_state: str) -> dict:
    deadline = time.monotonic() + 30
    while time.monotonic() < deadline:
        request = urllib.request.Request(
            endpoint, headers={"Authorization": f"Bearer {token}"}
        )
        with urllib.request.urlopen(request, timeout=10) as response:
            projection = json.loads(response.read())
        if projection.get("projectionState") == expected_state:
            return projection
        if projection.get("projectionState") == "FAILED":
            raise RuntimeError(
                "A projeção do cliente da aplicação falhou: "
                f"{projection.get('lastProjectionFailureCode', 'UNKNOWN')}"
            )
        time.sleep(1)
    raise RuntimeError("A projeção do cliente da aplicação não concluiu em 30 segundos.")


def put_application(
    endpoint: str, headers: dict[str, str], body: bytes
) -> tuple[int, dict]:
    request = urllib.request.Request(
        endpoint, data=body, headers=headers, method="PUT"
    )
    try:
        with urllib.request.urlopen(request, timeout=10) as response:
            return response.status, json.loads(response.read())
    except urllib.error.HTTPError as error:
        if error.code == 401:
            raise RuntimeError(
                "Sessão administrativa ausente ou expirada. Execute primeiro a ação 'token'."
            ) from error
        raise RuntimeError(f"Cadastro falhou com HTTP {error.code}.") from error


def main() -> None:
    parser = argparse.ArgumentParser(description="IdentityHub local development harness")
    parser.add_argument("action", choices=("up", "down", "status", "run", "token", "smoke"))
    parser.add_argument("--repository", type=pathlib.Path, required=True)
    parser.add_argument("--env-file", type=pathlib.Path, required=True)
    args = parser.parse_args()
    local = read_env(args.env_file)
    process_env = os.environ.copy()
    process_env.update(local)
    process_env.update(
        {
            "IDENTITYHUB_KEYCLOAK_MANAGEMENT_ENABLED": "true",
            "IDENTITYHUB_KEYCLOAK_BASE_URI": keycloak_url(process_env),
            "IDENTITYHUB_KEYCLOAK_REALM": REALM,
            "IDENTITYHUB_KEYCLOAK_MANAGEMENT_CLIENT_ID": MANAGEMENT_CLIENT,
            "IDENTITYHUB_KEYCLOAK_MANAGEMENT_CLIENT_SECRET": management_secret(),
        }
    )

    if args.action == "up":
        up(args, process_env)
    elif args.action == "down":
        compose(["down"], args, process_env)
        token_file(args).unlink(missing_ok=True)
    elif args.action == "status":
        compose(["ps"], args, process_env)
    elif args.action == "run":
        up(args, process_env)
        run(["./gradlew", ":identityhub-service:bootRun"], cwd=args.repository, env=process_env)
    elif args.action == "token":
        acquire_token(args, process_env)
    elif args.action == "smoke":
        smoke(args, process_env)


if __name__ == "__main__":
    try:
        main()
    except (RuntimeError, subprocess.CalledProcessError, OSError) as error:
        print(f"Erro: {error}", file=sys.stderr)
        raise SystemExit(1) from error
