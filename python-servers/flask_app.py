"""
Flask REST API Server for EtherFlow Integration.
Provides endpoints for GET, POST, path parameters, query parameters, and JSON payloads.
"""

from flask import Flask, jsonify, request

app = Flask(__name__)

@app.route('/api/flask/health', methods=['GET'])
def health():
    return jsonify({
        "status": "UP",
        "framework": "Flask",
        "message": "Flask API server is running smoothly"
    }), 200

@app.route('/api/flask/hello', methods=['GET'])
def hello():
    name = request.args.get('name', 'EtherFlow User')
    return jsonify({
        "service": "Flask API",
        "greeting": f"Hello, {name} from Flask API!",
        "timestamp": "2026-07-20T23:30:00Z"
    }), 200

@app.route('/api/flask/users/<user_id>', methods=['GET'])
def get_user(user_id):
    return jsonify({
        "id": user_id,
        "name": f"Flask User {user_id}",
        "role": "Developer",
        "active": True
    }), 200

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

if __name__ == '__main__':
    print("Starting Flask API server on http://localhost:5001")
    app.run(host='0.0.0.0', port=5001, debug=False)
