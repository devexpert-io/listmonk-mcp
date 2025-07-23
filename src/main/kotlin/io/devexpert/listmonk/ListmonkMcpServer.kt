package io.devexpert.listmonk

import io.devexpert.listmonk.config.ListmonkConfig
import io.devexpert.listmonk.service.ListmonkService
import io.devexpert.listmonk.tools.ListmonkTools
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import org.slf4j.LoggerFactory

class ListmonkMcpServer(private val config: ListmonkConfig) {
    private val logger = LoggerFactory.getLogger(ListmonkMcpServer::class.java)
    
    private val listmonkService = ListmonkService(config)
    private val listmonkTools = ListmonkTools(listmonkService)
    
    private val server = Server(
        serverInfo = Implementation(
            name = "listmonk-mcp",
            version = "1.0.0"
        ),
        options = ServerOptions(
            capabilities = ServerCapabilities(
                tools = ServerCapabilities.Tools(listChanged = true),
                prompts = ServerCapabilities.Prompts(listChanged = true)
            )
        )
    )
    
    init {
        setupServer()
    }
    
    private fun setupServer() {
        logger.info("Setting up Listmonk MCP Server...")
        
        // Register all tools
        listmonkTools.registerTools(server)
        
        logger.info("Listmonk MCP Server setup complete")
    }
    
    fun getServer(): Server = server
    
    fun close() {
        logger.info("Shutting down Listmonk MCP Server...")
        listmonkService.close()
        logger.info("Listmonk MCP Server shut down complete")
    }
}