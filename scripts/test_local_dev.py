#!/usr/bin/env python3
"""Fast contract tests for the local development harness."""

from __future__ import annotations

import argparse
import importlib.util
import pathlib
import tempfile
import unittest
import uuid
from unittest import mock


HARNESS_PATH = pathlib.Path(__file__).with_name("local-dev.py")
SPEC = importlib.util.spec_from_file_location("identityhub_local_dev", HARNESS_PATH)
assert SPEC is not None and SPEC.loader is not None
HARNESS = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(HARNESS)


class LocalDevelopmentHarnessTest(unittest.TestCase):
    def test_reads_comments_exports_and_quoted_values(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            path = pathlib.Path(directory) / "local.env"
            path.write_text(
                "# local only\nexport FIRST=value\nSECOND='quoted value'\n",
                encoding="utf-8",
            )

            self.assertEqual(
                {"FIRST": "value", "SECOND": "quoted value"},
                HARNESS.read_env(path),
            )

    def test_realm_keeps_admin_login_hosted_and_requires_totp_enrollment(self) -> None:
        realm = HARNESS.realm_representation(
            {
                "IDENTITYHUB_LOCAL_ADMIN_USERNAME": "local-admin",
                "IDENTITYHUB_LOCAL_ADMIN_PASSWORD": "test-only-password",
                "IDENTITYHUB_KEYCLOAK_MANAGEMENT_CLIENT_SECRET": "test-only-secret",
                "IDENTITYHUB_KEYCLOAK_IDENTITY_MANAGEMENT_CLIENT_SECRET": "test-only-identity-secret",
            }
        )

        client = next(
            configured
            for configured in realm["clients"]
            if configured["clientId"] == HARNESS.ADMIN_CLIENT
        )
        management = next(
            configured
            for configured in realm["clients"]
            if configured["clientId"] == HARNESS.MANAGEMENT_CLIENT
        )
        identity_management = next(
            configured
            for configured in realm["clients"]
            if configured["clientId"] == HARNESS.IDENTITY_MANAGEMENT_CLIENT
        )
        user = realm["users"][0]
        self.assertFalse(client["directAccessGrantsEnabled"])
        self.assertFalse(client["fullScopeAllowed"])
        self.assertEqual(
            "true",
            client["attributes"]["oauth2.device.authorization.grant.enabled"],
        )
        self.assertEqual(["CONFIGURE_TOTP"], user["requiredActions"])
        self.assertEqual(["PLATFORM_ADMIN"], user["realmRoles"])
        self.assertTrue(management["serviceAccountsEnabled"])
        self.assertFalse(management["fullScopeAllowed"])
        self.assertTrue(identity_management["serviceAccountsEnabled"])
        self.assertFalse(identity_management["fullScopeAllowed"])
        self.assertEqual("length(15) and maxLength(64)", realm["passwordPolicy"])
        self.assertTrue(realm["internationalizationEnabled"])
        self.assertEqual(["en", "pt-BR"], realm["supportedLocales"])
        self.assertTrue(realm["bruteForceProtected"])
        self.assertFalse(realm["permanentLockout"])
        self.assertEqual(5, realm["failureFactor"])
        self.assertEqual(0, realm["quickLoginCheckMilliSeconds"])
        self.assertEqual(30, realm["waitIncrementSeconds"])
        self.assertEqual(900, realm["maxFailureWaitSeconds"])
        self.assertTrue(realm["eventsEnabled"])
        self.assertTrue(realm["adminEventsEnabled"])
        self.assertFalse(realm["adminEventsDetailsEnabled"])
        self.assertEqual(
            ["LOGIN", "LOGIN_ERROR"],
            realm["enabledEventTypes"],
        )

    def test_email_only_profile_does_not_invent_personal_names(self) -> None:
        profile = {
            "attributes": [
                {"name": "username", "required": {"roles": ["user"]}},
                {"name": "email", "required": {"roles": ["user"]}},
                {"name": "firstName", "required": {"roles": ["user"]}},
                {"name": "lastName", "required": {"roles": ["user"]}},
            ]
        }

        configured = HARNESS.allow_email_only_profile(profile)

        required = {
            attribute["name"]: "required" in attribute
            for attribute in configured["attributes"]
        }
        self.assertEqual(
            {
                "username": True,
                "email": True,
                "firstName": False,
                "lastName": False,
            },
            required,
        )

    @mock.patch.object(HARNESS.subprocess, "run")
    def test_uses_pinned_compose_fallback_when_plugin_is_absent(self, run_mock) -> None:
        run_mock.return_value.returncode = 1
        arguments = argparse.Namespace(
            repository=pathlib.Path("/workspace/identity-hub"),
            env_file=pathlib.Path("/workspace/identity-hub.local.env"),
        )

        command = HARNESS.compose_command(arguments, {})

        self.assertIn("docker:29.1.3-cli", command)
        self.assertIn("/var/run/docker.sock:/var/run/docker.sock", command)

    @mock.patch.object(HARNESS, "request_json")
    @mock.patch.object(HARNESS.uuid, "uuid4")
    def test_public_registration_smoke_keeps_the_contract_generic(
        self, uuid_mock, request_mock
    ) -> None:
        fixed = "a" * 32
        uuid_mock.return_value = uuid.UUID(hex=fixed)
        request_mock.side_effect = [
            (202, {"message": "If the request is eligible, instructions will be sent"}),
            (200, {"messages": [{"To": f"local-smoke-{fixed}@example.test"}]}),
        ]

        HARNESS.smoke_public_registration(
            "local-smoke-application", {"IDENTITYHUB_MAILPIT_HTTP_PORT": "8025"}
        )

        registration_call = request_mock.call_args_list[0]
        self.assertEqual((202,), registration_call.kwargs["expected"])
        self.assertEqual(
            f"local-smoke-{fixed}@example.test",
            registration_call.kwargs["body"]["email"],
        )
        self.assertTrue(
            registration_call.kwargs["body"]["password"].startswith(
                "Local-smoke-registration-"
            )
        )
        self.assertEqual(2, request_mock.call_count)


if __name__ == "__main__":
    unittest.main()
