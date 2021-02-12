package no.nav.veilarbvedtaksstotte.client.dokdistfordeling

import no.nav.common.health.HealthCheckResult
import no.nav.common.health.HealthCheckUtils
import no.nav.common.rest.client.RestClient
import no.nav.common.rest.client.RestUtils
import no.nav.common.utils.UrlUtils.joinPaths
import no.nav.veilarbvedtaksstotte.utils.RestClientUtils
import no.nav.veilarbvedtaksstotte.utils.deserializeJsonOrThrow
import no.nav.veilarbvedtaksstotte.utils.toJson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.springframework.http.HttpHeaders

class DokdistribusjonClientImpl(val dokdistribusjonUrl: String) : DokdistribusjonClient {

    val client: OkHttpClient = RestClient.baseClient()

    override fun distribuerJournalpost(request: DistribuerJournalpostDTO): DistribuerJournalpostResponsDTO {
        val request = Request.Builder()
                .url(joinPaths(dokdistribusjonUrl, "/rest/v1/distribuerjournalpost"))
                .header(HttpHeaders.AUTHORIZATION, RestClientUtils.authHeaderMedInnloggetBruker())
                .post(RequestBody.create(RestUtils.MEDIA_TYPE_JSON, request.toJson()))
                .build()

        client.newCall(request).execute().use { response ->
            RestUtils.throwIfNotSuccessful(response)
            return response.deserializeJsonOrThrow()
        }
    }

    override fun checkHealth(): HealthCheckResult {
        return HealthCheckUtils.pingUrl(joinPaths(dokdistribusjonUrl, "isReady"), client)
    }
}