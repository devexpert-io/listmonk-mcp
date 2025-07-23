# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a fully implemented Kotlin-based MCP (Model Context Protocol) server called `listmonk-mcp` that provides comprehensive integration with Listmonk through its REST API. Listmonk is an open-source newsletter and mailing list manager. The project uses modern Gradle with version catalogs, Kotlin DSL, and targets JVM.

## Build Commands

```bash
# Build the project
./gradlew build

# Run tests  
./gradlew test

# Clean build artifacts
./gradlew clean

# Run the MCP server
./gradlew run

# Build shadow JAR (fat JAR with all dependencies)
./gradlew shadowJar

# Generated JARs location
# build/libs/listmonk-mcp-1.0.0.jar (regular JAR)
# build/libs/listmonk-mcp-all.jar (shadow JAR)
```

## Project Structure

Completed implementation with full source code:

```
src/main/kotlin/io/devexpert/listmonk/
├── Main.kt                           # Application entry point
├── ListmonkMcpServer.kt             # Main MCP server implementation
├── config/
│   └── ListmonkConfig.kt            # Configuration management via env vars
├── model/
│   └── ListmonkModels.kt            # Data models and serialization classes
├── service/
│   └── ListmonkService.kt           # HTTP client for Listmonk API
├── tools/
│   └── ListmonkTools.kt             # 15 MCP tools implementation
└── transport/
    └── StdioTransport.kt            # STDIO transport for MCP protocol

src/main/resources/
└── logback.xml                      # Logging configuration

gradle/libs.versions.toml             # Version catalog for dependencies
build.gradle.kts                     # Modern Gradle build with libs catalog
```

## Implemented MCP Tools (15 total)

### Subscriber Management (5 tools)
- `get_subscribers` - List subscribers with filtering (page, per_page, query, list_id, status)
- `get_subscriber` - Get specific subscriber details by ID
- `create_subscriber` - Create new subscribers with email, name, status, lists
- `update_subscriber` - Update subscriber information and list memberships
- `delete_subscriber` - Remove subscribers by ID

### List Management (5 tools)
- `get_lists` - List mailing lists with filtering (page, per_page, query, tag)
- `get_list` - Get specific list details by ID
- `create_list` - Create new lists (public/private, single/double opt-in)
- `update_list` - Update list properties, type, opt-in settings
- `delete_list` - Remove mailing lists by ID

### Campaign Management (5 tools)
- `get_campaigns` - List campaigns with filtering (page, per_page, query, status)
- `get_campaign` - Get specific campaign details by ID
- `create_campaign` - Create new email campaigns with content, lists, scheduling
- `update_campaign_status` - Change campaign status (draft/scheduled/running/paused/finished/cancelled)
- `delete_campaign` - Remove campaigns by ID

## Configuration

### Environment Variables (Required)
The MCP server requires these environment variables:

```bash
# Required
LISTMONK_BASE_URL="http://localhost:9000"    # Your Listmonk instance URL
LISTMONK_API_KEY="your-api-key-here"         # Your Listmonk API key

# Optional
LISTMONK_USERNAME="api"                      # Basic Auth username (default: "api")
LISTMONK_TIMEOUT="30000"                     # Request timeout in ms (default: 30000)
LISTMONK_RETRY_COUNT="3"                     # Retry count for failed requests (default: 3)
```

### Project Configuration
- **Group ID**: `io.devexpert`
- **Version**: `1.0.0`
- **Kotlin Version**: `2.2.0`
- **MCP SDK Version**: `0.5.0`
- **Code Style**: Official Kotlin style
- **Target**: JVM platform with JUnit Platform for testing

## Listmonk API Integration

The MCP server integrates with Listmonk's REST API:
- **Authentication**: Basic Auth with API key (format: `api:your-key`)
- **Content Types**: JSON request/response bodies
- **HTTP Client**: Ktor with content negotiation, auth, and logging
- **API Endpoints**: `/api/subscribers`, `/api/lists`, `/api/campaigns`
- **Error Handling**: Comprehensive error responses with details

## Key Dependencies

```kotlin
// MCP Core
io.modelcontextprotocol:kotlin-sdk:0.5.0

// HTTP Client  
io.ktor:ktor-client-core:2.3.12
io.ktor:ktor-client-cio:2.3.12
io.ktor:ktor-client-auth:2.3.12

// Serialization
org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0

// Logging
ch.qos.logback:logback-classic:1.5.18
```

## Development Notes

- **MCP SDK**: Uses official MCP Kotlin SDK v0.5.0 with STDIO transport
- **Architecture**: Clean separation of concerns with config, service, tools, and transport layers
- **Error Handling**: All API calls wrapped in Result types with detailed error messages
- **Logging**: Structured logging with SLF4J + Logback, configurable levels
- **Testing**: Framework ready (JUnit Jupiter + Ktor mock client)
- **Building**: Modern Gradle with version catalogs, shadow plugin for fat JARs

## Usage with Claude Desktop

Add to Claude Desktop configuration:

```json
{
  "mcpServers": {
    "listmonk": {
      "command": "java",
      "args": ["-jar", "/path/to/listmonk-mcp-all.jar"],
      "env": {
        "LISTMONK_BASE_URL": "http://localhost:9000",
        "LISTMONK_USERNAME": "your-username",
        "LISTMONK_API_KEY": "your-api-key"
      }
    }
  }
}
```