using System;
using System.Collections.Generic;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using System.Threading.Tasks;

namespace EtherFlow.Client
{
    /// <summary>
    /// EtherFlow Reactive & Asynchronous HTTP Client for C# .NET Application Developers.
    /// Provides fluent, non-blocking API invocation with retries, JSON serialization, and error handling.
    /// </summary>
    public class EtherFlowClient
    {
        private readonly HttpClient _httpClient;
        private readonly string _baseUrl;
        private readonly int _maxRetries;
        private readonly JsonSerializerOptions _jsonOptions;

        public EtherFlowClient(string baseUrl = "", int timeoutSeconds = 10, int maxRetries = 3)
        {
            _baseUrl = baseUrl.TrimEnd('/');
            _maxRetries = maxRetries;
            _httpClient = new HttpClient
            {
                Timeout = TimeSpan.FromSeconds(timeoutSeconds)
            };
            _httpClient.DefaultRequestHeaders.Accept.Add(new MediaTypeWithQualityHeaderValue("application/json"));
            _httpClient.DefaultRequestHeaders.Add("User-Agent", "EtherFlow-DotNet-Client/1.0");

            _jsonOptions = new JsonSerializerOptions
            {
                PropertyNameCaseInsensitive = true,
                PropertyNamingPolicy = JsonNamingPolicy.CamelCase
            };
        }

        public static EtherFlowClient Create(string baseUrl = "")
        {
            return new EtherFlowClient(baseUrl);
        }

        /// <summary>
        /// Executes an asynchronous GET request and deserializes the JSON response into type T.
        /// </summary>
        public async Task<T?> GetAsync<T>(string uri, Dictionary<string, string>? headers = null)
        {
            string url = ResolveUrl(uri);
            return await ExecuteWithRetryAsync(async () =>
            {
                using var request = new HttpRequestMessage(HttpMethod.Get, url);
                AddHeaders(request, headers);

                using var response = await _httpClient.SendAsync(request);
                response.EnsureSuccessStatusCode();

                string json = await response.Content.ReadAsStringAsync();
                return JsonSerializer.Deserialize<T>(json, _jsonOptions);
            });
        }

        /// <summary>
        /// Executes an asynchronous POST request sending a JSON body and deserializing response into TResponse.
        /// </summary>
        public async Task<TResponse?> PostAsync<TResponse>(string uri, object body, Dictionary<string, string>? headers = null)
        {
            string url = ResolveUrl(uri);
            string jsonBody = JsonSerializer.Serialize(body, _jsonOptions);

            return await ExecuteWithRetryAsync(async () =>
            {
                using var request = new HttpRequestMessage(HttpMethod.Post, url)
                {
                    Content = new StringContent(jsonBody, Encoding.UTF8, "application/json")
                };
                AddHeaders(request, headers);

                using var response = await _httpClient.SendAsync(request);
                response.EnsureSuccessStatusCode();

                string responseJson = await response.Content.ReadAsStringAsync();
                return JsonSerializer.Deserialize<TResponse>(responseJson, _jsonOptions);
            });
        }

        /// <summary>
        /// Executes an asynchronous PUT request.
        /// </summary>
        public async Task<TResponse?> PutAsync<TResponse>(string uri, object body, Dictionary<string, string>? headers = null)
        {
            string url = ResolveUrl(uri);
            string jsonBody = JsonSerializer.Serialize(body, _jsonOptions);

            return await ExecuteWithRetryAsync(async () =>
            {
                using var request = new HttpRequestMessage(HttpMethod.Put, url)
                {
                    Content = new StringContent(jsonBody, Encoding.UTF8, "application/json")
                };
                AddHeaders(request, headers);

                using var response = await _httpClient.SendAsync(request);
                response.EnsureSuccessStatusCode();

                string responseJson = await response.Content.ReadAsStringAsync();
                return JsonSerializer.Deserialize<TResponse>(responseJson, _jsonOptions);
            });
        }

        /// <summary>
        /// Executes an asynchronous DELETE request.
        /// </summary>
        public async Task<TResponse?> DeleteAsync<TResponse>(string uri, Dictionary<string, string>? headers = null)
        {
            string url = ResolveUrl(uri);
            return await ExecuteWithRetryAsync(async () =>
            {
                using var request = new HttpRequestMessage(HttpMethod.Delete, url);
                AddHeaders(request, headers);

                using var response = await _httpClient.SendAsync(request);
                response.EnsureSuccessStatusCode();

                string responseJson = await response.Content.ReadAsStringAsync();
                return JsonSerializer.Deserialize<TResponse>(responseJson, _jsonOptions);
            });
        }

        private string ResolveUrl(string uri)
        {
            if (uri.StartsWith("http://") || uri.StartsWith("https://"))
                return uri;
            return string.IsNullOrEmpty(_baseUrl) ? uri : $"{_baseUrl}/{(uri.StartsWith("/") ? uri.Substring(1) : uri)}";
        }

        private void AddHeaders(HttpRequestMessage request, Dictionary<string, string>? headers)
        {
            if (headers != null)
            {
                foreach (var kvp in headers)
                {
                    request.Headers.TryAddWithoutValidation(kvp.Key, kvp.Value);
                }
            }
        }

        private async Task<T> ExecuteWithRetryAsync<T>(Func<Task<T>> action)
        {
            int attempts = 0;
            while (true)
            {
                try
                {
                    attempts++;
                    return await action();
                }
                catch (Exception ex) when (attempts <= _maxRetries)
                {
                    Console.WriteLine($"[EtherFlow.Client] Retry attempt {attempts}/{_maxRetries} after error: {ex.Message}");
                    await Task.Delay(TimeSpan.FromMilliseconds(200 * Math.Pow(2, attempts - 1)));
                }
            }
        }
    }
}
