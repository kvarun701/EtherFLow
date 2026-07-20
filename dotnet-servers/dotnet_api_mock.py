"""
Standalone zero-dependency HTTP server simulating .NET Framework / ASP.NET Core Web API on port 5003.
Uses standard Python http.server without requiring .NET SDK or external pip packages.
"""

from http.server import HTTPServer, BaseHTTPRequestHandler
import json
import urllib.parse

class ReusableHTTPServer(HTTPServer):
    allow_reuse_address = True

class DotNetHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        parsed = urllib.parse.urlparse(self.path)
        path = parsed.path
        query = urllib.parse.parse_qs(parsed.query)

        self.send_response(200)
        self.send_header('Content-Type', 'application/json')
        self.end_headers()

        if path == '/api/dotnet/health':
            res = {
                "status": "UP",
                "framework": ".NET Core / ASP.NET Core Web API",
                "message": ".NET API server is running smoothly",
                "timestamp": "2026-07-20T23:35:00Z"
            }
        elif path == '/api/dotnet/hello':
            name = query.get('name', ['EtherFlow User'])[0]
            res = {
                "service": ".NET Web API",
                "greeting": f"Hello, {name} from .NET Framework / ASP.NET Core!",
                "version": "8.0"
            }
        elif path.startswith('/api/dotnet/products/'):
            prod_id = path.split('/')[-1]
            res = {
                "id": prod_id,
                "name": f".NET Enterprise Product {prod_id}",
                "category": "Enterprise Software",
                "price": 299.99,
                "inStock": True
            }
        else:
            res = {"service": ".NET Web API", "path": path, "status": "ok"}

        self.wfile.write(json.dumps(res).encode('utf-8'))

    def do_POST(self):
        content_length = int(self.headers.get('Content-Length', 0))
        body_data = self.rfile.read(content_length).decode('utf-8') if content_length > 0 else "{}"
        try:
            payload = json.loads(body_data)
        except Exception:
            payload = {}

        data_list = payload.get("data", [1, 2, 3, 4])
        task_name = payload.get("taskName", "DefaultTask")

        self.send_response(200)
        self.send_header('Content-Type', 'application/json')
        self.end_headers()

        res = {
            "status": "success",
            "framework": ".NET Web API",
            "taskName": task_name,
            "processedItems": len(data_list),
            "resultCode": 200
        }
        self.wfile.write(json.dumps(res).encode('utf-8'))

    def log_message(self, format, *args):
        return  # Suppress default HTTP logs

def serve_dotnet():
    server = ReusableHTTPServer(('127.0.0.1', 5003), DotNetHandler)
    print("  ✓ .NET Web API mock server running on http://127.0.0.1:5003")
    server.serve_forever()

if __name__ == '__main__':
    print("=" * 60)
    print("Starting Zero-Dependency .NET Web API Server (Port 5003)")
    print("=" * 60)
    try:
        serve_dotnet()
    except KeyboardInterrupt:
        print("Server stopped.")
