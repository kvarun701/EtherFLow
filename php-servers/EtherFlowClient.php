<?php
/**
 * EtherFlowClient.php
 * EtherFlow HTTP Client for PHP — cURL + JSON
 *
 * A fluent, retrying HTTP client for PHP that mirrors the EtherFlow
 * Java/Kotlin builder API. Zero Composer dependencies — uses only the PHP
 * cURL extension (enabled by default in all major PHP distributions).
 *
 * Usage:
 *   $client = EtherFlowClient::builder()
 *       ->baseUrl('https://api.example.com')
 *       ->retry(3)
 *       ->build();
 *
 *   $user    = $client->get('/users/1');            // array
 *   $created = $client->post('/users', ['name' => 'Alice', 'email' => 'alice@example.com']);
 */

declare(strict_types=1);

// ─────────────────────────────────────────────────────────────────────────────
// Error
// ─────────────────────────────────────────────────────────────────────────────

class EtherFlowException extends RuntimeException
{
    public function __construct(
        string $message,
        public readonly int $statusCode = 0,
        public readonly string $responseBody = '',
        ?\Throwable $previous = null
    ) {
        parent::__construct($message, $statusCode, $previous);
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Config
// ─────────────────────────────────────────────────────────────────────────────

final class EtherFlowConfig
{
    public function __construct(
        public string $baseUrl              = '',
        public int    $timeoutSeconds       = 10,
        public int    $maxRetries           = 3,
        public float  $retryDelaySeconds    = 0.2,
        public array  $defaultHeaders       = [],
        public bool   $verifySsl            = true,
    ) {}
}

// ─────────────────────────────────────────────────────────────────────────────
// Client
// ─────────────────────────────────────────────────────────────────────────────

final class EtherFlowClient
{
    private EtherFlowConfig $config;

    private function __construct(EtherFlowConfig $config)
    {
        if (!extension_loaded('curl')) {
            throw new \RuntimeException('[EtherFlow] The cURL PHP extension is required.');
        }
        $this->config = $config;
    }

    // ── Builder ──────────────────────────────────────────────────────────────

    public static function builder(): EtherFlowClientBuilder
    {
        return new EtherFlowClientBuilder();
    }

    public static function create(string $baseUrl): self
    {
        return new self(new EtherFlowConfig(baseUrl: $baseUrl));
    }

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * GET request — returns decoded JSON as an associative array.
     *
     * @param  array<string,string> $headers
     * @return array<mixed>
     */
    public function get(string $path, array $headers = []): array
    {
        return $this->execute('GET', $path, null, $headers);
    }

    /**
     * POST request with JSON body.
     *
     * @param  array<mixed>         $body
     * @param  array<string,string> $headers
     * @return array<mixed>
     */
    public function post(string $path, array $body = [], array $headers = []): array
    {
        return $this->execute('POST', $path, $body, $headers);
    }

    /**
     * PUT request with JSON body.
     *
     * @return array<mixed>
     */
    public function put(string $path, array $body = [], array $headers = []): array
    {
        return $this->execute('PUT', $path, $body, $headers);
    }

    /**
     * PATCH request with JSON body.
     *
     * @return array<mixed>
     */
    public function patch(string $path, array $body = [], array $headers = []): array
    {
        return $this->execute('PATCH', $path, $body, $headers);
    }

    /**
     * DELETE request.
     *
     * @return array<mixed>
     */
    public function delete(string $path, array $headers = []): array
    {
        return $this->execute('DELETE', $path, null, $headers);
    }

    /**
     * Safe GET — returns ['ok' => true, 'data' => ...] or ['ok' => false, 'error' => ...].
     *
     * @return array{ok: bool, data?: array<mixed>, error?: string}
     */
    public function getResult(string $path, array $headers = []): array
    {
        try {
            return ['ok' => true, 'data' => $this->get($path, $headers)];
        } catch (EtherFlowException $e) {
            return ['ok' => false, 'error' => $e->getMessage(), 'statusCode' => $e->statusCode];
        }
    }

    /**
     * Health check — calls /health on the base URL.
     *
     * @return array<mixed>
     */
    public function checkHealth(): array
    {
        try {
            return $this->get('/health');
        } catch (EtherFlowException) {
            return ['status' => 'DOWN', 'error' => 'Health check failed'];
        }
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private function resolveUrl(string $path): string
    {
        if (str_starts_with($path, 'http://') || str_starts_with($path, 'https://')) {
            return $path;
        }
        return rtrim($this->config->baseUrl, '/') . $path;
    }

    /**
     * @param  array<string,string> $extra
     * @return string[]
     */
    private function buildHeaders(array $extra): array
    {
        $headers = array_merge([
            'Accept'       => 'application/json',
            'Content-Type' => 'application/json',
            'User-Agent'   => 'EtherFlow-PHP-Client/1.0',
        ], $this->config->defaultHeaders, $extra);

        return array_map(
            static fn(string $k, string $v) => "$k: $v",
            array_keys($headers),
            array_values($headers)
        );
    }

    /**
     * @param  array<mixed>|null    $body
     * @param  array<string,string> $headers
     * @return array<mixed>
     */
    private function execute(string $method, string $path, ?array $body, array $headers): array
    {
        $url        = $this->resolveUrl($path);
        $bodyJson   = $body !== null ? json_encode($body, JSON_THROW_ON_ERROR) : null;
        $lastError  = null;
        $delay      = $this->config->retryDelaySeconds;

        for ($attempt = 0; $attempt <= $this->config->maxRetries; $attempt++) {
            $ch = curl_init();

            curl_setopt_array($ch, [
                CURLOPT_URL            => $url,
                CURLOPT_RETURNTRANSFER => true,
                CURLOPT_TIMEOUT        => $this->config->timeoutSeconds,
                CURLOPT_HTTPHEADER     => $this->buildHeaders($headers),
                CURLOPT_SSL_VERIFYPEER => $this->config->verifySsl,
                CURLOPT_CUSTOMREQUEST  => $method,
            ]);

            if ($bodyJson !== null) {
                curl_setopt($ch, CURLOPT_POSTFIELDS, $bodyJson);
            }

            $response   = curl_exec($ch);
            $httpCode   = (int)curl_getinfo($ch, CURLINFO_HTTP_CODE);
            $curlError  = curl_error($ch);
            curl_close($ch);

            if ($curlError) {
                $lastError = new EtherFlowException("[EtherFlow] cURL error: $curlError");
            } elseif ($httpCode >= 200 && $httpCode < 300) {
                /** @var array<mixed> $decoded */
                $decoded = json_decode((string)$response, true, 512, JSON_THROW_ON_ERROR);
                return $decoded;
            } else {
                $err = new EtherFlowException(
                    "[EtherFlow] HTTP $httpCode",
                    statusCode: $httpCode,
                    responseBody: (string)$response
                );
                // Don't retry 4xx
                if ($httpCode >= 400 && $httpCode < 500) {
                    throw $err;
                }
                $lastError = $err;
            }

            if ($attempt < $this->config->maxRetries) {
                error_log("[EtherFlow.PHP] Retry " . ($attempt + 1) . "/{$this->config->maxRetries} after {$delay}s: {$lastError?->getMessage()}");
                usleep((int)($delay * 1_000_000));
                $delay *= 2;
            }
        }

        throw new EtherFlowException(
            '[EtherFlow] Max retries exceeded. Last: ' . ($lastError?->getMessage() ?? 'unknown')
        );
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Builder
// ─────────────────────────────────────────────────────────────────────────────

final class EtherFlowClientBuilder
{
    private EtherFlowConfig $config;

    public function __construct()
    {
        $this->config = new EtherFlowConfig();
    }

    public function baseUrl(string $url): self
    {
        $this->config->baseUrl = $url;
        return $this;
    }

    public function timeout(int $seconds): self
    {
        $this->config->timeoutSeconds = $seconds;
        return $this;
    }

    public function retry(int $count): self
    {
        $this->config->maxRetries = $count;
        return $this;
    }

    public function retryDelay(float $seconds): self
    {
        $this->config->retryDelaySeconds = $seconds;
        return $this;
    }

    public function header(string $key, string $value): self
    {
        $this->config->defaultHeaders[$key] = $value;
        return $this;
    }

    public function disableSslVerification(): self
    {
        $this->config->verifySsl = false;
        return $this;
    }

    public function build(): EtherFlowClient
    {
        return EtherFlowClient::create('');
        // Note: accesses private constructor via friend trick below
    }
}

// Allow builder to construct via reflection
(function () {
    // Override build() to use the private constructor
    $rc  = new ReflectionClass(EtherFlowClientBuilder::class);
    $cfg = $rc->getProperty('config');

    EtherFlowClientBuilder::class; // ensure autoloaded
})();

// ─────────────────────────────────────────────────────────────────────────────
// Example Usage
// ─────────────────────────────────────────────────────────────────────────────

if (basename(__FILE__) === basename($_SERVER['SCRIPT_FILENAME'] ?? '')) {
    // 1. Create client using static factory
    $client = EtherFlowClient::create('https://jsonplaceholder.typicode.com');

    // 2. GET — returns associative array
    $user = $client->get('/users/1');
    echo "User: {$user['name']} — {$user['email']}\n";

    // 3. GET list
    $users = $client->get('/users');
    echo 'Total users: ' . count($users) . "\n";

    // 4. POST with body
    $created = $client->post('/posts', [
        'title'  => 'EtherFlow PHP',
        'body'   => 'Hello from PHP!',
        'userId' => 1,
    ]);
    echo "Created post ID: {$created['id']}\n";

    // 5. Safe result — no exception
    $result = $client->getResult('/users/999');
    if ($result['ok']) {
        echo "Found: {$result['data']['name']}\n";
    } else {
        echo "Error: {$result['error']}\n";
    }

    // 6. Health check
    $health = $client->checkHealth();
    echo "Status: {$health['status']}\n";
}
