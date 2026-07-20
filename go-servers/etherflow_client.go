// etherflow_client.go
// EtherFlow HTTP Client for Go — net/http + context
//
// A typed, context-aware HTTP client that mirrors the EtherFlow Java/Kotlin builder API.
// Zero dependencies beyond the Go standard library.
//
// Usage:
//   client := etherflow.NewClient("https://api.example.com", etherflow.WithRetry(3))
//   var user User
//   err := client.Get(context.Background(), "/users/1", &user)

package etherflow

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"math"
	"net/http"
	"strings"
	"time"
)

// ─────────────────────────────────────────────────────────────────────────────
// Errors
// ─────────────────────────────────────────────────────────────────────────────

// EtherFlowError represents an HTTP or network error from the client.
type EtherFlowError struct {
	StatusCode int
	Body       string
	Cause      error
}

func (e *EtherFlowError) Error() string {
	if e.Cause != nil {
		return fmt.Sprintf("[EtherFlow] Network error: %v", e.Cause)
	}
	return fmt.Sprintf("[EtherFlow] HTTP %d: %s", e.StatusCode, e.Body)
}

// ─────────────────────────────────────────────────────────────────────────────
// Config / Options
// ─────────────────────────────────────────────────────────────────────────────

// Config holds client configuration.
type Config struct {
	BaseURL        string
	Timeout        time.Duration
	MaxRetries     int
	RetryDelay     time.Duration
	DefaultHeaders map[string]string
}

// Option is a functional option for Config.
type Option func(*Config)

func WithTimeout(d time.Duration) Option    { return func(c *Config) { c.Timeout = d } }
func WithRetry(n int) Option                { return func(c *Config) { c.MaxRetries = n } }
func WithRetryDelay(d time.Duration) Option { return func(c *Config) { c.RetryDelay = d } }
func WithHeader(key, val string) Option {
	return func(c *Config) { c.DefaultHeaders[key] = val }
}

// ─────────────────────────────────────────────────────────────────────────────
// Client
// ─────────────────────────────────────────────────────────────────────────────

// Client is the EtherFlow HTTP client for Go.
type Client struct {
	cfg  Config
	http *http.Client
}

// NewClient creates a new EtherFlow client for Go.
func NewClient(baseURL string, opts ...Option) *Client {
	cfg := Config{
		BaseURL:        strings.TrimRight(baseURL, "/"),
		Timeout:        10 * time.Second,
		MaxRetries:     3,
		RetryDelay:     200 * time.Millisecond,
		DefaultHeaders: map[string]string{},
	}
	for _, opt := range opts {
		opt(&cfg)
	}
	return &Client{
		cfg:  cfg,
		http: &http.Client{Timeout: cfg.Timeout},
	}
}

// ─────────────────────────────────────────────────────────────────────────────
// Public API
// ─────────────────────────────────────────────────────────────────────────────

// Get performs a GET request and JSON-decodes the response into out.
func (c *Client) Get(ctx context.Context, path string, out any, headers ...map[string]string) error {
	return c.execute(ctx, http.MethodGet, path, nil, out, mergeHeaders(headers...))
}

// Post performs a POST request with a JSON body.
func (c *Client) Post(ctx context.Context, path string, body any, out any, headers ...map[string]string) error {
	return c.execute(ctx, http.MethodPost, path, body, out, mergeHeaders(headers...))
}

// Put performs a PUT request.
func (c *Client) Put(ctx context.Context, path string, body any, out any, headers ...map[string]string) error {
	return c.execute(ctx, http.MethodPut, path, body, out, mergeHeaders(headers...))
}

// Patch performs a PATCH request.
func (c *Client) Patch(ctx context.Context, path string, body any, out any, headers ...map[string]string) error {
	return c.execute(ctx, http.MethodPatch, path, body, out, mergeHeaders(headers...))
}

// Delete performs a DELETE request.
func (c *Client) Delete(ctx context.Context, path string, out any, headers ...map[string]string) error {
	return c.execute(ctx, http.MethodDelete, path, nil, out, mergeHeaders(headers...))
}

// GetRaw performs a GET and returns the raw response body bytes.
func (c *Client) GetRaw(ctx context.Context, path string) ([]byte, error) {
	return c.executeRaw(ctx, http.MethodGet, path, nil, nil)
}

// CheckHealth calls /health on the base URL and returns the JSON response.
func (c *Client) CheckHealth(ctx context.Context) (map[string]any, error) {
	var result map[string]any
	err := c.Get(ctx, "/health", &result)
	if err != nil {
		return map[string]any{"status": "DOWN", "error": err.Error()}, nil
	}
	return result, nil
}

// ─────────────────────────────────────────────────────────────────────────────
// Internal
// ─────────────────────────────────────────────────────────────────────────────

func (c *Client) execute(ctx context.Context, method, path string, body, out any, extra map[string]string) error {
	data, err := c.executeRaw(ctx, method, path, body, extra)
	if err != nil {
		return err
	}
	if out == nil {
		return nil
	}
	return json.Unmarshal(data, out)
}

