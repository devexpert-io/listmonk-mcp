package io.devexpert.listmonk.config

import kotlinx.serialization.Serializable

@Serializable
data class ListmonkConfig(
    val baseUrl: String,
    val username: String,
    val apiKey: String,
    val timeout: Long = 30000L,
    val retryCount: Int = 3
) {
    
    init {
        require(baseUrl.isNotBlank()) { "Base URL cannot be blank" }
        require(username.isNotBlank()) { "Username cannot be blank" }
        require(apiKey.isNotBlank()) { "API key cannot be blank" }
        require(timeout > 0) { "Timeout must be positive" }
        require(retryCount >= 0) { "Retry count must be non-negative" }
    }
    
    val normalizedBaseUrl: String
        get() = baseUrl.trimEnd('/')
        
    companion object {
        fun fromEnvironment(): ListmonkConfig {
            val baseUrl = System.getenv("LISTMONK_BASE_URL") 
                ?: throw IllegalArgumentException("LISTMONK_BASE_URL environment variable is required")
            val username = System.getenv("LISTMONK_USERNAME") ?: "api"
            val apiKey = System.getenv("LISTMONK_API_KEY")
                ?: throw IllegalArgumentException("LISTMONK_API_KEY environment variable is required")
            val timeout = System.getenv("LISTMONK_TIMEOUT")?.toLongOrNull() ?: 30000L
            val retryCount = System.getenv("LISTMONK_RETRY_COUNT")?.toIntOrNull() ?: 3
            
            return ListmonkConfig(
                baseUrl = baseUrl,
                username = username,
                apiKey = apiKey,
                timeout = timeout,
                retryCount = retryCount
            )
        }
    }
}