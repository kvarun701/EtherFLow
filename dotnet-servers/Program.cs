using Microsoft.AspNetCore.Builder;
using Microsoft.AspNetCore.Http;
using Microsoft.Extensions.Hosting;
using System;
using System.Collections.Generic;

var builder = WebApplication.CreateBuilder(args);
var app = builder.Build();

app.MapGet("/api/dotnet/health", () => Results.Ok(new
{
    status = "UP",
    framework = ".NET Core / ASP.NET Core Web API",
    message = ".NET API server is running smoothly",
    timestamp = DateTime.UtcNow
}));

app.MapGet("/api/dotnet/hello", (string? name) => Results.Ok(new
{
    service = ".NET Web API",
    greeting = $"Hello, {name ?? "EtherFlow User"} from .NET Framework / .NET Core!",
    version = "8.0"
}));

app.MapGet("/api/dotnet/products/{id}", (string id) => Results.Ok(new
{
    id = id,
    name = $".NET Product {id}",
    category = "Enterprise Software",
    price = 299.99,
    inStock = true
}));

app.MapPost("/api/dotnet/process", (ProcessRequest request) =>
{
    var count = request.Data?.Count ?? 0;
    return Results.Ok(new
    {
        status = "success",
        framework = ".NET Web API",
        processedItems = count,
        taskName = request.TaskName ?? "DefaultTask",
        resultCode = 200
    });
});

Console.WriteLine("Starting .NET Web API server on http://localhost:5003...");
app.Run("http://localhost:5003");

public record ProcessRequest(string? TaskName, List<int>? Data);