func (c *Client) executeRaw(ctx context.Context, method, path string, body any, extra map[string]string) ([]byte, error) {
	url := c.resolveURL(path)

	var lastErr error
	delay := c.cfg.RetryDelay

	for attempt := 0; attempt <= c.cfg.MaxRetries; attempt++ {
		data, err := c.doRequest(ctx, method, url, body, extra)
		if err == nil {
			return data, nil
		}

		// Don't retry 4xx
		if efErr, ok := err.(*EtherFlowError); ok && efErr.StatusCode >= 400 && efErr.StatusCode < 500 {
			return nil, err
		}

		lastErr = err
		if attempt < c.cfg.MaxRetries {
			fmt.Printf("[EtherFlow.Go] Retry %d/%d after %v: %v\n", attempt+1, c.cfg.MaxRetries, delay, err)
			select {
			case <-ctx.Done():
				return nil, ctx.Err()
			case <-time.After(delay):
			}
			delay = time.Duration(float64(delay) * math.Pow(2, 1))
		}
	}
	return nil, fmt.Errorf("[EtherFlow] max retries exceeded: %w", lastErr)
}

func (c *Client) doRequest(ctx context.Context, method, url string, body any, extra map[string]string) ([]byte, error) {
	var bodyReader io.Reader
	if body != nil {
		b, err := json.Marshal(body)
		if err != nil {
			return nil, &EtherFlowError{Cause: err}
		}
		bodyReader = bytes.NewReader(b)
	}

	req, err := http.NewRequestWithContext(ctx, method, url, bodyReader)
	if err != nil {
		return nil, &EtherFlowError{Cause: err}
	}

	req.Header.Set("Accept", "application/json")
	req.Header.Set("User-Agent", "EtherFlow-Go-Client/1.0")
	if body != nil {
		req.Header.Set("Content-Type", "application/json")
	}
	for k, v := range c.cfg.DefaultHeaders {
		req.Header.Set(k, v)
	}
	for k, v := range extra {
		req.Header.Set(k, v)
	}

	resp, err := c.http.Do(req)
	if err != nil {
		return nil, &EtherFlowError{Cause: err}
	}
	defer resp.Body.Close()

	respBody, err := io.ReadAll(resp.Body)
	if err != nil {
		return nil, &EtherFlowError{Cause: err}
	}

	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		return nil, &EtherFlowError{StatusCode: resp.StatusCode, Body: string(respBody)}
	}
	return respBody, nil
}

func (c *Client) resolveURL(path string) string {
	if strings.HasPrefix(path, "http") {
		return path
	}
	return c.cfg.BaseURL + path
}

func mergeHeaders(maps ...map[string]string) map[string]string {
	result := map[string]string{}
	for _, m := range maps {
		for k, v := range m {
			result[k] = v
		}
	}
	return result
}

// ─────────────────────────────────────────────────────────────────────────────
// Builder (mirrors Java EtherFlowClient.builder())
// ─────────────────────────────────────────────────────────────────────────────

// Builder provides a fluent builder for EtherFlow Go client.
type Builder struct {
	baseURL string
	opts    []Option
}

// NewBuilder returns a new fluent builder.
func NewBuilder() *Builder { return &Builder{} }

func (b *Builder) BaseURL(url string) *Builder    { b.baseURL = url; return b }
func (b *Builder) Timeout(d time.Duration) *Builder { b.opts = append(b.opts, WithTimeout(d)); return b }
func (b *Builder) Retry(n int) *Builder            { b.opts = append(b.opts, WithRetry(n)); return b }
func (b *Builder) Header(k, v string) *Builder     { b.opts = append(b.opts, WithHeader(k, v)); return b }
func (b *Builder) Build() *Client                  { return NewClient(b.baseURL, b.opts...) }

/*
// Example usage — main.go:

package main

import (
	"context"
	"fmt"
	"github.com/kvarun701/EtherFLow/go-servers/etherflow"
)

type User struct {
	ID    int    `json:"id"`
	Name  string `json:"name"`
	Email string `json:"email"`
}

type Post struct {
	Title  string `json:"title"`
	Body   string `json:"body"`
	UserID int    `json:"userId"`
}

func main() {
	ctx := context.Background()

	// 1. Create client using builder
	client := etherflow.NewBuilder().
		BaseURL("https://jsonplaceholder.typicode.com").
		Retry(3).
		Timeout(10 * time.Second).
		Build()

	// 2. GET — decode into struct
	var user User
	if err := client.Get(ctx, "/users/1", &user); err != nil {
		panic(err)
	}
	fmt.Printf("User: %s — %s\n", user.Name, user.Email)

	// 3. GET list
	var users []User
	if err := client.Get(ctx, "/users", &users); err != nil {
		panic(err)
	}
	fmt.Printf("Total users: %d\n", len(users))

	// 4. POST with body
	post := Post{Title: "EtherFlow Go", Body: "Hello from Go!", UserID: 1}
	var created map[string]any
	if err := client.Post(ctx, "/posts", post, &created); err != nil {
		panic(err)
	}
	fmt.Printf("Created post ID: %v\n", created["id"])

	// 5. Health check
	health, _ := client.CheckHealth(ctx)
	fmt.Printf("Status: %v\n", health["status"])
}
*/
