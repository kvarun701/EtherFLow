"""
Launcher script to run both Flask API (port 5001) and FastAPI (port 5002) concurrently.
Usage:
    python start_python_apis.py
"""

import multiprocessing
import time
import sys
import os

def run_flask():
    from flask_app import app
    app.run(host="0.0.0.0", port=5001, debug=False)

def run_fastapi():
    import uvicorn
    from fastapi_app import app
    uvicorn.run(app, host="0.0.0.0", port=5002, log_level="warning")

if __name__ == "__main__":
    print("=" * 60)
    print("Starting Python APIs (Flask & FastAPI) for EtherFlow Integration")
    print("=" * 60)
    print("  - Flask API:   http://localhost:5001")
    print("  - FastAPI API: http://localhost:5002")
    print("=" * 60)

    p1 = multiprocessing.Process(target=run_flask)
    p2 = multiprocessing.Process(target=run_fastapi)

    p1.start()
    p2.start()

    try:
        p1.join()
        p2.join()
    except KeyboardInterrupt:
        print("\nStopping Python API servers...")
        p1.terminate()
        p2.terminate()
        p1.join()
        p2.join()
        print("Servers stopped cleanly.")
