package io.devexpert.listmonk.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
data class ApiResponse<T>(
    val data: T
)

@Serializable
data class PaginatedResponse<T>(
    val results: List<T>,
    val query: String? = null,
    val total: Int,
    @SerialName("per_page")
    val perPage: Int,
    val page: Int
)

@Serializable
data class Subscriber(
    val id: Int? = null,
    val uuid: String? = null,
    val email: String,
    val name: String,
    val status: SubscriberStatus,
    val attribs: JsonObject? = null,
    val lists: List<SubscriberList>? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at") 
    val updatedAt: String? = null
)

@Serializable
data class SubscriberList(
    val id: Int,
    val name: String,
    @SerialName("subscription_status")
    val subscriptionStatus: SubscriptionStatus,
    val uuid: String? = null,
    val type: ListType? = null,
    val tags: List<String>? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

@Serializable
enum class SubscriberStatus {
    @SerialName("enabled")
    ENABLED,
    @SerialName("blocklisted")
    BLOCKLISTED
}

@Serializable
enum class SubscriptionStatus {
    @SerialName("confirmed")
    CONFIRMED,
    @SerialName("unconfirmed")
    UNCONFIRMED,
    @SerialName("unsubscribed")
    UNSUBSCRIBED
}

@Serializable
data class MailingList(
    val id: Int? = null,
    val uuid: String? = null,
    val name: String,
    val type: ListType? = null,
    val optin: OptinType? = null,
    val tags: List<String>? = null,
    val description: String? = null,
    @SerialName("subscriber_count")
    val subscriberCount: Int? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

@Serializable
enum class ListType {
    @SerialName("public")
    PUBLIC,
    @SerialName("private")
    PRIVATE
}

@Serializable
enum class OptinType {
    @SerialName("single")
    SINGLE,
    @SerialName("double")
    DOUBLE
}

@Serializable
data class Campaign(
    val id: Int? = null,
    val uuid: String? = null,
    val name: String,
    val subject: String,
    @SerialName("from_email")
    val fromEmail: String? = null,
    val body: String? = null,  
    val status: CampaignStatus? = null,
    val lists: List<MailingList>? = null,
    val tags: List<String>? = null,
    @SerialName("template_id")
    val templateId: Int? = null,
    val messenger: String? = "email",
    val type: CampaignType? = CampaignType.REGULAR,
    @SerialName("content_type")
    val contentType: ContentType? = ContentType.RICHTEXT,
    @SerialName("send_at")
    val sendAt: String? = null,
    val stats: CampaignStats? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

@Serializable
enum class CampaignStatus {
    @SerialName("draft")
    DRAFT,
    @SerialName("scheduled")
    SCHEDULED,
    @SerialName("running")
    RUNNING,
    @SerialName("paused")
    PAUSED,
    @SerialName("finished")
    FINISHED,
    @SerialName("cancelled")
    CANCELLED
}

@Serializable
enum class CampaignType {
    @SerialName("regular")
    REGULAR,
    @SerialName("optin")
    OPTIN
}

@Serializable  
enum class ContentType {
    @SerialName("richtext")
    RICHTEXT,
    @SerialName("html")
    HTML,
    @SerialName("markdown")
    MARKDOWN,
    @SerialName("plain")
    PLAIN
}

@Serializable
data class CampaignStats(
    val views: Int? = null,
    val clicks: Int? = null,
    val sent: Int? = null,
    val bounces: Int? = null
)

@Serializable
data class CreateSubscriberRequest(
    val email: String,
    val name: String,
    val status: SubscriberStatus,
    val lists: List<Int>? = null,
    val attribs: JsonObject? = null,
    @SerialName("preconfirm_subscriptions")
    val preconfirmSubscriptions: Boolean? = null
)

@Serializable
data class UpdateSubscriberRequest(
    val email: String? = null,
    val name: String? = null,
    val status: SubscriberStatus? = null,
    val lists: List<Int>? = null,
    val attribs: JsonObject? = null
)

@Serializable
data class CreateListRequest(
    val name: String,
    val type: ListType,
    val optin: OptinType,
    val tags: List<String>? = null,
    val description: String? = null
)

@Serializable
data class UpdateListRequest(
    val name: String? = null,
    val type: ListType? = null,
    val optin: OptinType? = null,
    val tags: List<String>? = null,
    val description: String? = null
)

@Serializable
data class CreateCampaignRequest(
    val name: String,
    val subject: String,
    val lists: List<Int>,
    val status: CampaignStatus? = null,
    val body: String? = null,
    @SerialName("from_email")
    val fromEmail: String? = null,
    @SerialName("content_type")
    val contentType: ContentType = ContentType.RICHTEXT,
    val messenger: String = "email",
    val type: CampaignType = CampaignType.REGULAR,
    val tags: List<String>? = null,
    @SerialName("template_id")
    val templateId: Int? = null,
    @SerialName("send_at")
    val sendAt: String? = null
)

@Serializable
data class UpdateCampaignRequest(
    val name: String? = null,
    val subject: String? = null,
    val lists: List<Int>? = null,
    val status: CampaignStatus? = null,
    val body: String? = null,
    @SerialName("from_email")
    val fromEmail: String? = null,
    @SerialName("content_type")
    val contentType: ContentType? = null,
    val tags: List<String>? = null,
    @SerialName("template_id")
    val templateId: Int? = null,
    @SerialName("send_at")
    val sendAt: String? = null
)

@Serializable
data class Template(
    val id: Int? = null,
    val name: String,
    val type: TemplateType,
    val subject: String? = null,
    val body: String,
    @SerialName("is_default")
    val isDefault: Boolean? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

@Serializable
enum class TemplateType {
    @SerialName("campaign")
    CAMPAIGN,
    @SerialName("campaign_visual")
    CAMPAIGN_VISUAL,
    @SerialName("tx")
    TX
}

@Serializable
data class CreateTemplateRequest(
    val name: String,
    val type: TemplateType,
    val body: String,
    val subject: String? = null
)

@Serializable

data class UpdateTemplateRequest(

    val name: String? = null,

    val type: TemplateType? = null,

    val body: String? = null,

    val subject: String? = null

)



@Serializable

data class TransactionalMessageRequest(

    @SerialName("subscriber_email")

    val subscriberEmail: String? = null,

    @SerialName("subscriber_id")

    val subscriberId: Int? = null,

    @SerialName("template_id")

    val templateId: Int? = null,

    @SerialName("template_name")

    val templateName: String? = null,

    val data: JsonObject? = null,

    val headers: List<Map<String, String>>? = null,

    val messenger: String = "email",

    @SerialName("content_type")

    val contentType: ContentType? = null

)



@Serializable

data class Media(

    val id: Int? = null,

    val uuid: String? = null,

    val filename: String,

    val thumb: String? = null,

    val type: String? = null,

    val size: Long? = null,

    @SerialName("created_at")

    val createdAt: String? = null

)



@Serializable

data class DashboardStats(

    val subscribers: Int,

    val lists: Int,

    val campaigns: Int,

    val messages: Int

)



@Serializable

data class HealthResponse(

    val status: String

)
