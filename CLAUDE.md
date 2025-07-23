# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Kotlin-based MCP (Model Context Protocol) server project called `listmonk-mcp` designed to communicate with Listmonk through its API. Listmonk is an open-source newsletter and mailing list manager. The project uses Gradle with Kotlin DSL and targets JVM.

## Build Commands

```bash
# Build the project
./gradlew build

# Run tests
./gradlew test

# Clean build artifacts
./gradlew clean

# Run the application (when source code exists)
./gradlew run
```

## Project Structure

The project follows standard Kotlin/Gradle conventions:
- `src/main/kotlin/` - Main source code (currently empty)
- `src/main/resources/` - Resources (currently empty)  
- `src/test/kotlin/` - Test code (currently empty)
- `build.gradle.kts` - Build configuration
- `settings.gradle.kts` - Project settings

## Intended Architecture

The project will implement an MCP server for Listmonk API integration with the following architecture:

### Core Components
- **Main** - Application entry point
- **ListmonkMcpServer** - Main MCP server implementation with tools, prompts, and logging capabilities
- **ListmonkService** - Listmonk API integration for newsletter and subscriber management
- **StdioTransport** - MCP protocol communication over STDIO

### Package Structure (planned)
```
io.devexpert.mcp/
├── ListmonkMcpServer
├── Main
└── tools/ListmonkTools

io.devexpert.listmonk/
└── ListmonkService

io.devexpert.transport/
└── StdioTransport
```

### Planned MCP Tools
The server will provide tools for Listmonk operations:
- **Subscriber Management**: Add, update, delete, and search subscribers
- **List Management**: Create, update, and manage mailing lists
- **Campaign Operations**: Create, send, and manage email campaigns
- **Template Management**: Manage email templates
- **Analytics**: Retrieve campaign statistics and metrics

## Configuration

- **Group ID**: `io.devexpert`
- **Version**: `1.0-SNAPSHOT`
- **Kotlin Version**: `2.1.21`
- **Code Style**: Official Kotlin style
- **Target**: JVM platform with JUnit Platform for testing

## Listmonk API Integration

The MCP server will integrate with Listmonk's REST API endpoints:
- **Base URL**: Configurable Listmonk instance URL
- **Authentication**: Basic Auth or API key authentication
- **API Documentation**: Follows Listmonk's OpenAPI specification
- **Content Types**: JSON for data exchange