using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Builder;
using Microsoft.AspNetCore.Http;
using Microsoft.Extensions.Hosting;
using EtherFlow.Client;

var builder = WebApplication.CreateBuilder(args);
var app = builder.Build();

// Instantiate EtherFlow Client in .NET targeting external APIs
var etherFlowClient = EtherFlowClient.Create("https://jsonplaceholder.typicode.com");

// ---------------------------------------------------------
// 1. .NET Web API Endpoints using EtherFlow Client
// ---------------------------------------------------------
app.MapGet("/api/dotnet/health", () => Results.Ok(new
{
    status = "UP",
    framework = ".NET Core 8.0",
    etherFlowClient = "Active (EtherFlow.Client)",
    timestamp = DateTime.UtcNow
}));

// 2. Calling Third-Party API from .NET using EtherFlowClient
app.MapGet("/api/dotnet/external-user/{id}", async (int id) =>
{
    try
    {
        // Calling third-party API via EtherFlow Client in C#
        var externalUser = await etherFlowClient.GetAsync<Dictionary<string, object>>($"/users/{id}");
        return Results.Ok(new
        {
            source = ".NET -> EtherFlowClient -> Third-Party API",
            user = externalUser
        });
    }
    catch (Exception ex)
    {
        return Results.Problem($"EtherFlow Client Error: {ex.Message}");
    }
});

app.MapPost("/api/dotnet/external-post", async (PostPayload payload) =>
{
    try
    {
        // Calling third-party POST API via EtherFlow Client in C#
        var response = await etherFlowClient.PostAsync<Dictionary<string, object>>("/posts", payload);
        return Results.Created("/api/dotnet/external-post", new
        {
            source = ".NET -> EtherFlowClient -> Third-Party API",
            remoteResult = response
        });
    }
    catch (Exception ex)
    {
        return Results.Problem($"EtherFlow Client Error: {ex.Message}");
    }
});

Console.WriteLine("================================================================");
Console.WriteLine(" 🚀 .NET Application running EtherFlow Client on http://localhost:5003");
Console.WriteLine("================================================================");
app.Run("http://localhost:5003");

public record PostPayload(string Title, string Body, int UserId);
