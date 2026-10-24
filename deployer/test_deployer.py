#!/usr/bin/env python3
"""
Unit and integration tests for deployer/server.py
"""

import os
import sys
import time
import json
import urllib.request
import urllib.error
import http.cookiejar
import subprocess
import unittest

PORT = 17777
BASE_URL = f"http://127.0.0.1:{PORT}"
SECRET = "testsecret123"

class TestDeployerServer(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        # Start server as a subprocess
        env = os.environ.copy()
        env["DEPLOYER_PORT"] = str(PORT)
        env["DEPLOYER_HOST"] = "127.0.0.1"
        env["DEPLOY_SECRET"] = SECRET
        env["PROJECT_DIR"] = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))

        cls.proc = subprocess.Popen(
            [sys.executable, os.path.join(os.path.dirname(__file__), "server.py")],
            env=env,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE
        )
        # Wait for server to bind
        time.sleep(1.0)

    @classmethod
    def tearDownClass(cls):
        cls.proc.terminate()
        try:
            cls.proc.wait(timeout=2)
        except subprocess.TimeoutExpired:
            cls.proc.kill()

    def test_01_unauthenticated_login_page(self):
        req = urllib.request.Request(f"{BASE_URL}/")
        with urllib.request.urlopen(req) as resp:
            self.assertEqual(resp.status, 200)
            content = resp.read().decode("utf-8")
            self.assertIn("Lingua Optima | Deploy Access", content)
            self.assertIn("Access Key", content)

    def test_02_invalid_login(self):
        data = urllib.parse.urlencode({"secret": "wrongpass"}).encode("utf-8")
        req = urllib.request.Request(f"{BASE_URL}/api/login", data=data, method="POST")
        try:
            with urllib.request.urlopen(req) as resp:
                self.assertIn("error=invalid_key", resp.geturl())
        except urllib.error.HTTPError as e:
            self.assertEqual(e.code, 303)

    def test_03_valid_login_and_cookie(self):
        cj = http.cookiejar.CookieJar()
        opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(cj))

        data = urllib.parse.urlencode({"secret": SECRET}).encode("utf-8")
        resp = opener.open(f"{BASE_URL}/api/login", data=data)
        self.assertEqual(resp.status, 200)
        content = resp.read().decode("utf-8")
        # Should now have access to main dashboard
        self.assertIn("Lingua Optima", content)
        self.assertIn("Запустить Redeploy", content)

        # Check cookies
        cookies = [c.name for c in cj]
        self.assertIn("deploy_auth", cookies)

    def test_04_authenticated_api_status(self):
        # Using Bearer token
        headers = {"Authorization": f"Bearer {SECRET}"}
        req = urllib.request.Request(f"{BASE_URL}/api/status", headers=headers)
        with urllib.request.urlopen(req) as resp:
            self.assertEqual(resp.status, 200)
            data = json.loads(resp.read().decode("utf-8"))
            self.assertIn("git", data)
            self.assertIn("containers", data)
            self.assertIn("system", data)
            self.assertIn("deployment", data)
            self.assertIn("ram", data["system"])
            self.assertIn("disk", data["system"])

    def test_05_token_query_param(self):
        # Using ?token= query parameter
        req = urllib.request.Request(f"{BASE_URL}/api/status?token={SECRET}")
        with urllib.request.urlopen(req) as resp:
            self.assertEqual(resp.status, 200)
            data = json.loads(resp.read().decode("utf-8"))
            self.assertIn("git", data)

    def test_06_unauthenticated_api_rejection(self):
        req = urllib.request.Request(f"{BASE_URL}/api/deploy", data=b"{}", headers={"Content-Type": "application/json"})
        try:
            urllib.request.urlopen(req)
            self.fail("Expected HTTP 401")
        except urllib.error.HTTPError as e:
            self.assertEqual(e.code, 401)

    def test_07_deploy_stream_sse_handshake(self):
        headers = {"Authorization": f"Bearer {SECRET}"}
        req = urllib.request.Request(f"{BASE_URL}/api/deploy/stream", headers=headers)
        with urllib.request.urlopen(req) as resp:
            self.assertEqual(resp.status, 200)
            self.assertEqual(resp.headers.get("Content-Type"), "text/event-stream")

    def test_08_restart_endpoint_validation(self):
        headers = {"Authorization": f"Bearer {SECRET}", "Content-Type": "application/json"}
        # Test restart with invalid service to check error handling
        payload = json.dumps({"service": "nonexistent_service_xyz"}).encode("utf-8")
        req = urllib.request.Request(f"{BASE_URL}/api/restart", data=payload, headers=headers)
        try:
            with urllib.request.urlopen(req) as resp:
                data = json.loads(resp.read().decode("utf-8"))
                self.assertIn("success", data)
        except urllib.error.HTTPError as e:
            self.assertEqual(e.code, 500)
            data = json.loads(e.read().decode("utf-8"))
            self.assertFalse(data.get("success"))

if __name__ == "__main__":
    unittest.main()

