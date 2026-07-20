"""
FastAPI Server with Async Third-Party API Integration & Pydantic Validation.

Architecture:
- FastAPI Router & Controller Layer (@app.get, @app.post)
- Pydantic Request / Response Schema Layer
- Service Layer (AsyncThirdPartyService)
- Non-Blocking Async Third-Party HTTP Calls (httpx)
- Global Exception Handlers
"""

from typing import List, Optional, Dict, Any
from fastapi import FastAPI, Query, HTTPException, Depends, status
from pydantic import BaseModel, Field
import uvicorn
from third_party_service import AsyncThirdPartyService

app = FastAPI(
    title="EtherFlow FastAPI Architecture & Third-Party Integration",
    description="High-performance async Python REST API calling third-party services",
    version="2.0.0"
)

async_service = AsyncThirdPartyService()

# ---------------------------------------------------------
# 1. Pydantic Schemas (Request / Response Data Models)
# ---------------------------------------------------------
class AnalysisRequest(BaseModel):
    dataset_name: str = Field(..., example="CustomerChurn2026")
    metrics: List[float] = Field(..., example=[10.5, 20.2, 30.8])

class AnalyticsEvent(BaseModel):
    event_name: str = Field(..., example="user_signup")
    metadata: Dict[str, Any] = Field(default_factory=dict)

# ---------------------------------------------------------
# 2. API Creation: Endpoints
# ---------------------------------------------------------
@app.get("/api/fastapi/health")
async def health():
    return {
        "status": "UP",
        "framework": "FastAPI (Async)",
        "thirdPartyIntegration": "Enabled (Async HTTPX Client)",
        "message": "FastAPI server is running asynchronously"
    }

@app.get("/api/fastapi/hello")
async def hello(name: str = Query("EtherFlow User")):
    return {
        "service": "FastAPI",
        "greeting": f"Hello, {name} from FastAPI!",
        "architecture": "Async ASGI Server"
    }

@app.get("/api/fastapi/items/{item_id}")
async def get_item(item_id: str):
    return {
        "id": item_id,
        "name": f"FastAPI Item {item_id}",
        "category": "Technology",
        "price": 199.99,
        "in_stock": True
    }

# ---------------------------------------------------------
# 3. Third-Party API Integration Endpoints (Calling External APIs)
# ---------------------------------------------------------
@app.get("/api/fastapi/external-posts")
async def fetch_third_party_posts(limit: int = Query(5, ge=1, le=20)):
    """
    Asynchronously calls external third-party API and returns formatted posts.
    """
    result = await async_service.fetch_external_posts(limit=limit)
    return {
        "fastApiEndpoint": "/api/fastapi/external-posts",
        "result": result
    }

@app.post("/api/fastapi/external-event", status_code=status.HTTP_201_CREATED)
async def post_third_party_event(event: AnalyticsEvent):
    """
    Asynchronously posts an analytics event to an external third-party service.
    """
    result = await async_service.send_analytics_event(event.event_name, event.metadata)
    return {
        "fastApiEndpoint": "/api/fastapi/external-event",
        "result": result
    }

@app.post("/api/fastapi/analyze")
async def analyze_data(req: AnalysisRequest):
    count = len(req.metrics)
    total = sum(req.metrics)
    mean = total / count if count > 0 else 0

    return {
        "dataset": req.dataset_name,
        "status": "ok",
        "result": {
            "count": count,
            "sum": total,
            "mean": mean
        }
    }

if __name__ == '__main__':
    print("Starting FastAPI server with Async Third-Party Integration on http://localhost:5002")
    uvicorn.run(app, host="0.0.0.0", port=5002)
