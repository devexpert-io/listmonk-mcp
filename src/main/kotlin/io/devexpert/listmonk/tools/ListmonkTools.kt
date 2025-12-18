package io.devexpert.listmonk.tools

import io.devexpert.listmonk.model.*
import io.devexpert.listmonk.service.ListmonkService
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.server.Server
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory

class ListmonkTools(private val listmonkService: ListmonkService) {
    private val logger = LoggerFactory.getLogger(ListmonkTools::class.java)
    
    
    fun registerTools(server: Server) {
        logger.info("Registering Listmonk tools...")
        
        // Subscriber tools
        registerGetSubscribersTool(server)
        registerGetSubscriberTool(server)
        registerCreateSubscriberTool(server)
        registerUpdateSubscriberTool(server)
        registerDeleteSubscriberTool(server)
        
        // List tools
        registerGetListsTool(server)
        registerGetListTool(server)
        registerCreateListTool(server)
        registerUpdateListTool(server)
        registerDeleteListTool(server)
        
        // Campaign tools
        registerGetCampaignsTool(server)
        registerGetCampaignTool(server)
        registerCreateCampaignTool(server)
        registerUpdateCampaignTool(server)
        registerUpdateCampaignStatusTool(server)
        registerDeleteCampaignTool(server)
        
        // Template tools
        registerGetTemplatesTool(server)
        registerGetTemplateTool(server)
        registerGetTemplatePreviewTool(server)
        registerCreateTemplateTool(server)
        registerUpdateTemplateTool(server)
        registerSetDefaultTemplateTool(server)
        registerDeleteTemplateTool(server)
        
        // Miscellaneous tools
        registerGetHealthTool(server)
        registerGetDashboardStatsTool(server)
        
        // Transactional tools
        registerSendTransactionalEmailTool(server)
        
        // Media tools
        registerGetMediaTool(server)
        
        // Analytics tools
        registerGetCampaignAnalyticsTool(server)
        
        logger.info("Listmonk tools registered successfully")
    }
    
