"""
Third-Party API Integration Service Module.
Demonstrates how Python applications (Flask & FastAPI) call external third-party HTTP APIs.
Supports both synchronous HTTP calls (via requests) and asynchronous HTTP calls (via httpx / urllib).
"""

import time
import requests
from typing import Dict, Any, Optional, List

# Target external third-party public API for demonstration
THIRD_PARTY_BASE_URL = "https://jsonplaceholder.typicode.com"

class SyncThirdPartyService:
    """
    Synchronous Third-Party API Client (Ideal for Flask APIs).
    Uses 'requests' library with timeouts, headers, and exception handling.
    """

    def __init__(self, base_url: str = THIRD_PARTY_BASE_URL, timeout: int = 5):
        self.base_url = base_url.rstrip("/")
        self.timeout = timeout
        self.session = requests.Session()
        self.session.headers.update({
            "User-Agent": "EtherFlow-Python-Client/1.0",
            "Accept": "application/json"
        })

    def fetch_external_user(self, user_id: int) -> Dict[str, Any]:
        """Fetches user details from a third-party API synchronously."""
        url = f"{self.base_url}/users/{user_id}"
        try:
            response = self.session.get(url, timeout=self.timeout)
            response.raise_for_status()
            data = response.json()
            return {
                "success": True,
                "source": "ThirdPartyAPI (JSONPlaceholder)",
                "data": {
                    "external_id": data.get("id"),
                    "name": data.get("name"),
                    "username": data.get("username"),
                    "email": data.get("email"),
                    "city": data.get("address", {}).get("city")
                }
            }
        except requests.exceptions.RequestException as err:
            return {
                "success": False,
                "source": "ThirdPartyAPI",
                "error": str(err),
                "fallback_data": {
                    "external_id": user_id,
                    "name": f"Mock External User {user_id}",
                    "email": f"user{user_id}@external-service.org"
                }
            }

    def post_to_external_system(self, payload: Dict[str, Any]) -> Dict[str, Any]:
        """Sends a POST request with JSON payload to a third-party API."""
        url = f"{self.base_url}/posts"
        try:
            response = self.session.post(url, json=payload, timeout=self.timeout)
            response.raise_for_status()
            return {
                "success": True,
                "source": "ThirdPartyAPI",
                "statusCode": response.status_code,
                "remoteResponse": response.json()
            }
        except requests.exceptions.RequestException as err:
            return {
                "success": False,
                "source": "ThirdPartyAPI",
                "error": str(err),
                "simulatedResponse": {"id": 101, **payload}
            }


class AsyncThirdPartyService:
    """
    Asynchronous Third-Party API Client (Ideal for FastAPI).
    Uses 'httpx' (or fallback standard asyncio/urllib) for non-blocking async execution.
    """

    def __init__(self, base_url: str = THIRD_PARTY_BASE_URL, timeout: int = 5):
        self.base_url = base_url.rstrip("/")
        self.timeout = timeout

    async def fetch_external_posts(self, limit: int = 5) -> Dict[str, Any]:
        """Asynchronously fetches posts from an external third-party API."""
        url = f"{self.base_url}/posts?_limit={limit}"
        try:
            import httpx
            async with httpx.AsyncClient(timeout=self.timeout) as client:
                res = await client.get(url)
                res.raise_for_status()
                return {
                    "success": True,
                    "source": "ThirdPartyAPI (Async HTTPX)",
                    "count": len(res.json()),
                    "posts": res.json()
                }
        except Exception as err:
            # Fallback handling
            return {
                "success": False,
                "source": "ThirdPartyAPI (Fallback Async)",
                "error": str(err),
                "count": limit,
                "posts": [
                    {"id": i, "title": f"Async Mock Post {i}", "body": "Sample post content from third-party API"}
                    for i in range(1, limit + 1)
                ]
            }

    async def send_analytics_event(self, event_name: str, metadata: Dict[str, Any]) -> Dict[str, Any]:
        """Asynchronously posts analytics event to a third-party webhook/service."""
        url = f"{self.base_url}/posts"
        payload = {"event": event_name, "metadata": metadata, "timestamp": time.time()}
        try:
            import httpx
            async with httpx.AsyncClient(timeout=self.timeout) as client:
                res = await client.post(url, json=payload)
                res.raise_for_status()
                return {"success": True, "event": event_name, "externalId": res.json().get("id", 999)}
        except Exception as err:
            return {"success": False, "event": event_name, "error": str(err), "simulatedId": 999}
