#!/usr/bin/env python3
"""Local IdentityHub harness. It never prints credentials or bearer tokens."""

from __future__ import annotations

import argparse
import json
import os
import pathlib
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
    body: dict | None = None,
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


def up(args: argparse.Namespace, env: dict[str, str]) -> None:
    compose(["up", "-d", "identityhub-database", "keycloak"], args, env)
    bootstrap(env)
    print("Infraestrutura local pronta.")


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
    application_id = uuid.UUID("75f911ae-d107-46b4-bb6f-bcb46a62f124")
    endpoint = f"http://127.0.0.1:8080/internal/admin/client-applications/{application_id}"
    headers = {
        "Accept": "application/json",
        "Content-Type": "application/json",
        "Authorization": f"Bearer {token}",
        "X-Correlation-ID": str(uuid.uuid4()),
    }
    request = urllib.request.Request(
        endpoint,
        data=json.dumps({"identifier": "local-smoke-app", "displayName": "Local Smoke App"}).encode(),
        headers=headers,
        method="PUT",
    )
    try:
        with urllib.request.urlopen(request, timeout=10) as response:
            created = json.loads(response.read())
            put_status = response.status
    except urllib.error.HTTPError as error:
        raise RuntimeError(f"Cadastro falhou com HTTP {error.code}.") from error
    get_request = urllib.request.Request(endpoint, headers={"Authorization": f"Bearer {token}"})
    try:
        with urllib.request.urlopen(get_request, timeout=10) as response:
            found = json.loads(response.read())
    except urllib.error.HTTPError as error:
        raise RuntimeError(f"Consulta falhou com HTTP {error.code}.") from error
    if created.get("applicationId") != str(application_id) or found != created:
        raise RuntimeError("O round-trip da aplicação não preservou o contrato esperado.")
    print(f"Smoke test aprovado: PUT HTTP {put_status}, GET HTTP 200, estado {found['state']}.")


def main() -> None:
    parser = argparse.ArgumentParser(description="IdentityHub local development harness")
    parser.add_argument("action", choices=("up", "down", "status", "run", "token", "smoke"))
    parser.add_argument("--repository", type=pathlib.Path, required=True)
    parser.add_argument("--env-file", type=pathlib.Path, required=True)
    args = parser.parse_args()
    local = read_env(args.env_file)
    process_env = os.environ.copy()
    process_env.update(local)

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
