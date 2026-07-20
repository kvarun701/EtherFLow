// server.go — EtherFlow-compatible REST API Server for Go
// Exposes the same endpoints as Python Flask/FastAPI servers so EtherFlow
// Java/Kotlin clients can call them seamlessly.
//
// Run: go run server.go etherflow_client.go
// Port: 5008

package etherflow

import (
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"strconv"
	"strings"
)

// ─────────────────────────────────────────────────────────────────────────────
// JSON Helpers
// ─────────────────────────────────────────────────────────────────────────────

func writeJSON(w http.ResponseWriter, status int, v any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	json.NewEncoder(w).Encode(v)
}

// ─────────────────────────────────────────────────────────────────────────────
// Handlers
// ─────────────────────────────────────────────────────────────────────────────

func healthHandler(w http.ResponseWriter, r *http.Request) {
	writeJSON(w, 200, map[string]any{
		"status":    "UP",
		"framework": "net/http (Go)",
		"message":   "Go API server is running smoothly via EtherFlow",
	})
}

func helloHandler(w http.ResponseWriter, r *http.Request) {
	name := r.URL.Query().Get("name")
	if name == "" {
		name = "EtherFlow User"
	}
	writeJSON(w, 200, map[string]any{
		"service":   "Go net/http API",
		"greeting":  fmt.Sprintf("Hello, %s from Go!", name),
		"framework": "net/http / Go 1.22",
	})
}

func getUserHandler(w http.ResponseWriter, r *http.Request) {
	// Path: /api/go/users/{id}
	parts := strings.Split(r.URL.Path, "/")
	idStr := parts[len(parts)-1]
	id, err := strconv.Atoi(idStr)
	if err != nil || id <= 0 {
		writeJSON(w, 400, map[string]any{"error": "Invalid user ID"})
		return
	}
	writeJSON(w, 200, map[string]any{
		"id":     id,
		"name":   fmt.Sprintf("Go User %d", id),
		"role":   "Developer",
		"active": true,
	})
}

func predictHandler(w http.ResponseWriter, r *http.Request) {
	var body struct {
		Inputs []float64 `json:"inputs"`
	}
	if err := json.NewDecoder(r.Body).Decode(&body); err != nil || len(body.Inputs) == 0 {
		body.Inputs = []float64{10, 20, 30}
	}

	var total float64
	for _, v := range body.Inputs {
		total += v
	}
	avg := total / float64(len(body.Inputs))

	writeJSON(w, 200, map[string]any{
		"status": "success",
		"inputs": body.Inputs,
		"prediction": map[string]any{
			"sum":     total,
			"average": avg,
			"score":   float64(int(avg*1.5*100)) / 100,
		},
	})
}

func externalPostHandler(w http.ResponseWriter, r *http.Request) {
	var payload map[string]any
	if err := json.NewDecoder(r.Body).Decode(&payload); err != nil {
		payload = map[string]any{"title": "Default Go Post", "body": "Content"}
	}

	client := NewClient("https://jsonplaceholder.typicode.com", WithRetry(3))
	var created map[string]any
	if err := client.Post(context.Background(), "/posts", payload, &created); err != nil {
		writeJSON(w, 502, map[string]any{"status": "error", "message": err.Error()})
		return
	}

	writeJSON(w, 201, map[string]any{
		"status":         "success",
		"source":         "Go net/http → Third-Party API",
		"remoteResponse": created,
	})
}

// ─────────────────────────────────────────────────────────────────────────────
// Router & Entry Point
// ─────────────────────────────────────────────────────────────────────────────

func newServerMux() *http.ServeMux {
	mux := http.NewServeMux()
	mux.HandleFunc("GET /api/go/health",        healthHandler)
	mux.HandleFunc("GET /api/go/hello",         helloHandler)
	mux.HandleFunc("GET /api/go/users/",        getUserHandler)
	mux.HandleFunc("POST /api/go/predict",      predictHandler)
	mux.HandleFunc("POST /api/go/external-post", externalPostHandler)
	return mux
}

func StartServer(addr string) error {
	mux := newServerMux()
	fmt.Printf("Go API server running on http://%s\n", addr)
	fmt.Println("  GET  /api/go/health")
	fmt.Println("  GET  /api/go/hello?name=Alice")
	fmt.Println("  GET  /api/go/users/1")
	fmt.Println("  POST /api/go/predict")
	fmt.Println("  POST /api/go/external-post")
	return http.ListenAndServe(addr, mux)
}

// Uncomment to run as standalone:
// func main() {
//     if err := StartServer(":5008"); err != nil {
//         log.Fatal(err)
//     }
// }
