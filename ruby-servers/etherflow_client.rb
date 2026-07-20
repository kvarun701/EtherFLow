# etherflow_client.rb
# EtherFlow HTTP Client for Ruby — Net::HTTP + JSON
#
# A fluent, retrying HTTP client for Ruby that mirrors the EtherFlow
# Java/Kotlin builder API. Zero gem dependencies — uses only the standard library.
#
# Usage:
#   client = EtherFlow::Client.builder
#               .base_url("https://api.example.com")
#               .retry(3)
#               .build
#
#   user = client.get("/users/1")          # => Hash
#   post = client.post("/posts", title: "Hello", userId: 1)

require 'net/http'
require 'json'
require 'uri'

module EtherFlow
  # ─────────────────────────────────────────────────────────────────────────────
  # Error
  # ─────────────────────────────────────────────────────────────────────────────

  class Error < StandardError
    attr_reader :status_code, :response_body

    def initialize(message, status_code: nil, response_body: nil)
      super(message)
      @status_code   = status_code
      @response_body = response_body
    end

    def to_s
      msg = super
      msg += " (HTTP #{@status_code})" if @status_code
      msg
    end
  end

  # ─────────────────────────────────────────────────────────────────────────────
  # Config
  # ─────────────────────────────────────────────────────────────────────────────

  Config = Struct.new(
    :base_url,
    :timeout_seconds,
    :max_retries,
    :retry_delay_seconds,
    :default_headers,
    keyword_init: true
  ) do
    def self.default
      new(
        base_url:             '',
        timeout_seconds:      10,
        max_retries:          3,
        retry_delay_seconds:  0.2,
        default_headers:      {}
      )
    end
  end

  # ─────────────────────────────────────────────────────────────────────────────
  # Client
  # ─────────────────────────────────────────────────────────────────────────────

  class Client
    def initialize(config = Config.default)
      @config = config
    end

    # ── Builder ────────────────────────────────────────────────────────────────

    def self.builder
      Builder.new
    end

    def self.create(base_url)
      new(Config.default.tap { |c| c.base_url = base_url })
    end

    # ── Public API ─────────────────────────────────────────────────────────────

    # GET request — returns parsed JSON (Hash or Array).
    def get(path, headers: {})
      execute(:Get, path, body: nil, headers: headers)
    end

    # POST request — body is a Hash (will be JSON-serialised).
    def post(path, body = {}, headers: {})
      execute(:Post, path, body: body, headers: headers)
    end

    # PUT request.
    def put(path, body = {}, headers: {})
      execute(:Put, path, body: body, headers: headers)
    end

    # PATCH request.
    def patch(path, body = {}, headers: {})
      execute(:Patch, path, body: body, headers: headers)
    end

    # DELETE request.
    def delete(path, headers: {})
      execute(:Delete, path, body: nil, headers: headers)
    end

    # Safe variant — returns [:ok, data] or [:error, EtherFlow::Error].
    def get_result(path, headers: {})
      [:ok, get(path, headers: headers)]
    rescue Error => e
      [:error, e]
    end

    # Health check — calls /health on the base URL.
    def check_health
      get('/health')
    rescue Error
      { 'status' => 'DOWN', 'error' => 'Health check failed' }
    end

    private

    # ── Internal ───────────────────────────────────────────────────────────────

    def resolve_url(path)
      return path if path.start_with?('http://', 'https://')
      base = @config.base_url.chomp('/')
      "#{base}#{path}"
    end

    def build_headers(extra)
      {
        'Accept'       => 'application/json',
        'Content-Type' => 'application/json',
        'User-Agent'   => 'EtherFlow-Ruby-Client/1.0'
      }.merge(@config.default_headers).merge(extra)
    end

    def execute(method_sym, path, body:, headers:)
      url          = URI(resolve_url(path))
      all_headers  = build_headers(headers)
      body_json    = body ? JSON.generate(body) : nil

      last_error = nil
      delay      = @config.retry_delay_seconds

      (@config.max_retries + 1).times do |attempt|
        begin
          response = Net::HTTP.start(url.host, url.port,
                                     use_ssl: url.scheme == 'https',
                                     open_timeout: @config.timeout_seconds,
                                     read_timeout: @config.timeout_seconds) do |http|
            klass   = Net::HTTP.const_get(method_sym)
            request = klass.new(url)
            all_headers.each { |k, v| request[k] = v }
            request.body = body_json if body_json
            http.request(request)
          end

          code = response.code.to_i

          if (200..299).cover?(code)
            return JSON.parse(response.body)
          end

          err = Error.new("[EtherFlow] HTTP #{code}: #{response.message}",
                          status_code: code, response_body: response.body)

          # Don't retry 4xx
          raise err if (400..499).cover?(code)
          last_error = err

        rescue Error => e
          raise e if e.status_code && (400..499).cover?(e.status_code)
          last_error = e
        rescue => e
          last_error = Error.new("[EtherFlow] Network error: #{e.message}")
        end

        if attempt < @config.max_retries
          warn "[EtherFlow.Ruby] Retry #{attempt + 1}/#{@config.max_retries} after #{delay}s: #{last_error&.message}"
          sleep delay
          delay *= 2
        end
      end

      raise Error.new("[EtherFlow] Max retries exceeded. Last: #{last_error&.message}")
    end
  end

  # ─────────────────────────────────────────────────────────────────────────────
  # Builder
  # ─────────────────────────────────────────────────────────────────────────────

  class Builder
    def initialize
      @config = Config.default
    end

    def base_url(url)
      @config.base_url = url
      self
    end

    def timeout(seconds)
      @config.timeout_seconds = seconds
      self
    end

    def retry(count)
      @config.max_retries = count
      self
    end

    def retry_delay(seconds)
      @config.retry_delay_seconds = seconds
      self
    end

    def header(key, value)
      @config.default_headers[key] = value
      self
    end

    def build
      Client.new(@config)
    end
  end
end

# ─────────────────────────────────────────────────────────────────────────────
# Example Usage
# ─────────────────────────────────────────────────────────────────────────────

if __FILE__ == $0
  # 1. Create client using builder (mirrors Java API)
  client = EtherFlow::Client.builder
               .base_url('https://jsonplaceholder.typicode.com')
               .retry(3)
               .timeout(10)
               .build

  # 2. GET — returns a Hash
  user = client.get('/users/1')
  puts "User: #{user['name']} — #{user['email']}"

  # 3. GET list
  users = client.get('/users')
  puts "Total users: #{users.length}"

  # 4. POST with body
  created = client.post('/posts', title: 'EtherFlow Ruby', body: 'Hello!', userId: 1)
  puts "Created post ID: #{created['id']}"

  # 5. Safe result — never raises
  status, result = client.get_result('/users/999')
  if status == :ok
    puts "Found: #{result['name']}"
  else
    puts "Error: #{result.message}"
  end

  # 6. Health check
  health = client.check_health
  puts "Service status: #{health['status']}"
end
