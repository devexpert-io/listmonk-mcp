# Listmonk MCP Server

A Model Context Protocol (MCP) server for integrating with Listmonk, the self-hosted newsletter and mailing list manager.

## Features

This MCP server provides comprehensive tools for managing Listmonk through AI assistants:

### Subscriber Management
- **get_subscribers** - Retrieve subscribers with filtering and pagination
- **get_subscriber** - Get details of a specific subscriber
- **create_subscriber** - Add new subscribers to lists
- **update_subscriber** - Modify subscriber information and list memberships
- **delete_subscriber** - Remove subscribers

### Mailing List Management
- **get_lists** - Retrieve all mailing lists
- **get_list** - Get details of a specific mailing list
- **create_list** - Create new mailing lists (public/private)
- **update_list** - Modify list properties and settings
- **delete_list** - Remove mailing lists

### Campaign Management
- **get_campaigns** - Retrieve campaigns with filtering
- **get_campaign** - Get details of a specific campaign
- **create_campaign** - Create new email campaigns with timezone-aware scheduling
- **update_campaign_status** - Change campaign status (draft, scheduled, running, etc.)
- **delete_campaign** - Remove campaigns

### Template Management
- **get_templates** - Retrieve all email templates
- **get_template** - Get details of a specific template
- **get_template_preview** - Get HTML preview of a template
- **create_template** - Create new email templates
- **update_template** - Modify existing templates
- **set_default_template** - Set a template as default
- **delete_template** - Remove templates

### Guided Workflows
The server includes helpful prompts for common workflows:
- **manage_subscriber_lifecycle** - Complete subscriber management guide
- **create_campaign_workflow** - Step-by-step campaign creation
- **list_management_guide** - Mailing list setup and management

## Configuration

The server is configured through environment variables:

### Required Variables
- `LISTMONK_BASE_URL` - Your Listmonk instance URL (e.g., `http://localhost:9000`)
- `LISTMONK_API_KEY` - Your Listmonk API key

### Optional Variables
- `LISTMONK_USERNAME` - Basic Auth username (default: "api")
- `LISTMONK_TIMEOUT` - Request timeout in milliseconds (default: 30000)
- `LISTMONK_RETRY_COUNT` - Number of retries for failed requests (default: 3)

## Setup

### 1. Listmonk API Key

First, create an API user in your Listmonk instance:

1. Go to your Listmonk admin panel
2. Navigate to **Admin → Users**
3. Create a new user with API permissions
4. Copy the generated API key

### 2. Environment Configuration

Create a `.env` file or set environment variables:

```bash
export LISTMONK_BASE_URL="http://localhost:9000"
export LISTMONK_API_KEY="your-api-key-here"
export LISTMONK_USERNAME="api"  # Optional, defaults to "api"
export LISTMONK_TIMEOUT="30000"
export LISTMONK_RETRY_COUNT="3"
```

### 3. Running the Server

#### Using Gradle

```bash
# Build the project
./gradlew build

# Run the server
./gradlew run
```

#### Using Docker (if Dockerfile is available)

```bash
docker build -t listmonk-mcp .
docker run -e LISTMONK_BASE_URL="http://localhost:9000" \
           -e LISTMONK_API_KEY="your-api-key" \
           -e LISTMONK_USERNAME="api" \
           listmonk-mcp
```

## Usage with Claude Desktop

Add the server to your Claude Desktop configuration:

```json
{
  "mcpServers": {
    "listmonk": {
      "command": "java",
      "args": ["-jar", "/path/to/listmonk-mcp-all.jar"],
      "env": {
        "LISTMONK_BASE_URL": "http://localhost:9000",
        "LISTMONK_API_KEY": "your-api-key-here",
        "LISTMONK_USERNAME": "api"
      }
    }
  }
}
```

## Example Interactions

### Creating a Subscriber

```
Use the create_subscriber tool to add a new subscriber:
- Email: john@example.com
- Name: John Doe
- Status: enabled
- Lists: [1, 2]
```

### Creating a Campaign

```
Use the create_campaign tool to create a newsletter:
- Name: Monthly Newsletter
- Subject: Our Latest Updates
- Lists: [1]
- Content Type: richtext
- Body: Your campaign content here
- Send At: "19:00" or "2024-07-24T19:00:00" (uses local timezone)
```

### Managing Lists

```
Use the create_list tool to create a new mailing list:
- Name: Product Updates
- Type: public
- Opt-in: double
- Description: Updates about our products
```

## API Compatibility

This MCP server is compatible with Listmonk v2.0+ and uses the official Listmonk REST API endpoints:

- `/api/subscribers` - Subscriber management
- `/api/lists` - Mailing list operations  
- `/api/campaigns` - Campaign management
- `/api/templates` - Template management

## Key Features

### Timezone-Aware Scheduling
When creating campaigns with `send_at`, you can use local time formats:
- `"19:00"` - Today at 7 PM in your timezone
- `"2024-07-24T19:00:00"` - Specific date and time in your timezone
- `"2024-07-24 19:00:00"` - Alternative format with space

The server automatically converts to UTC for Listmonk.

### Robust Array Parameter Handling
All array parameters (lists, tags) support both direct arrays and JSON string formats for maximum compatibility with different MCP clients.

## Logging

Logs are written to both console and files:
- Console output for real-time monitoring
- Rolling log files in `logs/listmonk-mcp.log`
- Configurable log levels in `src/main/resources/logback.xml`

## Development

### Project Structure

```
src/main/kotlin/io/devexpert/listmonk/
├── config/
│   └── ListmonkConfig.kt        # Configuration management
├── model/
│   └── ListmonkModels.kt        # Data models and serialization
├── service/
│   └── ListmonkService.kt       # HTTP client for Listmonk API
├── tools/
│   └── ListmonkTools.kt         # MCP tool definitions
├── ListmonkMcpServer.kt         # Main MCP server implementation
└── Main.kt                      # Application entry point
```

### Building

```bash
# Clean and build
./gradlew clean build

# Run tests
./gradlew test

# Generate fat JAR
./gradlew shadowJar
```

## Troubleshooting

### Common Issues

1. **Authentication Errors**: Verify your API key is correct and the user has proper permissions
2. **Connection Errors**: Check that `LISTMONK_BASE_URL` is accessible and correct
3. **Timeout Issues**: Increase `LISTMONK_TIMEOUT` for slower networks
4. **SSL Errors**: Ensure proper SSL configuration if using HTTPS

### Debug Logging

Enable debug logging by setting the root logger level to `DEBUG` in `logback.xml`:

```xml
<root level="DEBUG">
    <appender-ref ref="STDOUT" />
    <appender-ref ref="FILE" />
</root>
```

## License

This project is licensed under the MIT License.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## Support

For issues and questions:
- Check the [Listmonk documentation](https://listmonk.app/docs/)
- Review the [MCP specification](https://modelcontextprotocol.io/)
- Open an issue in this repository