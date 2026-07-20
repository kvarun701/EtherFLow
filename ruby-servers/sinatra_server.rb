# sinatra_server.rb
# EtherFlow-compatible REST API server using Sinatra (Ruby)
#
# Exposes the same endpoints as Python Flask/FastAPI servers so EtherFlow
# Java/Kotlin clients can call them seamlessly.
#
# Run with:
#   bundle exec ruby sinatra_server.rb
#   # or without Bundler:
#   ruby sinatra_server.rb
#
# Port: 5007
#
# Endpoints:
#   GET  /api/ruby/health
#   GET  /api/ruby/hello?name=Alice
#   GET  /api/ruby/users/:id
#   POST /api/ruby/predict
#   POST /api/ruby/external-post

require 'sinatra'
require 'json'
require_relative 'etherflow_client'

set :port, 5_007
set :bind, '0.0.0.0'

# ─── Middleware ───────────────────────────────────────────────────────────────

before do
  content_type :json
  puts "[EtherFlow.Ruby] #{request.request_method} #{request.path}"
end

# ─── Health ──────────────────────────────────────────────────────────────────

get '/api/ruby/health' do
  {
    status:    'UP',
    framework: 'Sinatra (Ruby)',
    message:   'Ruby Sinatra API server is running smoothly via EtherFlow'
  }.to_json
end

# ─── Hello ───────────────────────────────────────────────────────────────────

get '/api/ruby/hello' do
  name = params['name'] || 'EtherFlow User'
  {
    service:   'Ruby Sinatra API',
    greeting:  "Hello, #{name} from Ruby + Sinatra!",
    framework: 'Sinatra 3.x / Ruby 3.x'
  }.to_json
end

# ─── User by ID ──────────────────────────────────────────────────────────────

get '/api/ruby/users/:id' do
  id = params[:id].to_i
  halt 400, { error: 'Invalid user ID' }.to_json if id <= 0

  {
    id:     id,
    name:   "Ruby User #{id}",
    role:   'Developer',
    active: true
  }.to_json
end

# ─── ML Predict ──────────────────────────────────────────────────────────────

post '/api/ruby/predict' do
  data   = JSON.parse(request.body.read) rescue {}
  inputs = data['inputs'] || [10, 20, 30]
  total  = inputs.sum.to_f
  avg    = inputs.empty? ? 0.0 : total / inputs.size

  {
    status: 'success',
    inputs: inputs,
    prediction: {
      sum:     total,
      average: avg,
      score:   (avg * 1.5).round(2)
    }
  }.to_json
end

# ─── External post (calls JSONPlaceholder via EtherFlowClient) ────────────────

post '/api/ruby/external-post' do
  payload = JSON.parse(request.body.read) rescue { 'title' => 'Default Ruby Post' }

  begin
    client  = EtherFlow::Client.create('https://jsonplaceholder.typicode.com')
    created = client.post('/posts', **payload.transform_keys(&:to_sym))

    status 201
    {
      status:         'success',
      source:         'Ruby Sinatra → Third-Party API',
      remoteResponse: created
    }.to_json
  rescue EtherFlow::Error => e
    status 502
    { status: 'error', message: e.message }.to_json
  end
end

# ─── Error handlers ───────────────────────────────────────────────────────────

not_found { { error: 'Resource Not Found', status: 404 }.to_json }
error     { { error: 'Internal Server Error', status: 500 }.to_json }

puts "Ruby Sinatra API server running on http://localhost:5007"
