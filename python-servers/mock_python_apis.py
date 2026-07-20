"""
Standalone zero-dependency Python mock server simulating Flask API (port 5001) and FastAPI (port 5002).
Uses standard Python http.server without requiring pip packages.
"""

from http.server import HTTPServer, BaseHTTPRequestHandler
import json
import threading
import urllib.parse

class FlaskHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        parsed = urllib.parse.urlparse(self.path)
        path = parsed.path
        query = urllib.parse.parse_qs(parsed.query)

        self.send_response(200)
        self.send_header('Content-Type', 'application/json')
        self.end_headers()

        if path == '/api/flask/health':
            res = {"status": "UP", "framework": "Flask", "message": "Flask API server is running smoothly"}
        elif path == '/api/flask/hello':
            name = query.get('name', ['EtherFlow User'])[0]
            res = {"service": "Flask API", "greeting": f"Hello, {name} from Flask API!", "framework": "Flask"}
        elif path.startswith('/api/flask/users/'):
            user_id = path.split('/')[-1]
            res = {"id": user_id, "name": f"Flask User {user_id}", "role": "Developer", "active": True}
        else:
            res = {"service": "Flask API", "path": path, "status": "ok"}

        self.wfile.write(json.dumps(res).encode('utf-8'))

    def do_POST(self):
        content_length = int(self.headers.get('Content-Length', 0))
        body_data = self.rfile.read(content_length).decode('utf-8') if content_length > 0 else "{}"
        try:
            payload = json.loads(body_data)
        except Exception:
            payload = {}

        inputs = payload.get("inputs", [10, 20, 30])
        total = sum(inputs)
        avg = total / len(inputs) if inputs else 0

        self.send_response(200)
        self.send_header('Content-Type', 'application/json')
        self.end_headers()

        res = {
            "status": "success",
            "framework": "Flask",
            "inputs": inputs,
            "prediction": {"sum": total, "average": avg, "score": round(avg * 1.5, 2)}
        }
        self.wfile.write(json.dumps(res).encode('utf-8'))

    def log_message(self, format, *args):
        return  # Suppress detailed standard HTTP log output


class FastApiHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        parsed = urllib.parse.urlparse(self.path)
        path = parsed.path
        query = urllib.parse.parse_qs(parsed.query)

        self.send_response(200)
        self.send_header('Content-Type', 'application/json')
        self.end_headers()

        if path == '/api/fastapi/health':
            res = {"status": "UP", "framework": "FastAPI", "message": "FastAPI server running asynchronously"}
        elif path == '/api/fastapi/hello':
            name = query.get('name', ['EtherFlow User'])[0]
            res = {"service": "FastAPI", "greeting": f"Hello, {name} from FastAPI!", "version": "1.0.0"}
        elif path.startswith('/api/fastapi/items/'):
            item_id = path.split('/')[-1]
            res = {"id": item_id, "name": f"FastAPI Item {item_id}", "category": "Technology", "price": 199.99}
        else:
            res = {"service": "FastAPI", "path": path, "status": "ok"}

        self.wfile.write(json.dumps(res).encode('utf-8'))

    def do_POST(self):
        content_length = int(self.headers.get('Content-Length', 0))
        body_data = self.rfile.read(content_length).decode('utf-8') if content_length > 0 else "{}"
        try:
            payload = json.loads(body_data)
        except Exception:
            payload = {}

        metrics = payload.get("metrics", [5.0, 10.0, 15.0])
        count = len(metrics)
        total = sum(metrics)
        mean = total / count if count > 0 else 0

        self.send_response(200)
        self.send_header('Content-Type', 'application/json')
        self.end_headers()

        res = {
            "dataset": payload.get("dataset_name", "Default Dataset"),
            "framework": "FastAPI",
            "status": "ok",
            "result": {"count": count, "sum": total, "mean": mean}
        }
        self.wfile.write(json.dumps(res).encode('utf-8'))

    def log_message(self, format, *args):
        return  # Suppress detailed standard HTTP log output


class ReusableHTTPServer(HTTPServer):
    allow_reuse_address = True

def serve_flask():
    server = ReusableHTTPServer(('127.0.0.1', 5001), FlaskHandler)
    print("  ✓ Flask API mock running on http://127.0.0.1:5001")
    server.serve_forever()

def serve_fastapi():
    server = ReusableHTTPServer(('127.0.0.1', 5002), FastApiHandler)
    print("  ✓ FastAPI API mock running on http://127.0.0.1:5002")
    server.serve_forever()

if __name__ == '__main__':
    print("=" * 60)
    print("Starting Zero-Dependency Python API Servers (Flask & FastAPI)")
    print("=" * 60)
    t1 = threading.Thread(target=serve_flask, daemon=True)
    t2 = threading.Thread(target=serve_fastapi, daemon=True)
    t1.start()
    t2.start()

    try:
        t1.join()
        t2.join()
    except KeyboardInterrupt:
        print("Servers stopped.")
