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
        registerUpdateCampaignStatusTool(server)
        registerDeleteCampaignTool(server)
        
        logger.info("Listmonk tools registered successfully: 15 tools available")
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
            
            val listsStr = request.arguments.getArgument("lists", "")
            val lists = if (listsStr.isNotBlank()) {
                try {
                    Json.parseToJsonElement(listsStr).jsonArray.map { it.jsonPrimitive.int }
                } catch (e: Exception) {
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
            
            val listsStr = request.arguments.getArgument("lists", "")
            val lists = if (listsStr.isNotBlank()) {
                try {
                    Json.parseToJsonElement(listsStr).jsonArray.map { it.jsonPrimitive.int }
                } catch (e: Exception) {
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
                Json.encodeToString(ApiResponse.serializer(kotlinx.serialization.serializer()), lists)
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
            
            val tagsStr = request.arguments.getArgument("tags", "")
            val tags = if (tagsStr.isNotBlank()) {
                try {
                    Json.parseToJsonElement(tagsStr).jsonArray.map { it.jsonPrimitive.content }
                } catch (e: Exception) {
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
            
            val tagsStr = request.arguments.getArgument("tags", "")
            val tags = if (tagsStr.isNotBlank()) {
                try {
                    Json.parseToJsonElement(tagsStr).jsonArray.map { it.jsonPrimitive.content }
                } catch (e: Exception) {
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
                Json.encodeToString(ApiResponse.serializer(kotlinx.serialization.serializer()), campaigns)
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
                        put("description", JsonPrimitive("Schedule send time (ISO format)"))
                    })
                },
                required = listOf("name", "subject", "lists")
            )
        ) { request ->
            val name = request.arguments.getArgument("name", "")
            val subject = request.arguments.getArgument("subject", "")
            val listsStr = request.arguments.getArgument("lists", "")
            
            if (name.isBlank() || subject.isBlank() || listsStr.isBlank()) {
                return@addTool CallToolResult(
                    content = listOf(TextContent(text = "Error: name, subject, and lists are required"))
                )
            }
            
            val lists = try {
                Json.parseToJsonElement(listsStr).jsonArray.map { it.jsonPrimitive.int }
            } catch (e: Exception) {
                return@addTool CallToolResult(
                    content = listOf(TextContent(text = "Error: lists must be a valid JSON array of integers"))
                )
            }
            
            if (lists.isEmpty()) {
                return@addTool CallToolResult(
                    content = listOf(TextContent(text = "Error: at least one list ID is required"))
                )
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
            
            val tagsStr = request.arguments.getArgument("tags", "")
            val tags = if (tagsStr.isNotBlank()) {
                try {
                    Json.parseToJsonElement(tagsStr).jsonArray.map { it.jsonPrimitive.content }
                } catch (e: Exception) {
                    null
                }
            } else null
            
            val templateId = request.arguments.getArgument("template_id", 0L).let { if (it == 0L) null else it.toInt() }
            val sendAt = request.arguments.getArgument("send_at", "").takeIf { it.isNotBlank() }
            
            val createRequest = CreateCampaignRequest(
                name = name,
                subject = subject,
                lists = lists,
                body = body,
                fromEmail = fromEmail,
                contentType = contentType,
                tags = tags,
                templateId = templateId,
                sendAt = sendAt
            )
            
            
            val result = runBlocking {
                listmonkService.createCampaign(createRequest)
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
    
    private fun registerUpdateCampaignStatusTool(server: Server) {
        server.addTool(
            name = "update_campaign_status",
            description = "Update the status of an existing campaign",
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