    // Subscriber Tools
    private fun registerGetSubscribersTool(server: Server) {
        server.addTool(
            name = "get_subscribers",
            description = "Retrieve subscribers from Listmonk with optional filtering",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    put("page", buildJsonObject {
                        put("type", JsonPrimitive("integer"))
                        put("description", JsonPrimitive("Page number (default: 1)"))
                        put("minimum", JsonPrimitive(1))
                    })
                    put("per_page", buildJsonObject {
                        put("type", JsonPrimitive("integer"))
                        put("description", JsonPrimitive("Items per page (default: 20)"))
                        put("minimum", JsonPrimitive(1))
                        put("maximum", JsonPrimitive(100))
                    })
                    put("query", buildJsonObject {
                        put("type", JsonPrimitive("string"))
                        put("description", JsonPrimitive("Search query for subscribers"))
                    })
                    put("list_id", buildJsonObject {
                        put("type", JsonPrimitive("integer"))
                        put("description", JsonPrimitive("Filter by specific list ID"))
                    })
                    put("status", buildJsonObject {
                        put("type", JsonPrimitive("string"))
                        put("enum", JsonArray(listOf(
                            JsonPrimitive("enabled"),
                            JsonPrimitive("blocklisted")
                        )))
                        put("description", JsonPrimitive("Filter by subscriber status"))
                    })
                }
            )
        ) { request ->
            val page = request.arguments.getArgument("page", 1L).toInt()
            val perPage = request.arguments.getArgument("per_page", 20L).toInt()
            val query = request.arguments.getArgument("query", "")
            val listId = request.arguments.getArgument("list_id", 0L).let { if (it == 0L) null else it.toInt() }
            val statusStr = request.arguments.getArgument("status", "")
            val status = when (statusStr) {
                "enabled" -> SubscriberStatus.ENABLED
                "blocklisted" -> SubscriberStatus.BLOCKLISTED
                else -> null
            }
            
            val result = runBlocking {
                listmonkService.getSubscribers(
                    page = page,
                    perPage = perPage,
                    query = query.takeIf { it.isNotBlank() },
                    listId = listId,
                    status = status
                )
            }
            
            val responseText = if (result.isSuccess) {
                val response = result.getOrThrow()
                Json.encodeToString(response)
            } else {
                "Error getting subscribers: ${result.exceptionOrNull()?.message}"
            }
            
            CallToolResult(
                content = listOf(TextContent(text = responseText))
            )
        }
    }
    
    private fun registerGetSubscriberTool(server: Server) {
        server.addTool(
            name = "get_subscriber",
            description = "Get details of a specific subscriber by ID",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    put("id", buildJsonObject {
                        put("type", JsonPrimitive("integer"))
                        put("description", JsonPrimitive("Subscriber ID"))
                    })
                },
                required = listOf("id")
            )
        ) { request ->
            val id = request.arguments.getArgument("id", 0L).toInt()
            if (id == 0) {
                return@addTool CallToolResult(
                    content = listOf(TextContent(text = "Error: id parameter is required"))
                )
            }
            
            val result = runBlocking {
                listmonkService.getSubscriber(id)
            }
            
            val responseText = if (result.isSuccess) {
                val subscriber = result.getOrThrow()
                Json.encodeToString(ApiResponse.serializer(Subscriber.serializer()), subscriber)
            } else {
                "Error getting subscriber: ${result.exceptionOrNull()?.message}"
            }
            
            CallToolResult(
                content = listOf(TextContent(text = responseText))
            )
        }
    }
    
    private fun registerCreateSubscriberTool(server: Server) {
        server.addTool(
            name = "create_subscriber",
            description = "Create a new subscriber in Listmonk",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    put("email", buildJsonObject {
                        put("type", JsonPrimitive("string"))
                        put("format", JsonPrimitive("email"))
                        put("description", JsonPrimitive("Subscriber email address"))
                    })
                    put("name", buildJsonObject {
                        put("type", JsonPrimitive("string"))
                        put("description", JsonPrimitive("Subscriber name"))
                    })
                    put("status", buildJsonObject {
                        put("type", JsonPrimitive("string"))
                        put("enum", JsonArray(listOf(
                            JsonPrimitive("enabled"),
                            JsonPrimitive("blocklisted")
                        )))
                        put("description", JsonPrimitive("Subscriber status"))
                    })
                    put("lists", buildJsonObject {
                        put("type", JsonPrimitive("array"))
                        put("items", buildJsonObject {
                            put("type", JsonPrimitive("integer"))
                        })
                        put("description", JsonPrimitive("List of list IDs to subscribe to"))
                    })
                    put("preconfirm_subscriptions", buildJsonObject {
                        put("type", JsonPrimitive("boolean"))
                        put("description", JsonPrimitive("Whether to preconfirm subscriptions"))
                    })
                },
                required = listOf("email", "name", "status")
            )
        ) { request ->
            val email = request.arguments.getArgument("email", "")
            val name = request.arguments.getArgument("name", "")
            val statusStr = request.arguments.getArgument("status", "enabled")
            
            if (email.isBlank() || name.isBlank()) {
                return@addTool CallToolResult(
                    content = listOf(TextContent(text = "Error: email and name are required"))
                )
            }
            
            val status = when (statusStr) {
                "enabled" -> SubscriberStatus.ENABLED
                "blocklisted" -> SubscriberStatus.BLOCKLISTED
                else -> return@addTool CallToolResult(
                    content = listOf(TextContent(text = "Error: status must be 'enabled' or 'blocklisted'"))
                )
            }
            
            // Handle lists array parameter
            val listsElement = request.arguments["lists"]
            val lists = if (listsElement != null) {
                try {
                    when (listsElement) {
                        is JsonArray -> listsElement.map { it.jsonPrimitive.int }
                        is JsonPrimitive -> {
                            // Handle case where it might be passed as a JSON string
                            Json.parseToJsonElement(listsElement.content).jsonArray.map { it.jsonPrimitive.int }
                        }
                        else -> null
                    }
                } catch (_: Exception) {
                    null
                }
            } else null
            
            val preconfirm = request.arguments.getArgument("preconfirm_subscriptions", "false").toBoolean()
            
            val createRequest = CreateSubscriberRequest(
                email = email,
                name = name,
                status = status,
                lists = lists,
                preconfirmSubscriptions = preconfirm
            )
            
            val result = runBlocking {
                listmonkService.createSubscriber(createRequest)
            }
            
            val responseText = if (result.isSuccess) {
                val subscriber = result.getOrThrow()
                Json.encodeToString(ApiResponse.serializer(Subscriber.serializer()), subscriber)
            } else {
                "Error creating subscriber: ${result.exceptionOrNull()?.message}"
            }
            
            CallToolResult(
                content = listOf(TextContent(text = responseText))
            )
        }
    }
    
    private fun registerUpdateSubscriberTool(server: Server) {
        server.addTool(
            name = "update_subscriber",
            description = "Update an existing subscriber",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    put("id", buildJsonObject {
                        put("type", JsonPrimitive("integer"))
                        put("description", JsonPrimitive("Subscriber ID"))
                    })
                    put("email", buildJsonObject {
                        put("type", JsonPrimitive("string"))
                        put("format", JsonPrimitive("email"))
                        put("description", JsonPrimitive("New email address"))
                    })
                    put("name", buildJsonObject {
                        put("type", JsonPrimitive("string"))
                        put("description", JsonPrimitive("New name"))
                    })
                    put("status", buildJsonObject {
                        put("type", JsonPrimitive("string"))
                        put("enum", JsonArray(listOf(
                            JsonPrimitive("enabled"),
                            JsonPrimitive("blocklisted")
                        )))
                        put("description", JsonPrimitive("New status"))
                    })
                    put("lists", buildJsonObject {
                        put("type", JsonPrimitive("array"))
                        put("items", buildJsonObject {
                            put("type", JsonPrimitive("integer"))
                        })
                        put("description", JsonPrimitive("New list of list IDs"))
                    })
                },
                required = listOf("id")
            )
        ) { request ->
            val id = request.arguments.getArgument("id", 0L).toInt()
            if (id == 0) {
                return@addTool CallToolResult(
                    content = listOf(TextContent(text = "Error: id parameter is required"))
                )
            }
            
            val email = request.arguments.getArgument("email", "").takeIf { it.isNotBlank() }
            val name = request.arguments.getArgument("name", "").takeIf { it.isNotBlank() }
            val statusStr = request.arguments.getArgument("status", "")
            val status = when (statusStr) {
                "enabled" -> SubscriberStatus.ENABLED
                "blocklisted" -> SubscriberStatus.BLOCKLISTED
                "" -> null
                else -> return@addTool CallToolResult(
                    content = listOf(TextContent(text = "Error: status must be 'enabled' or 'blocklisted'"))
                )
            }
            
            // Handle lists array parameter
            val listsElement = request.arguments["lists"]
            val lists = if (listsElement != null) {
                try {
                    when (listsElement) {
                        is JsonArray -> listsElement.map { it.jsonPrimitive.int }
                        is JsonPrimitive -> {
                            // Handle case where it might be passed as a JSON string
                            Json.parseToJsonElement(listsElement.content).jsonArray.map { it.jsonPrimitive.int }
                        }
                        else -> null
                    }
                } catch (_: Exception) {
                    null
                }
            } else null
            
            val updateRequest = UpdateSubscriberRequest(
                email = email,
                name = name,
                status = status,
                lists = lists
            )
            
            val result = runBlocking {
                listmonkService.updateSubscriber(id, updateRequest)
            }
            
            val responseText = if (result.isSuccess) {
                val subscriber = result.getOrThrow()
                Json.encodeToString(ApiResponse.serializer(Subscriber.serializer()), subscriber)
            } else {
                "Error updating subscriber: ${result.exceptionOrNull()?.message}"
            }
            
            CallToolResult(
                content = listOf(TextContent(text = responseText))
            )
        }
    }
    
    private fun registerDeleteSubscriberTool(server: Server) {
        server.addTool(
            name = "delete_subscriber",
            description = "Delete a subscriber by ID",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    put("id", buildJsonObject {
                        put("type", JsonPrimitive("integer"))
                        put("description", JsonPrimitive("Subscriber ID to delete"))
                    })
                },
                required = listOf("id")
            )
        ) { request ->
            val id = request.arguments.getArgument("id", 0L).toInt()
            if (id == 0) {
                return@addTool CallToolResult(
                    content = listOf(TextContent(text = "Error: id parameter is required"))
                )
            }
            
            val result = runBlocking {
                listmonkService.deleteSubscriber(id)
            }
            
            val responseText = if (result.isSuccess) {
                "Subscriber $id deleted successfully"
            } else {
                "Error deleting subscriber: ${result.exceptionOrNull()?.message}"
            }
            
            CallToolResult(
                content = listOf(TextContent(text = responseText))
            )
        }
    }
    
    // List Tools
    private fun registerGetListsTool(server: Server) {
        server.addTool(
            name = "get_lists",
            description = "Retrieve mailing lists from Listmonk",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    put("page", buildJsonObject {
                        put("type", JsonPrimitive("integer"))
                        put("description", JsonPrimitive("Page number (default: 1)"))
                        put("minimum", JsonPrimitive(1))
                    })
                    put("per_page", buildJsonObject {
                        put("type", JsonPrimitive("integer"))
                        put("description", JsonPrimitive("Items per page (default: 20)"))
                        put("minimum", JsonPrimitive(1))
                        put("maximum", JsonPrimitive(100))
                    })
                    put("query", buildJsonObject {
                        put("type", JsonPrimitive("string"))
                        put("description", JsonPrimitive("Search query for lists"))
                    })
                    put("tag", buildJsonObject {
                        put("type", JsonPrimitive("string"))
                        put("description", JsonPrimitive("Filter by tag"))
                    })
                }
            )
        ) { request ->
            val page = request.arguments.getArgument("page", 1L).toInt()
            val perPage = request.arguments.getArgument("per_page", 20L).toInt()
            val query = request.arguments.getArgument("query", "").takeIf { it.isNotBlank() }
            val tag = request.arguments.getArgument("tag", "").takeIf { it.isNotBlank() }
            
            val result = runBlocking {
                listmonkService.getLists(page, perPage, query, tag)
            }
            
            val responseText = if (result.isSuccess) {
                val lists = result.getOrThrow()
                Json.encodeToString(lists)
            } else {
                "Error getting lists: ${result.exceptionOrNull()?.message}"
            }
            
            CallToolResult(
                content = listOf(TextContent(text = responseText))
            )
        }
    }
    
    private fun registerGetListTool(server: Server) {
        server.addTool(
            name = "get_list",
            description = "Get details of a specific mailing list by ID",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    put("id", buildJsonObject {
                        put("type", JsonPrimitive("integer"))
                        put("description", JsonPrimitive("List ID"))
                    })
                },
                required = listOf("id")
            )
        ) { request ->
            val id = request.arguments.getArgument("id", 0L).toInt()
            if (id == 0) {
                return@addTool CallToolResult(
                    content = listOf(TextContent(text = "Error: id parameter is required"))
                )
            }
            
            val result = runBlocking {
                listmonkService.getList(id)
            }
            
            val responseText = if (result.isSuccess) {
                val list = result.getOrThrow()
                Json.encodeToString(ApiResponse.serializer(MailingList.serializer()), list)
            } else {
                "Error getting list: ${result.exceptionOrNull()?.message}"
            }
            
            CallToolResult(
                content = listOf(TextContent(text = responseText))
            )
        }
    }
    
    private fun registerCreateListTool(server: Server) {
        server.addTool(
            name = "create_list",
            description = "Create a new mailing list in Listmonk",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    put("name", buildJsonObject {
                        put("type", JsonPrimitive("string"))
                        put("description", JsonPrimitive("List name"))
                    })
                    put("type", buildJsonObject {
                        put("type", JsonPrimitive("string"))
                        put("enum", JsonArray(listOf(
                            JsonPrimitive("public"),
                            JsonPrimitive("private")
                        )))
                        put("description", JsonPrimitive("List type"))
                    })
                    put("optin", buildJsonObject {
                        put("type", JsonPrimitive("string"))
                        put("enum", JsonArray(listOf(
                            JsonPrimitive("single"),
                            JsonPrimitive("double")
                        )))
                        put("description", JsonPrimitive("Opt-in type"))
                    })
                    put("tags", buildJsonObject {
                        put("type", JsonPrimitive("array"))
                        put("items", buildJsonObject {
                            put("type", JsonPrimitive("string"))
                        })
                        put("description", JsonPrimitive("List tags"))
                    })
                    put("description", buildJsonObject {
                        put("type", JsonPrimitive("string"))
                        put("description", JsonPrimitive("List description"))
                    })
                },
                required = listOf("name", "type", "optin")
            )
        ) { request ->
            val name = request.arguments.getArgument("name", "")
            val typeStr = request.arguments.getArgument("type", "")
            val optinStr = request.arguments.getArgument("optin", "")
            
            if (name.isBlank() || typeStr.isBlank() || optinStr.isBlank()) {
                return@addTool CallToolResult(
                    content = listOf(TextContent(text = "Error: name, type, and optin are required"))
                )
            }
            
            val type = when (typeStr) {
                "public" -> ListType.PUBLIC
                "private" -> ListType.PRIVATE
                else -> return@addTool CallToolResult(
                    content = listOf(TextContent(text = "Error: type must be 'public' or 'private'"))
                )
            }
            
            val optin = when (optinStr) {
                "single" -> OptinType.SINGLE
                "double" -> OptinType.DOUBLE
                else -> return@addTool CallToolResult(
                    content = listOf(TextContent(text = "Error: optin must be 'single' or 'double'"))
                )
            }
            
            // Handle tags array parameter
            val tagsElement = request.arguments["tags"]
            val tags = if (tagsElement != null) {
                try {
                    when (tagsElement) {
                        is JsonArray -> tagsElement.map { it.jsonPrimitive.content }
                        is JsonPrimitive -> {
                            // Handle case where it might be passed as a JSON string
                            Json.parseToJsonElement(tagsElement.content).jsonArray.map { it.jsonPrimitive.content }
                        }
                        else -> null
                    }
                } catch (_: Exception) {
                    null
                }
            } else null
            
            val description = request.arguments.getArgument("description", "").takeIf { it.isNotBlank() }
            
            val createRequest = CreateListRequest(
                name = name,
                type = type,
                optin = optin,
                tags = tags,
                description = description
            )
            
            val result = runBlocking {
                listmonkService.createList(createRequest)
            }
            
            val responseText = if (result.isSuccess) {
                val list = result.getOrThrow()
                Json.encodeToString(ApiResponse.serializer(MailingList.serializer()), list)
            } else {
                "Error creating list: ${result.exceptionOrNull()?.message}"
            }
            
            CallToolResult(
                content = listOf(TextContent(text = responseText))
            )
        }
    }
    
    private fun registerUpdateListTool(server: Server) {
        server.addTool(
            name = "update_list",
            description = "Update an existing mailing list",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    put("id", buildJsonObject {
                        put("type", JsonPrimitive("integer"))
                        put("description", JsonPrimitive("List ID"))
                    })
                    put("name", buildJsonObject {
                        put("type", JsonPrimitive("string"))
                        put("description", JsonPrimitive("New list name"))
                    })
                    put("type", buildJsonObject {
                        put("type", JsonPrimitive("string"))
                        put("enum", JsonArray(listOf(
                            JsonPrimitive("public"),
                            JsonPrimitive("private")
                        )))
                        put("description", JsonPrimitive("New list type"))
                    })
                    put("optin", buildJsonObject {
                        put("type", JsonPrimitive("string"))
                        put("enum", JsonArray(listOf(
                            JsonPrimitive("single"),
                            JsonPrimitive("double")
                        )))
                        put("description", JsonPrimitive("New opt-in type"))
                    })
                    put("tags", buildJsonObject {
                        put("type", JsonPrimitive("array"))
                        put("items", buildJsonObject {
                            put("type", JsonPrimitive("string"))
                        })
                        put("description", JsonPrimitive("New list tags"))
                    })
                    put("description", buildJsonObject {
                        put("type", JsonPrimitive("string"))
                        put("description", JsonPrimitive("New list description"))
                    })
                },
                required = listOf("id")
            )
        ) { request ->
            val id = request.arguments.getArgument("id", 0L).toInt()
            if (id == 0) {
                return@addTool CallToolResult(
                    content = listOf(TextContent(text = "Error: id parameter is required"))
                )
            }
            
            val name = request.arguments.getArgument("name", "").takeIf { it.isNotBlank() }
            val typeStr = request.arguments.getArgument("type", "")
            val type = when (typeStr) {
                "public" -> ListType.PUBLIC
                "private" -> ListType.PRIVATE
                "" -> null
                else -> return@addTool CallToolResult(
                    content = listOf(TextContent(text = "Error: type must be 'public' or 'private'"))
                )
            }
            
            val optinStr = request.arguments.getArgument("optin", "")
            val optin = when (optinStr) {
                "single" -> OptinType.SINGLE
                "double" -> OptinType.DOUBLE
                "" -> null
                else -> return@addTool CallToolResult(
                    content = listOf(TextContent(text = "Error: optin must be 'single' or 'double'"))
                )
            }
            
            // Handle tags array parameter
            val tagsElement = request.arguments["tags"]
            val tags = if (tagsElement != null) {
                try {
                    when (tagsElement) {
                        is JsonArray -> tagsElement.map { it.jsonPrimitive.content }
                        is JsonPrimitive -> {
                            // Handle case where it might be passed as a JSON string
                            Json.parseToJsonElement(tagsElement.content).jsonArray.map { it.jsonPrimitive.content }
                        }
                        else -> null
                    }
                } catch (_: Exception) {
                    null
                }
            } else null
            
            val description = request.arguments.getArgument("description", "").takeIf { it.isNotBlank() }
            
            val updateRequest = UpdateListRequest(
                name = name,
                type = type,
                optin = optin,
                tags = tags,
                description = description
            )
            
            val result = runBlocking {
                listmonkService.updateList(id, updateRequest)
            }
            
            val responseText = if (result.isSuccess) {
                val list = result.getOrThrow()
                Json.encodeToString(ApiResponse.serializer(MailingList.serializer()), list)
            } else {
                "Error updating list: ${result.exceptionOrNull()?.message}"
            }
            
            CallToolResult(
                content = listOf(TextContent(text = responseText))
            )
        }
    }
    
    private fun registerDeleteListTool(server: Server) {
        server.addTool(
            name = "delete_list",
            description = "Delete a mailing list by ID",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    put("id", buildJsonObject {
                        put("type", JsonPrimitive("integer"))
                        put("description", JsonPrimitive("List ID to delete"))
                    })
                },
                required = listOf("id")
            )
        ) { request ->
            val id = request.arguments.getArgument("id", 0L).toInt()
            if (id == 0) {
                return@addTool CallToolResult(
                    content = listOf(TextContent(text = "Error: id parameter is required"))
                )
            }
            
            val result = runBlocking {
                listmonkService.deleteList(id)
            }
            
            val responseText = if (result.isSuccess) {
                "List $id deleted successfully"
            } else {
                "Error deleting list: ${result.exceptionOrNull()?.message}"
            }
            
            CallToolResult(
                content = listOf(TextContent(text = responseText))
            )
        }
    }
    
    // Campaign Tools
    private fun registerGetCampaignsTool(server: Server) {
        server.addTool(
            name = "get_campaigns",
            description = "Retrieve campaigns from Listmonk",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    put("page", buildJsonObject {
                        put("type", JsonPrimitive("integer"))
                        put("description", JsonPrimitive("Page number (default: 1)"))
                        put("minimum", JsonPrimitive(1))
                    })
                    put("per_page", buildJsonObject {
                        put("type", JsonPrimitive("integer"))
                        put("description", JsonPrimitive("Items per page (default: 20)"))
                        put("minimum", JsonPrimitive(1))
                        put("maximum", JsonPrimitive(100))
                    })
                    put("query", buildJsonObject {
                        put("type", JsonPrimitive("string"))
                        put("description", JsonPrimitive("Search query for campaigns"))
                    })
                    put("status", buildJsonObject {
                        put("type", JsonPrimitive("string"))
                        put("enum", JsonArray(listOf(
                            JsonPrimitive("draft"),
                            JsonPrimitive("scheduled"),
                            JsonPrimitive("running"),
                            JsonPrimitive("paused"),
                            JsonPrimitive("finished"),
                            JsonPrimitive("cancelled")
                        )))
                        put("description", JsonPrimitive("Filter by campaign status"))
                    })
                }
            )
        ) { request ->
            val page = request.arguments.getArgument("page", 1L).toInt()
            val perPage = request.arguments.getArgument("per_page", 20L).toInt()
            val query = request.arguments.getArgument("query", "").takeIf { it.isNotBlank() }
            val statusStr = request.arguments.getArgument("status", "")
            val status = when (statusStr) {
                "draft" -> CampaignStatus.DRAFT
                "scheduled" -> CampaignStatus.SCHEDULED
                "running" -> CampaignStatus.RUNNING
                "paused" -> CampaignStatus.PAUSED
                "finished" -> CampaignStatus.FINISHED
                "cancelled" -> CampaignStatus.CANCELLED
                "" -> null
                else -> null
            }
            
            val result = runBlocking {
                listmonkService.getCampaigns(page, perPage, query, status)
            }
            
            val responseText = if (result.isSuccess) {
                val campaigns = result.getOrThrow()
                Json.encodeToString(campaigns)
            } else {
                "Error getting campaigns: ${result.exceptionOrNull()?.message}"
            }
            
            CallToolResult(
                content = listOf(TextContent(text = responseText))
            )
        }
    }
    
    private fun registerGetCampaignTool(server: Server) {
        server.addTool(
            name = "get_campaign",
            description = "Get details of a specific campaign by ID",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    put("id", buildJsonObject {
                        put("type", JsonPrimitive("integer"))
                        put("description", JsonPrimitive("Campaign ID"))
                    })
                },
                required = listOf("id")
            )
        ) { request ->
            val id = request.arguments.getArgument("id", 0L).toInt()
            if (id == 0) {
                return@addTool CallToolResult(
                    content = listOf(TextContent(text = "Error: id parameter is required"))
                )
            }
            
            val result = runBlocking {
                listmonkService.getCampaign(id)
            }
            
            val responseText = if (result.isSuccess) {
                val campaign = result.getOrThrow()
                Json.encodeToString(ApiResponse.serializer(Campaign.serializer()), campaign)
            } else {
                "Error getting campaign: ${result.exceptionOrNull()?.message}"
            }
            
            CallToolResult(
                content = listOf(TextContent(text = responseText))
            )
        }
    }
    
    private fun registerCreateCampaignTool(server: Server) {
        server.addTool(
            name = "create_campaign",
            description = "Create a new campaign in Listmonk",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    put("name", buildJsonObject {
                        put("type", JsonPrimitive("string"))
                        put("description", JsonPrimitive("Campaign name"))
                    })
                    put("subject", buildJsonObject {
                        put("type", JsonPrimitive("string"))
                        put("description", JsonPrimitive("Email subject"))
                    })
                    put("lists", buildJsonObject {
                        put("type", JsonPrimitive("array"))
                        put("items", buildJsonObject {
                            put("type", JsonPrimitive("integer"))
                        })
                        put("description", JsonPrimitive("List of list IDs to send to"))
                    })
                    put("status", buildJsonObject {
                        put("type", JsonPrimitive("string"))
                        put("enum", JsonArray(listOf(
                            JsonPrimitive("draft"),
                            JsonPrimitive("scheduled"),
                            JsonPrimitive("running"),
                            JsonPrimitive("paused"),
                            JsonPrimitive("finished"),
                            JsonPrimitive("cancelled")
                        )))
                        put("description", JsonPrimitive("Initial campaign status (default: draft)"))
                    })
                    put("body", buildJsonObject {
                        put("type", JsonPrimitive("string"))
                        put("description", JsonPrimitive("Campaign body/content"))
                    })
                    put("from_email", buildJsonObject {
                        put("type", JsonPrimitive("string"))
                        put("format", JsonPrimitive("email"))
                        put("description", JsonPrimitive("From email address"))
                    })
                    put("content_type", buildJsonObject {
                        put("type", JsonPrimitive("string"))
                        put("enum", JsonArray(listOf(
                            JsonPrimitive("richtext"),
                            JsonPrimitive("html"),
                            JsonPrimitive("markdown"),
                            JsonPrimitive("plain")
                        )))
                        put("description", JsonPrimitive("Content type (default: richtext)"))
                    })
                    put("tags", buildJsonObject {
                        put("type", JsonPrimitive("array"))
                        put("items", buildJsonObject {
                            put("type", JsonPrimitive("string"))
                        })
                        put("description", JsonPrimitive("Campaign tags"))
                    })
                    put("template_id", buildJsonObject {
                        put("type", JsonPrimitive("integer"))
                        put("description", JsonPrimitive("Template ID to use"))
                    })
                    put("send_at", buildJsonObject {
                        put("type", JsonPrimitive("string"))
                        put("format", JsonPrimitive("date-time"))
                        put("description", JsonPrimitive("Schedule send time in local timezone (formats: '19:00', '2024-07-24T19:00:00', '2024-07-24 19:00:00')"))
                    })
                },
                required = listOf("name", "subject", "lists")
            )
        ) { request ->
            val name = request.arguments.getArgument("name", "")
            val subject = request.arguments.getArgument("subject", "")
            
            // Handle array parameter directly from request.arguments
            val listsElement = request.arguments["lists"]
            
            if (name.isBlank() || subject.isBlank() || listsElement == null) {
                return@addTool CallToolResult(
                    content = listOf(TextContent(text = "Error: name, subject, and lists are required"))
                )
            }
            
            val lists = try {
                when (listsElement) {
                    is JsonArray -> listsElement.map { it.jsonPrimitive.int }
                    is JsonPrimitive -> {
                        // Handle case where it might be passed as a JSON string
                        Json.parseToJsonElement(listsElement.content).jsonArray.map { it.jsonPrimitive.int }
                    }
                    else -> throw IllegalArgumentException("Invalid lists format")
                }
            } catch (_: Exception) {
                return@addTool CallToolResult(
                    content = listOf(TextContent(text = "Error: lists must be a valid JSON array of integers"))
                )
            }
            
            if (lists.isEmpty()) {
                return@addTool CallToolResult(
                    content = listOf(TextContent(text = "Error: at least one list ID is required"))
                )
            }
            
            val statusStr = request.arguments.getArgument("status", "")
            val status = when (statusStr) {
                "draft" -> CampaignStatus.DRAFT
                "scheduled" -> CampaignStatus.SCHEDULED
                "running" -> CampaignStatus.RUNNING
                "paused" -> CampaignStatus.PAUSED
                "finished" -> CampaignStatus.FINISHED
                "cancelled" -> CampaignStatus.CANCELLED
                else -> null
            }
            
            val body = request.arguments.getArgument("body", "").takeIf { it.isNotBlank() }
            val fromEmail = request.arguments.getArgument("from_email", "").takeIf { it.isNotBlank() }
            val contentTypeStr = request.arguments.getArgument("content_type", "richtext")
            val contentType = when (contentTypeStr) {
                "richtext" -> ContentType.RICHTEXT
                "html" -> ContentType.HTML
                "markdown" -> ContentType.MARKDOWN
                "plain" -> ContentType.PLAIN
                else -> ContentType.RICHTEXT
            }
            
            // Handle tags array parameter
            val tagsElement = request.arguments["tags"]
            val tags = if (tagsElement != null) {
                try {
                    when (tagsElement) {
                        is JsonArray -> tagsElement.map { it.jsonPrimitive.content }
                        is JsonPrimitive -> {
                            // Handle case where it might be passed as a JSON string
                            Json.parseToJsonElement(tagsElement.content).jsonArray.map { it.jsonPrimitive.content }
                        }
                        else -> null
                    }
                } catch (_: Exception) {
                    null
                }
            } else null
            
            val templateId = request.arguments.getArgument("template_id", 0L).let { if (it == 0L) null else it.toInt() }
            val sendAt = request.arguments.getArgument("send_at", "").takeIf { it.isNotBlank() }
                ?.let { convertLocalTimeToUTC(it) }
            
            val createRequest = CreateCampaignRequest(
                name = name,
                subject = subject,
                lists = lists,
                status = status,
                body = body,
                fromEmail = fromEmail,
                contentType = contentType,
                tags = tags,
                templateId = templateId,
                sendAt = sendAt
            )
            
            val result = runBlocking {
                val createResult = listmonkService.createCampaign(createRequest)
                if (createResult.isSuccess && status != null && status != CampaignStatus.DRAFT) {
                    val newCampaign = createResult.getOrThrow()
                    val campaignId = newCampaign.data.id
                    if (campaignId != null) {
                        // Attempt to update status if provided and not draft
                        listmonkService.updateCampaignStatus(campaignId, status)
                    } else {
                        createResult
                    }
                } else {
                    createResult
                }
            }
            
            val responseText = if (result.isSuccess) {
                val campaign = result.getOrThrow()
                Json.encodeToString(ApiResponse.serializer(Campaign.serializer()), campaign)
            } else {
                "Error creating campaign: ${result.exceptionOrNull()?.message}"
            }
            
            CallToolResult(
                content = listOf(TextContent(text = responseText))
            )
        }
    }
    
    private fun registerUpdateCampaignTool(server: Server) {
        server.addTool(
            name = "update_campaign",
            description = "Update an existing campaign's details",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    put("id", buildJsonObject {
                        put("type", JsonPrimitive("integer"))
                        put("description", JsonPrimitive("Campaign ID"))
                    })
                    put("name", buildJsonObject {
                        put("type", JsonPrimitive("string"))
                        put("description", JsonPrimitive("New campaign name"))
                    })
                    put("subject", buildJsonObject {
                        put("type", JsonPrimitive("string"))
                        put("description", JsonPrimitive("New email subject"))
                    })
                    put("lists", buildJsonObject {
                        put("type", JsonPrimitive("array"))
                        put("items", buildJsonObject {
                            put("type", JsonPrimitive("integer"))
                        })
                        put("description", JsonPrimitive("New list of list IDs"))
                    })
                    put("status", buildJsonObject {
                        put("type", JsonPrimitive("string"))
                        put("enum", JsonArray(listOf(
                            JsonPrimitive("draft"),
                            JsonPrimitive("scheduled"),
                            JsonPrimitive("running"),
                            JsonPrimitive("paused"),
                            JsonPrimitive("finished"),
                            JsonPrimitive("cancelled")
                        )))
                        put("description", JsonPrimitive("New campaign status"))
                    })
                    put("body", buildJsonObject {
                        put("type", JsonPrimitive("string"))
                        put("description", JsonPrimitive("New campaign body content"))
                    })
                    put("from_email", buildJsonObject {
                        put("type", JsonPrimitive("string"))
                        put("format", JsonPrimitive("email"))
                        put("description", JsonPrimitive("New from email address"))
                    })
                    put("content_type", buildJsonObject {
                        put("type", JsonPrimitive("string"))
                        put("enum", JsonArray(listOf(
                            JsonPrimitive("richtext"),
                            JsonPrimitive("html"),
                            JsonPrimitive("markdown"),
                            JsonPrimitive("plain")
                        )))
                        put("description", JsonPrimitive("New content type"))
                    })
                    put("tags", buildJsonObject {
                        put("type", JsonPrimitive("array"))
                        put("items", buildJsonObject {
                            put("type", JsonPrimitive("string"))
                        })
                        put("description", JsonPrimitive("New campaign tags"))
                    })
                    put("template_id", buildJsonObject {
                        put("type", JsonPrimitive("integer"))
                        put("description", JsonPrimitive("New template ID"))
                    })
                    put("send_at", buildJsonObject {
                        put("type", JsonPrimitive("string"))
                        put("format", JsonPrimitive("date-time"))
                        put("description", JsonPrimitive("New schedule time (UTC ISO format or local time like '19:00')"))
                    })
                },
                required = listOf("id")
            )
        ) { request ->
            val id = request.arguments.getArgument("id", 0L).toInt()
            if (id == 0) {
                return@addTool CallToolResult(
                    content = listOf(TextContent(text = "Error: id parameter is required"))
                )
            }
            
            val name = request.arguments.getArgument("name", "").takeIf { it.isNotBlank() }
            val subject = request.arguments.getArgument("subject", "").takeIf { it.isNotBlank() }
            
            val listsElement = request.arguments["lists"]
            val lists = try {
                when (listsElement) {
                    is JsonArray -> listsElement.map { it.jsonPrimitive.int }
                    is JsonPrimitive -> Json.parseToJsonElement(listsElement.content).jsonArray.map { it.jsonPrimitive.int }
                    else -> null
                }
            } catch (_: Exception) { null }
            
            val statusStr = request.arguments.getArgument("status", "")
            val status = when (statusStr) {
                "draft" -> CampaignStatus.DRAFT
                "scheduled" -> CampaignStatus.SCHEDULED
                "running" -> CampaignStatus.RUNNING
                "paused" -> CampaignStatus.PAUSED
                "finished" -> CampaignStatus.FINISHED
                "cancelled" -> CampaignStatus.CANCELLED
                else -> null
            }
            
            val body = request.arguments.getArgument("body", "").takeIf { it.isNotBlank() }
            val fromEmail = request.arguments.getArgument("from_email", "").takeIf { it.isNotBlank() }
            val contentTypeStr = request.arguments.getArgument("content_type", "")
            val contentType = when (contentTypeStr) {
                "richtext" -> ContentType.RICHTEXT
                "html" -> ContentType.HTML
                "markdown" -> ContentType.MARKDOWN
                "plain" -> ContentType.PLAIN
                else -> null
            }
            
            val tagsElement = request.arguments["tags"]
            val tags = try {
                when (tagsElement) {
                    is JsonArray -> tagsElement.map { it.jsonPrimitive.content }
                    is JsonPrimitive -> Json.parseToJsonElement(tagsElement.content).jsonArray.map { it.jsonPrimitive.content }
                    else -> null
                }
            } catch (_: Exception) { null }
            
            val templateId = request.arguments.getArgument("template_id", 0L).let { if (it == 0L) null else it.toInt() }
            val sendAt = request.arguments.getArgument("send_at", "").takeIf { it.isNotBlank() }
                ?.let { convertLocalTimeToUTC(it) }
            
            val updateRequest = UpdateCampaignRequest(
                name = name,
                subject = subject,
                lists = lists,
                status = status,
                body = body,
                fromEmail = fromEmail,
                contentType = contentType,
                tags = tags,
                templateId = templateId,
                sendAt = sendAt
            )
            
            val result = runBlocking {
                val updateResult = listmonkService.updateCampaign(id, updateRequest)
                if (updateResult.isSuccess && status != null) {
                    // If status was provided, also call updateCampaignStatus as Listmonk 
                    // requires a specific endpoint for state transitions
                    listmonkService.updateCampaignStatus(id, status)
                } else {
                    updateResult
                }
            }
            
            val responseText = if (result.isSuccess) {
                val campaign = result.getOrThrow()
                Json.encodeToString(ApiResponse.serializer(Campaign.serializer()), campaign)
            } else {
                "Error updating campaign: ${result.exceptionOrNull()?.message}"
            }
            
            CallToolResult(
                content = listOf(TextContent(text = responseText))
            )
        }
    }

    private fun registerUpdateCampaignStatusTool(server: Server) {
        server.addTool(
            name = "update_campaign_status",
            description = "Update the status of an existing campaign (draft, scheduled, running, paused, finished, cancelled)",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    put("id", buildJsonObject {
                        put("type", JsonPrimitive("integer"))
                        put("description", JsonPrimitive("Campaign ID"))
                    })
                    put("status", buildJsonObject {
                        put("type", JsonPrimitive("string"))
                        put("enum", JsonArray(listOf(
                            JsonPrimitive("draft"),
                            JsonPrimitive("scheduled"),
                            JsonPrimitive("running"),
                            JsonPrimitive("paused"),
                            JsonPrimitive("finished"),
                            JsonPrimitive("cancelled")
                        )))
                        put("description", JsonPrimitive("New campaign status"))
                    })
                },
                required = listOf("id", "status")
            )
        ) { request ->
            val id = request.arguments.getArgument("id", 0L).toInt()
            val statusStr = request.arguments.getArgument("status", "")
            
            if (id == 0 || statusStr.isBlank()) {
                return@addTool CallToolResult(
                    content = listOf(TextContent(text = "Error: id and status parameters are required"))
                )
            }
            
            val status = when (statusStr) {
                "draft" -> CampaignStatus.DRAFT
                "scheduled" -> CampaignStatus.SCHEDULED
                "running" -> CampaignStatus.RUNNING
                "paused" -> CampaignStatus.PAUSED
                "finished" -> CampaignStatus.FINISHED
                "cancelled" -> CampaignStatus.CANCELLED
                else -> return@addTool CallToolResult(
                    content = listOf(TextContent(text = "Error: invalid status"))
                )
            }
            
            val result = runBlocking {
                listmonkService.updateCampaignStatus(id, status)
            }
            
            val responseText = if (result.isSuccess) {
                val campaign = result.getOrThrow()
                Json.encodeToString(ApiResponse.serializer(Campaign.serializer()), campaign)
            } else {
                "Error updating campaign status: ${result.exceptionOrNull()?.message}"
            }
            
            CallToolResult(
                content = listOf(TextContent(text = responseText))
            )
        }
    }
    
    private fun registerDeleteCampaignTool(server: Server) {
        server.addTool(
            name = "delete_campaign",
            description = "Delete a campaign by ID",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    put("id", buildJsonObject {
                        put("type", JsonPrimitive("integer"))
                        put("description", JsonPrimitive("Campaign ID to delete"))
                    })
                },
                required = listOf("id")
            )
        ) { request ->
            val id = request.arguments.getArgument("id", 0L).toInt()
            if (id == 0) {
                return@addTool CallToolResult(
                    content = listOf(TextContent(text = "Error: id parameter is required"))
                )
            }
            
            val result = runBlocking {
                listmonkService.deleteCampaign(id)
            }
            
            val responseText = if (result.isSuccess) {
                "Campaign $id deleted successfully"
            } else {
                "Error deleting campaign: ${result.exceptionOrNull()?.message}"
            }
            
            CallToolResult(
                content = listOf(TextContent(text = responseText))
            )
        }
    }
    
    // Template Tools
    private fun registerGetTemplatesTool(server: Server) {
        server.addTool(
            name = "get_templates",
            description = "Retrieve all templates from Listmonk",
            inputSchema = Tool.Input(
                properties = buildJsonObject {}
            )
        ) { request ->
            val result = runBlocking {
                listmonkService.getTemplates()
            }
            
            val responseText = if (result.isSuccess) {
                Json.encodeToString(result.getOrNull())
            } else {
                "Error retrieving templates: ${result.exceptionOrNull()?.message}"
            }
            
            CallToolResult(
                content = listOf(TextContent(text = responseText))
            )
        }
    }
    
    private fun registerGetTemplateTool(server: Server) {
        server.addTool(
            name = "get_template",
            description = "Get details of a specific template by ID",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    put("id", buildJsonObject {
                        put("type", JsonPrimitive("integer"))
                        put("description", JsonPrimitive("Template ID"))
                    })
                },
                required = listOf("id")
            )
        ) { request ->
            val id = request.arguments.getArgument("id", 0L).toInt()
            if (id == 0) {
                return@addTool CallToolResult(
                    content = listOf(TextContent(text = "Error: id parameter is required"))
                )
            }
            
            val result = runBlocking {
                listmonkService.getTemplate(id)
            }
            
            val responseText = if (result.isSuccess) {
                Json.encodeToString(result.getOrNull())
            } else {
                "Error retrieving template: ${result.exceptionOrNull()?.message}"
            }
            
            CallToolResult(
                content = listOf(TextContent(text = responseText))
            )
        }
    }
    
    private fun registerGetTemplatePreviewTool(server: Server) {
        server.addTool(
            name = "get_template_preview",
            description = "Get HTML preview of a specific template by ID",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    put("id", buildJsonObject {
                        put("type", JsonPrimitive("integer"))
                        put("description", JsonPrimitive("Template ID"))
                    })
                },
                required = listOf("id")
            )
        ) { request ->
            val id = request.arguments.getArgument("id", 0L).toInt()
            if (id == 0) {
                return@addTool CallToolResult(
                    content = listOf(TextContent(text = "Error: id parameter is required"))
                )
            }
            
            val result = runBlocking {
                listmonkService.getTemplatePreview(id)
            }
            
            val responseText = if (result.isSuccess) {
                result.getOrNull() ?: "No preview available"
            } else {
                "Error retrieving template preview: ${result.exceptionOrNull()?.message}"
            }
            
            CallToolResult(
                content = listOf(TextContent(text = responseText))
            )
        }
    }
    
    private fun registerCreateTemplateTool(server: Server) {
        server.addTool(
            name = "create_template",
            description = "Create a new template in Listmonk",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    put("name", buildJsonObject {
                        put("type", JsonPrimitive("string"))
                        put("description", JsonPrimitive("Template name"))
                    })
                    put("type", buildJsonObject {
                        put("type", JsonPrimitive("string"))
                        put("enum", JsonArray(listOf(
                            JsonPrimitive("campaign"),
                            JsonPrimitive("tx")
                        )))
                        put("description", JsonPrimitive("Template type: 'campaign' or 'tx'"))
                    })
                    put("body", buildJsonObject {
                        put("type", JsonPrimitive("string"))
                        put("description", JsonPrimitive("Template HTML body"))
                    })
                    put("subject", buildJsonObject {
                        put("type", JsonPrimitive("string"))
                        put("description", JsonPrimitive("Template subject (for transactional templates)"))
                    })
                },
                required = listOf("name", "type", "body")
            )
        ) { request ->
            val name = request.arguments.getArgument("name", "")
            val typeStr = request.arguments.getArgument("type", "")
            val body = request.arguments.getArgument("body", "")
            val subject = request.arguments.getArgument("subject", "")
            
            if (name.isBlank() || typeStr.isBlank() || body.isBlank()) {
                return@addTool CallToolResult(
                    content = listOf(TextContent(text = "Error: name, type, and body parameters are required"))
                )
            }
            
            val templateType = when (typeStr) {
                "campaign" -> TemplateType.CAMPAIGN
                "tx" -> TemplateType.TX
                else -> {
                    return@addTool CallToolResult(
                        content = listOf(TextContent(text = "Error: type must be 'campaign' or 'tx'"))
                    )
                }
            }
            
            val createRequest = CreateTemplateRequest(
                name = name,
                type = templateType,
                body = body,
                subject = subject.takeIf { it.isNotBlank() }
            )
            
            val result = runBlocking {
                listmonkService.createTemplate(createRequest)
            }
            
            val responseText = if (result.isSuccess) {
                Json.encodeToString(result.getOrNull())
            } else {
                "Error creating template: ${result.exceptionOrNull()?.message}"
            }
            
            CallToolResult(
                content = listOf(TextContent(text = responseText))
            )
        }
    }
    
    private fun registerUpdateTemplateTool(server: Server) {
        server.addTool(
            name = "update_template",
            description = "Update an existing template",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    put("id", buildJsonObject {
                        put("type", JsonPrimitive("integer"))
                        put("description", JsonPrimitive("Template ID"))
                    })
                    put("name", buildJsonObject {
                        put("type", JsonPrimitive("string"))
                        put("description", JsonPrimitive("New template name"))
                    })
                    put("type", buildJsonObject {
                        put("type", JsonPrimitive("string"))
                        put("enum", JsonArray(listOf(
                            JsonPrimitive("campaign"),
                            JsonPrimitive("tx")
                        )))
                        put("description", JsonPrimitive("New template type"))
                    })
                    put("body", buildJsonObject {
                        put("type", JsonPrimitive("string"))
                        put("description", JsonPrimitive("New template HTML body"))
                    })
                    put("subject", buildJsonObject {
                        put("type", JsonPrimitive("string"))
                        put("description", JsonPrimitive("New template subject"))
                    })
                },
                required = listOf("id")
            )
        ) { request ->
            val id = request.arguments.getArgument("id", 0L).toInt()
            val name = request.arguments.getArgument("name", "")
            val typeStr = request.arguments.getArgument("type", "")
            val body = request.arguments.getArgument("body", "")
            val subject = request.arguments.getArgument("subject", "")
            
            if (id == 0) {
                return@addTool CallToolResult(
                    content = listOf(TextContent(text = "Error: id parameter is required"))
                )
            }
            
            val templateType = if (typeStr.isNotBlank()) {
                when (typeStr) {
                    "campaign" -> TemplateType.CAMPAIGN
                    "tx" -> TemplateType.TX
                    else -> {
                        return@addTool CallToolResult(
                            content = listOf(TextContent(text = "Error: type must be 'campaign' or 'tx'"))
                        )
                    }
                }
            } else null
            
            val updateRequest = UpdateTemplateRequest(
                name = name.takeIf { it.isNotBlank() },
                type = templateType,
                body = body.takeIf { it.isNotBlank() },
                subject = subject.takeIf { it.isNotBlank() }
            )
            
            val result = runBlocking {
                listmonkService.updateTemplate(id, updateRequest)
            }
            
            val responseText = if (result.isSuccess) {
                Json.encodeToString(result.getOrNull())
            } else {
                "Error updating template: ${result.exceptionOrNull()?.message}"
            }
            
            CallToolResult(
                content = listOf(TextContent(text = responseText))
            )
        }
    }
    
    private fun registerSetDefaultTemplateTool(server: Server) {
        server.addTool(
            name = "set_default_template",
            description = "Set a specific template as the default template",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    put("id", buildJsonObject {
                        put("type", JsonPrimitive("integer"))
                        put("description", JsonPrimitive("Template ID to set as default"))
                    })
                },
                required = listOf("id")
            )
        ) { request ->
            val id = request.arguments.getArgument("id", 0L).toInt()
            if (id == 0) {
                return@addTool CallToolResult(
                    content = listOf(TextContent(text = "Error: id parameter is required"))
                )
            }
            
            val result = runBlocking {
                listmonkService.setDefaultTemplate(id)
            }
            
            val responseText = if (result.isSuccess) {
                "Template $id set as default successfully"
            } else {
                "Error setting default template: ${result.exceptionOrNull()?.message}"
            }
            
            CallToolResult(
                content = listOf(TextContent(text = responseText))
            )
        }
    }
    
    private fun registerDeleteTemplateTool(server: Server) {
        server.addTool(
            name = "delete_template",
            description = "Delete a template by ID",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    put("id", buildJsonObject {
                        put("type", JsonPrimitive("integer"))
                        put("description", JsonPrimitive("Template ID to delete"))
                    })
                },
                required = listOf("id")
            )
        ) { request ->
            val id = request.arguments.getArgument("id", 0L).toInt()
            if (id == 0) {
                return@addTool CallToolResult(
                    content = listOf(TextContent(text = "Error: id parameter is required"))
                )
            }
            
            val result = runBlocking {
                listmonkService.deleteTemplate(id)
            }
            
            val responseText = if (result.isSuccess) {
                "Template $id deleted successfully"
            } else {
                "Error deleting template: ${result.exceptionOrNull()?.message}"
            }
            
            CallToolResult(
                content = listOf(TextContent(text = responseText))
            )
        }
    }

    // Miscellaneous Tools
    private fun registerGetHealthTool(server: Server) {
        server.addTool(
            name = "get_health",
            description = "Check the health of the Listmonk server",
            inputSchema = Tool.Input(properties = buildJsonObject {})
        ) { _ ->
            val result = runBlocking { listmonkService.getHealth() }
            val responseText = if (result.isSuccess) {
                Json.encodeToString(result.getOrThrow())
            } else {
                "Error getting health: ${result.exceptionOrNull()?.message}"
            }
            CallToolResult(content = listOf(TextContent(text = responseText)))
        }
    }

    private fun registerGetDashboardStatsTool(server: Server) {
        server.addTool(
            name = "get_dashboard_stats",
            description = "Get statistics counts for the dashboard",
            inputSchema = Tool.Input(properties = buildJsonObject {})
        ) { _ ->
            val result = runBlocking { listmonkService.getDashboardStats() }
            val responseText = if (result.isSuccess) {
                Json.encodeToString(result.getOrThrow())
            } else {
                "Error getting dashboard stats: ${result.exceptionOrNull()?.message}"
            }
            CallToolResult(content = listOf(TextContent(text = responseText)))
        }
    }

    // Transactional Tools
    private fun registerSendTransactionalEmailTool(server: Server) {
        server.addTool(
            name = "send_transactional_email",
            description = "Send a transactional message to a subscriber",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    put("subscriber_email", buildJsonObject {
                        put("type", JsonPrimitive("string"))
                        put("description", JsonPrimitive("Subscriber email address"))
                    })
                    put("subscriber_id", buildJsonObject {
                        put("type", JsonPrimitive("integer"))
                        put("description", JsonPrimitive("Subscriber ID"))
                    })
                    put("template_id", buildJsonObject {
                        put("type", JsonPrimitive("integer"))
                        put("description", JsonPrimitive("Template ID to use"))
                    })
                    put("template_name", buildJsonObject {
                        put("type", JsonPrimitive("string"))
                        put("description", JsonPrimitive("Template name to use"))
                    })
                    put("data", buildJsonObject {
                        put("type", JsonPrimitive("object"))
                        put("description", JsonPrimitive("Dynamic data to inject into the template"))
                    })
                }
            )
        ) { request ->
            val email = request.arguments.getArgument("subscriber_email", "").takeIf { it.isNotBlank() }
            val id = request.arguments.getArgument("subscriber_id", 0L).let { if (it == 0L) null else it.toInt() }
            val templateId = request.arguments.getArgument("template_id", 0L).let { if (it == 0L) null else it.toInt() }
            val templateName = request.arguments.getArgument("template_name", "").takeIf { it.isNotBlank() }
            
            val data = request.arguments["data"] as? JsonObject
            
            if (email == null && id == null) {
                return@addTool CallToolResult(
                    content = listOf(TextContent(text = "Error: either subscriber_email or subscriber_id is required"))
                )
            }
            
            if (templateId == null && templateName == null) {
                return@addTool CallToolResult(
                    content = listOf(TextContent(text = "Error: either template_id or template_name is required"))
                )
            }
            
            val txRequest = TransactionalMessageRequest(
                subscriberEmail = email,
                subscriberId = id,
                templateId = templateId,
                templateName = templateName,
                data = data
            )
            
            val result = runBlocking {
                listmonkService.sendTransactionalEmail(txRequest)
            }
            
            val responseText = if (result.isSuccess) {
                "Transactional email sent successfully"
            } else {
                "Error sending transactional email: ${result.exceptionOrNull()?.message}"
            }
            
            CallToolResult(
                content = listOf(TextContent(text = responseText))
            )
        }
    }

    // Media Tools
    private fun registerGetMediaTool(server: Server) {
        server.addTool(
            name = "get_media",
            description = "Retrieve uploaded media files",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    put("page", buildJsonObject {
                        put("type", JsonPrimitive("integer"))
                        put("description", JsonPrimitive("Page number (default: 1)"))
                    })
                    put("per_page", buildJsonObject {
                        put("type", JsonPrimitive("integer"))
                        put("description", JsonPrimitive("Items per page (default: 20)"))
                    })
                }
            )
        ) { request ->
            val page = request.arguments.getArgument("page", 1L).toInt()
            val perPage = request.arguments.getArgument("per_page", 20L).toInt()
            
            val result = runBlocking {
                listmonkService.getMedia(page, perPage)
            }
            
            val responseText = if (result.isSuccess) {
                Json.encodeToString(result.getOrThrow())
            } else {
                "Error getting media: ${result.exceptionOrNull()?.message}"
            }
            
            CallToolResult(
                content = listOf(TextContent(text = responseText))
            )
        }
    }

    // Analytics Tools
    private fun registerGetCampaignAnalyticsTool(server: Server) {
        server.addTool(
            name = "get_campaign_analytics",
            description = "Retrieve analytics for a specific campaign",
            inputSchema = Tool.Input(
                properties = buildJsonObject {
                    put("id", buildJsonObject {
                        put("type", JsonPrimitive("integer"))
                        put("description", JsonPrimitive("Campaign ID"))
                    })
                    put("type", buildJsonObject {
                        put("type", JsonPrimitive("string"))
                        put("enum", JsonArray(listOf(
                            JsonPrimitive("links"),
                            JsonPrimitive("views"),
                            JsonPrimitive("clicks"),
                            JsonPrimitive("bounces")
                        )))
                        put("description", JsonPrimitive("Type of analytics to retrieve"))
                    })
                },
                required = listOf("id", "type")
            )
        ) { request ->
            val id = request.arguments.getArgument("id", 0L).toInt()
            val type = request.arguments.getArgument("type", "")
            
            if (id == 0 || type.isBlank()) {
                return@addTool CallToolResult(
                    content = listOf(TextContent(text = "Error: id and type are required"))
                )
            }
            
            val result = runBlocking {
                listmonkService.getCampaignAnalytics(id, type)
            }
            
            val responseText = if (result.isSuccess) {
                Json.encodeToString(result.getOrThrow())
            } else {
                "Error getting campaign analytics: ${result.exceptionOrNull()?.message}"
            }
            
            CallToolResult(
                content = listOf(TextContent(text = responseText))
            )
        }
    }
}

