package io.devexpert.listmonk.service

import io.devexpert.listmonk.config.ListmonkConfig
import io.devexpert.listmonk.model.*
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json

private val logger = KotlinLogging.logger {}

class ListmonkService(private val config: ListmonkConfig) {
    
    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
        
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
        }
        
        install(Auth) {
            basic {
                credentials {
                    BasicAuthCredentials(username = config.username, password = config.apiKey)
                }
                sendWithoutRequest { true }
            }
        }
        
        install(HttpTimeout) {
            requestTimeoutMillis = config.timeout
            connectTimeoutMillis = config.timeout
            socketTimeoutMillis = config.timeout
        }
        
        defaultRequest {
            headers {
                append(HttpHeaders.Accept, "application/json")
                append(HttpHeaders.ContentType, "application/json")
            }
        }
    }
    
    // Subscriber operations
    suspend fun getSubscribers(
        page: Int = 1,
        perPage: Int = 20,
        query: String? = null,
        listId: Int? = null,
        status: SubscriberStatus? = null
    ): Result<ApiResponse<PaginatedResponse<Subscriber>>> = runCatching {
        logger.info { "Getting subscribers: page=$page, perPage=$perPage" }
        
        // Temporary debug logging
        val fullUrl = "${config.normalizedBaseUrl}/api/subscribers"
        logger.info { "DEBUG: Calling endpoint: $fullUrl" }
        logger.info { "DEBUG: Username: ${config.username}" }
        logger.info { "DEBUG: API Key: ${config.apiKey}" }
        logger.info { "DEBUG: Base URL from config: ${config.normalizedBaseUrl}" }
        
        val response = httpClient.get(fullUrl) {
            parameter("page", page)
            parameter("per_page", perPage)
            query?.let { parameter("query", it) }
            listId?.let { parameter("list_id", it) }
            status?.let { parameter("status", it.name.lowercase()) }
        }
        
        try {
            response.body<ApiResponse<PaginatedResponse<Subscriber>>>()
        } catch (e: Exception) {
            val jsonResponse = response.body<String>()
            logger.error { "Failed to parse response: $jsonResponse" }
            throw Exception("Failed to parse response. JSON: $jsonResponse", e)
        }
    }
    
    suspend fun getSubscriber(id: Int): Result<ApiResponse<Subscriber>> = runCatching {
        logger.info { "Getting subscriber: id=$id" }
        
        val response = httpClient.get("${config.normalizedBaseUrl}/api/subscribers/$id")
        response.body<ApiResponse<Subscriber>>()
    }
    
    suspend fun createSubscriber(request: CreateSubscriberRequest): Result<ApiResponse<Subscriber>> = runCatching {
        logger.info { "Creating subscriber: email=${request.email}" }
        
        val response = httpClient.post("${config.normalizedBaseUrl}/api/subscribers") {
            setBody(request)
        }
        
        response.body<ApiResponse<Subscriber>>()
    }
    
    suspend fun updateSubscriber(id: Int, request: UpdateSubscriberRequest): Result<ApiResponse<Subscriber>> = runCatching {
        logger.info { "Updating subscriber: id=$id" }
        
        val response = httpClient.put("${config.normalizedBaseUrl}/api/subscribers/$id") {
            setBody(request)
        }
        
        response.body<ApiResponse<Subscriber>>()
    }
    
    suspend fun deleteSubscriber(id: Int): Result<ApiResponse<Unit>> = runCatching {
        logger.info { "Deleting subscriber: id=$id" }
        
        val response = httpClient.delete("${config.normalizedBaseUrl}/api/subscribers/$id")
        response.body<ApiResponse<Unit>>()
    }
    
    // List operations
    suspend fun getLists(
        page: Int = 1,
        perPage: Int = 20,
        query: String? = null,
        tag: String? = null
    ): Result<ApiResponse<PaginatedResponse<MailingList>>> = runCatching {
        logger.info { "Getting lists: page=$page, perPage=$perPage" }
        
        val response = httpClient.get("${config.normalizedBaseUrl}/api/lists") {
            parameter("page", page)
            parameter("per_page", perPage)
            query?.let { parameter("query", it) }
            tag?.let { parameter("tag", it) }
        }
        
        response.body<ApiResponse<PaginatedResponse<MailingList>>>()
    }
    
    suspend fun getList(id: Int): Result<ApiResponse<MailingList>> = runCatching {
        logger.info { "Getting list: id=$id" }
        
        val response = httpClient.get("${config.normalizedBaseUrl}/api/lists/$id")
        response.body<ApiResponse<MailingList>>()
    }
    
    suspend fun createList(request: CreateListRequest): Result<ApiResponse<MailingList>> = runCatching {
        logger.info { "Creating list: name=${request.name}" }
        
        val response = httpClient.post("${config.normalizedBaseUrl}/api/lists") {
            setBody(request)
        }
        
        response.body<ApiResponse<MailingList>>()
    }
    
    suspend fun updateList(id: Int, request: UpdateListRequest): Result<ApiResponse<MailingList>> = runCatching {
        logger.info { "Updating list: id=$id" }
        
        val response = httpClient.put("${config.normalizedBaseUrl}/api/lists/$id") {
            setBody(request)
        }
        
        response.body<ApiResponse<MailingList>>()
    }
    
    suspend fun deleteList(id: Int): Result<ApiResponse<Unit>> = runCatching {
        logger.info { "Deleting list: id=$id" }
        
        val response = httpClient.delete("${config.normalizedBaseUrl}/api/lists/$id")
        response.body<ApiResponse<Unit>>()
    }
    
    // Campaign operations
    suspend fun getCampaigns(
        page: Int = 1,
        perPage: Int = 20,
        query: String? = null,
        status: CampaignStatus? = null
    ): Result<ApiResponse<PaginatedResponse<Campaign>>> = runCatching {
        logger.info { "Getting campaigns: page=$page, perPage=$perPage" }
        
        val response = httpClient.get("${config.normalizedBaseUrl}/api/campaigns") {
            parameter("page", page)
            parameter("per_page", perPage)
            query?.let { parameter("query", it) }
            status?.let { parameter("status", it.name.lowercase()) }
        }
        
        response.body<ApiResponse<PaginatedResponse<Campaign>>>()
    }
    
    suspend fun getCampaign(id: Int): Result<ApiResponse<Campaign>> = runCatching {
        logger.info { "Getting campaign: id=$id" }
        
        val response = httpClient.get("${config.normalizedBaseUrl}/api/campaigns/$id")
        response.body<ApiResponse<Campaign>>()
    }
    
    suspend fun createCampaign(request: CreateCampaignRequest): Result<ApiResponse<Campaign>> = runCatching {
        logger.info { "Creating campaign: name=${request.name}" }
        
        val response = httpClient.post("${config.normalizedBaseUrl}/api/campaigns") {
            setBody(request)
        }
        
        response.body<ApiResponse<Campaign>>()
    }
    
    suspend fun updateCampaignStatus(id: Int, status: CampaignStatus): Result<ApiResponse<Campaign>> = runCatching {
        logger.info { "Updating campaign status: id=$id, status=$status" }
        
        val response = httpClient.put("${config.normalizedBaseUrl}/api/campaigns/$id/status") {
            setBody(UpdateCampaignStatusRequest(status))
        }
        
        response.body<ApiResponse<Campaign>>()
    }
    
    suspend fun deleteCampaign(id: Int): Result<ApiResponse<Unit>> = runCatching {
        logger.info { "Deleting campaign: id=$id" }
        
        val response = httpClient.delete("${config.normalizedBaseUrl}/api/campaigns/$id")
        response.body<ApiResponse<Unit>>()
    }
    
    // Template operations
    suspend fun getTemplates(): Result<ApiResponse<List<Template>>> = runCatching {
        logger.info { "Getting templates" }
        
        val response = httpClient.get("${config.normalizedBaseUrl}/api/templates")
        response.body<ApiResponse<List<Template>>>()
    }
    
    suspend fun getTemplate(id: Int): Result<ApiResponse<Template>> = runCatching {
        logger.info { "Getting template: id=$id" }
        
        val response = httpClient.get("${config.normalizedBaseUrl}/api/templates/$id")
        response.body<ApiResponse<Template>>()
    }
    
    suspend fun getTemplatePreview(id: Int): Result<String> = runCatching {
        logger.info { "Getting template preview: id=$id" }
        
        val response = httpClient.get("${config.normalizedBaseUrl}/api/templates/$id/preview")
        response.body<String>()
    }
    
    suspend fun createTemplate(request: CreateTemplateRequest): Result<ApiResponse<Template>> = runCatching {
        logger.info { "Creating template: name=${request.name}" }
        
        val response = httpClient.post("${config.normalizedBaseUrl}/api/templates") {
            setBody(request)
        }
        
        response.body<ApiResponse<Template>>()
    }
    
    suspend fun updateTemplate(id: Int, request: UpdateTemplateRequest): Result<ApiResponse<Template>> = runCatching {
        logger.info { "Updating template: id=$id" }
        
        val response = httpClient.put("${config.normalizedBaseUrl}/api/templates/$id") {
            setBody(request)
        }
        
        response.body<ApiResponse<Template>>()
    }
    
    suspend fun setDefaultTemplate(id: Int): Result<ApiResponse<Template>> = runCatching {
        logger.info { "Setting default template: id=$id" }
        
        val response = httpClient.put("${config.normalizedBaseUrl}/api/templates/$id/default")
        response.body<ApiResponse<Template>>()
    }
    
    suspend fun deleteTemplate(id: Int): Result<ApiResponse<Unit>> = runCatching {
        logger.info { "Deleting template: id=$id" }
        
        val response = httpClient.delete("${config.normalizedBaseUrl}/api/templates/$id")
        response.body<ApiResponse<Unit>>()
    }
    
    fun close() {
        httpClient.close()
    }
}