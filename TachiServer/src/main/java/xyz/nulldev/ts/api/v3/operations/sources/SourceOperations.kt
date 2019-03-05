package xyz.nulldev.ts.api.v3.operations.sources

import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.source.online.LoginSource
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.api.contract.openapi3.OpenAPI3RouterFactory
import xyz.nulldev.ts.api.v3.OperationGroup
import xyz.nulldev.ts.api.v3.models.WLoginRequest
import xyz.nulldev.ts.api.v3.models.sources.WSource
import xyz.nulldev.ts.api.v3.op
import xyz.nulldev.ts.api.v3.opWithParamsAndContext
import xyz.nulldev.ts.api.v3.util.await
import xyz.nulldev.ts.ext.kInstanceLazy
import java.util.*

private const val SOURCE_ID_PARAM = "sourceId"

class SourceOperations : OperationGroup {
    private val sourceManager: SourceManager by kInstanceLazy()

    override fun register(routerFactory: OpenAPI3RouterFactory) {
        routerFactory.op(::getSources.name, ::getSources)
        routerFactory.op(::getSourceById.name, ::getSourceById)
        routerFactory.opWithParamsAndContext(::applyLoginToSourceBySourceId.name, SOURCE_ID_PARAM, ::applyLoginToSourceBySourceId)
    }

    suspend fun getSources(): List<WSource> {
        return sourceManager.getCatalogueSources().map {
            it.asWeb()
        }
    }

    suspend fun getSourceById(sourceId: String): WSource {
        return sourceManager.get(sourceId.toLongOrNull() ?: notFound())?.asWeb() ?: notFound()
    }

    suspend fun applyLoginToSourceBySourceId(sourceId: String, credentials: WLoginRequest, routingContext: RoutingContext) {
        val source = sourceManager.get(sourceId.toLongOrNull() ?: notFound()) ?: notFound()
        val loginSource = (source as? LoginSource) ?: abort(400)

        val result = try {
            loginSource.login(credentials.username, credentials.password).toSingle().await()
        } catch (e: Exception) {
            false
        }

        routingContext.response().statusCode = if (result) 204 else 401
    }

    fun Source.asWeb(): WSource {
        val locale = (this as? HttpSource)?.lang?.let { Locale(it) }

        return WSource(
                id.toString(),
                (this as? HttpSource)?.lang,
                locale?.getDisplayName(locale),
                locale?.getDisplayName(Locale.US),
                (this as? LoginSource)?.isLogged(),
                name,
                this is LoginSource,
                (this as? CatalogueSource)?.supportsLatest ?: false
        )
    }
}