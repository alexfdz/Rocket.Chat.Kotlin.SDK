package chat.rocket.core.internal.rest

import chat.rocket.common.model.ServerInfo
import chat.rocket.common.model.SettingsOauth
import chat.rocket.core.RocketChatClient
import chat.rocket.core.internal.model.ConfigurationsPayload
import chat.rocket.core.model.Value
import com.squareup.moshi.Types
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.withContext
import okhttp3.Request

suspend fun RocketChatClient.serverInfo(): ServerInfo = withContext(CommonPool) {
    val url = restUrl.newBuilder()
        .addPathSegment("api")
        .addPathSegment("info")
        .build()

    val request = Request.Builder().url(url).get().build()

    handleRestCall<ServerInfo>(request, ServerInfo::class.java, allowRedirects = false)
}

suspend fun RocketChatClient.configurations(): Map<String, Map<String, String>> = withContext(CommonPool) {
    val url = restUrl.newBuilder().apply {
        addPathSegment("api")
        addPathSegment("v1")
        addPathSegment("service.configurations")
    }.build()

    val request = Request.Builder().url(url).get().build()

    val payload = handleRestCall<ConfigurationsPayload>(
        request,
        ConfigurationsPayload::class.java
    )

    val result = HashMap<String, Map<String, String>>()
    payload.configurations.map { map ->
        map["service"]?.let {
            val values = map.filterNot { entry -> entry.key == "service" }
            Pair(it, values)
        }
    }.forEach { pair: Pair<String, Map<String, String>>? ->
        pair?.let {
            result.put(pair.first, pair.second)
        }
    }

    return@withContext result
}

/**
 * A simple method, requires no authentication, that returns list of all available oauth services.
 *
 * @since 0.63.0
 */
suspend fun RocketChatClient.settingsOauth(): SettingsOauth = withContext(CommonPool) {
    val url = restUrl.newBuilder()
        .addPathSegment("api")
        .addPathSegment("v1")
        .addPathSegment("settings.oauth")
        .build()

    val request = Request.Builder().url(url).get().build()

    handleRestCall<SettingsOauth>(request, SettingsOauth::class.java)
}

suspend fun RocketChatClient.settings(vararg filter: String): Map<String, Value<Any>> = withContext(CommonPool) {
    val url = restUrl.newBuilder().apply {
        addPathSegment("api")
        addPathSegment("v1")
        addPathSegment("settings.public")
        addQueryParameter("count", "0")
        addQueryParameter("fields", "{\"type\": 1}")
        if (filter.isNotEmpty()) {
            val adapter = moshi.adapter<List<String>>(Types.newParameterizedType(List::class.java, String::class.java))
            val fields = adapter.toJson(filter.asList())
            addQueryParameter("query", "{\"_id\": {\"\$in\": $fields}}")
        }
    }.build()

    val request = Request.Builder().url(url).get().build()

    val type = Types.newParameterizedType(
        Map::class.java, String::class.java,
        Types.newParameterizedType(Value::class.java, Any::class.java)
    )
    handleRestCall<Map<String, Value<Any>>>(request, type)
}