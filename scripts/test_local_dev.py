#!/usr/bin/env python3
"""Fast contract tests for the local development harness."""

from __future__ import annotations

import argparse
import importlib.util
import pathlib
import tempfile
import unittest
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
            }
        )

        client = realm["clients"][0]
        user = realm["users"][0]
        self.assertFalse(client["directAccessGrantsEnabled"])
        self.assertEqual(
            "true",
            client["attributes"]["oauth2.device.authorization.grant.enabled"],
        )
        self.assertEqual(["CONFIGURE_TOTP"], user["requiredActions"])
        self.assertEqual(["PLATFORM_ADMIN"], user["realmRoles"])

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


if __name__ == "__main__":
    unittest.main()
