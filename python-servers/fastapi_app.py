"""
FastAPI Server for EtherFlow Integration.
Provides high-performance async REST API endpoints with Pydantic validation.
"""

from typing import List, Optional
from fastapi import FastAPI, Query
from pydantic import BaseModel
import uvicorn

app = FastAPI(
    title="EtherFlow FastAPI Integration",
    description="FastAPI service for reactive interop with EtherFlow HTTP Client",
    version="1.0.0"
)

class ItemRequest(BaseModel):
    name: str
    category: str
    price: float
    tags: Optional[List[str]] = []

class AnalysisRequest(BaseModel):
    dataset_name: str
    metrics: List[float]

@app.get("/api/fastapi/health")
async def health():
    return {
        "status": "UP",
        "framework": "FastAPI",
        "message": "FastAPI server is running asynchronously"
    }

@app.get("/api/fastapi/hello")
async def hello(name: str = Query("EtherFlow User")):
    return {
        "service": "FastAPI",
        "greeting": f"Hello, {name} from FastAPI!",
        "version": "1.0.0"
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

@app.post("/api/fastapi/analyze")
async def analyze_data(req: AnalysisRequest):
    count = len(req.metrics)
    total = sum(req.metrics)
    mean = total / count if count > 0 else 0
    max_val = max(req.metrics) if req.metrics else 0
    min_val = min(req.metrics) if req.metrics else 0

    return {
        "dataset": req.dataset_name,
        "status": "ok",
        "result": {
            "count": count,
            "sum": total,
            "mean": mean,
            "max": max_val,
            "min": min_val
        }
    }

if __name__ == '__main__':
    print("Starting FastAPI server on http://localhost:5002")
    uvicorn.run(app, host="0.0.0.0", port=5002)