// Extension function to get arguments safely
private inline fun <reified T> Map<String, Any>.getArgument(key: String, defaultValue: T): T {
    return (this[key] as? JsonPrimitive)?.content?.let {
        when (T::class) {
            String::class -> it as T
            Long::class -> it.toLongOrNull() as? T ?: defaultValue
            Double::class -> it.toDoubleOrNull() as? T ?: defaultValue
            Boolean::class -> it.toBooleanStrictOrNull() as? T ?: defaultValue
            else -> defaultValue
        }
    } ?: defaultValue
}

/**
 * Converts a local time string to UTC ISO format for Listmonk API.
 * Supports formats: "2024-07-24T19:00:00", "2024-07-24 19:00:00", "19:00"
 */
private fun convertLocalTimeToUTC(localTimeStr: String): String {
    return try {
        val now = java.time.LocalDateTime.now()
        val localDateTime = when {
            // Format: "19:00" or "19:00:00" - assume today
            localTimeStr.matches(Regex("\\d{1,2}:\\d{2}(:\\d{2})?")) -> {
                val timeParts = localTimeStr.split(":")
                val hour = timeParts[0].toInt()
                val minute = timeParts[1].toInt()
                val second = if (timeParts.size > 2) timeParts[2].toInt() else 0
                now.toLocalDate().atTime(hour, minute, second)
            }
            // Format: "2024-07-24 19:00:00" or "2024-07-24T19:00:00"
            else -> {
                val normalizedStr = localTimeStr.replace(" ", "T")
                if (normalizedStr.contains("T")) {
                    java.time.LocalDateTime.parse(normalizedStr)
                } else {
                    // Assume it's just a date, add current time
                    java.time.LocalDate.parse(normalizedStr).atTime(now.toLocalTime())
                }
            }
        }
        
        // Convert to UTC
        val systemZone = java.time.ZoneId.systemDefault()
        val utcDateTime = localDateTime.atZone(systemZone).withZoneSameInstant(java.time.ZoneOffset.UTC)
        
        // Return in ISO format
        utcDateTime.format(java.time.format.DateTimeFormatter.ISO_INSTANT)
    } catch (_: Exception) {
        // If parsing fails, return the original string
        localTimeStr
    }
}