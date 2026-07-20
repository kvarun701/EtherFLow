"""
Flask REST API Server with Third-Party API Integration.

Architecture:
- Flask Router / Controller Layer (app.route)
- Service Layer (SyncThirdPartyService)
- Third-Party External REST API Invocation (requests)
- Error Handling (@app.errorhandler)
"""

from flask import Flask, jsonify, request
from third_party_service import SyncThirdPartyService

app = Flask(__name__)
third_party_service = SyncThirdPartyService()

# ---------------------------------------------------------
# 1. API Creation: Health & Meta Endpoints
# ---------------------------------------------------------
@app.route('/api/flask/health', methods=['GET'])
def health():
    return jsonify({
        "status": "UP",
        "framework": "Flask",
        "message": "Flask API server is running smoothly",
        "thirdPartyIntegration": "Enabled (Sync Requests Client)"
    }), 200

@app.route('/api/flask/hello', methods=['GET'])
def hello():
    name = request.args.get('name', 'EtherFlow User')
    return jsonify({
        "service": "Flask API",
        "greeting": f"Hello, {name} from Flask API!",
        "framework": "Flask 3.x"
    }), 200

@app.route('/api/flask/users/<int:user_id>', methods=['GET'])
def get_user(user_id):
    return jsonify({
        "id": user_id,
        "name": f"Flask User {user_id}",
        "role": "Developer",
        "active": True
    }), 200

# ---------------------------------------------------------
# 2. Third-Party API Integration Endpoints (Calling External APIs)
# ---------------------------------------------------------
@app.route('/api/flask/external-user/<int:user_id>', methods=['GET'])
def fetch_third_party_user(user_id):
    """
    Endpoint that calls a third-party REST API synchronously and returns the enriched data.
    """
    external_response = third_party_service.fetch_external_user(user_id)
    return jsonify({
        "flaskEndpoint": f"/api/flask/external-user/{user_id}",
        "result": external_response
    }), 200

@app.route('/api/flask/external-post', methods=['POST'])
def create_third_party_post():
    """
    Endpoint that accepts JSON payload and forwards/posts it to a third-party REST API.
    """
    payload = request.get_json(silent=True) or {"title": "Default Flask Post", "body": "Flask content"}
    result = third_party_service.post_to_external_system(payload)
    return jsonify({
        "flaskEndpoint": "/api/flask/external-post",
        "result": result
    }), 201

@app.route('/api/flask/predict', methods=['POST'])
def predict():
    data = request.get_json(silent=True) or {}
    inputs = data.get("inputs", [10, 20, 30])
    total = sum(inputs)
    avg = total / len(inputs) if inputs else 0
    return jsonify({
        "status": "success",
        "inputs": inputs,
        "prediction": {
            "sum": total,
            "average": avg,
            "score": round(avg * 1.5, 2)
        }
    }), 200

# ---------------------------------------------------------
# 3. Global Exception & Error Handling
# ---------------------------------------------------------
@app.errorhandler(404)
def not_found(error):
    return jsonify({"error": "Resource Not Found", "status": 404}), 404

@app.errorhandler(500)
def internal_error(error):
    return jsonify({"error": "Internal Server Error", "status": 500}), 500

if __name__ == '__main__':
    print("Starting Flask API server with Third-Party Integration on http://localhost:5001")
    app.run(host='0.0.0.0', port=5001, debug=False)
