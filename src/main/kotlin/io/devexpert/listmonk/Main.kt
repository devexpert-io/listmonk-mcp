package io.devexpert.listmonk

import io.devexpert.listmonk.config.ListmonkConfig
import io.devexpert.listmonk.transport.StdioTransport
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

private val logger = LoggerFactory.getLogger("MainKt")

fun main() {
    logger.info("Starting Listmonk MCP Server...")
    
    try {
        // Load configuration from environment variables
        val config = ListmonkConfig.fromEnvironment()
        
        logger.info("Configuration loaded successfully")
        logger.info("Listmonk Configuration:")
        logger.info("- Base URL: ${config.normalizedBaseUrl}")
        logger.info("- Timeout: ${config.timeout}ms")
        logger.info("- Retry Count: ${config.retryCount}")
        
        // Create the MCP server
        val mcpServer = ListmonkMcpServer(config)
        
        // Set up shutdown hook
        Runtime.getRuntime().addShutdownHook(Thread {
            logger.info("Shutdown signal received, closing server...")
            mcpServer.close()
        })
        
        // Start the server with STDIO transport (this will block)
        val transport = StdioTransport()
        transport.startServer(mcpServer.getServer())
        
    } catch (e: IllegalArgumentException) {
        logger.error("Configuration error: ${e.message}")
        logger.error("Please ensure the following environment variables are set:")
        logger.error("- LISTMONK_BASE_URL: Your Listmonk instance URL (e.g., http://localhost:9000)")
        logger.error("- LISTMONK_API_KEY: Your Listmonk API key")
        logger.error("Optional:")
        logger.error("- LISTMONK_TIMEOUT: Request timeout in milliseconds (default: 30000)")
        logger.error("- LISTMONK_RETRY_COUNT: Number of retries for failed requests (default: 3)")
        exitProcess(1)
    } catch (e: Exception) {
        logger.error("Failed to start Listmonk MCP Server: ${e.message}", e)
        exitProcess(1)
    }
}