package no.nav.veilarbvedtaksstotte.config

import no.nav.common.abac.AbacCachedClient
import no.nav.common.abac.AbacClient
import no.nav.common.abac.AbacHttpClient
import no.nav.common.auth.context.AuthContextHolder
import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.client.aktoroppslag.CachedAktorOppslagClient
import no.nav.common.client.aktoroppslag.PdlAktorOppslagClient
import no.nav.common.client.pdl.PdlClientImpl
import no.nav.common.featuretoggle.UnleashClient
import no.nav.common.featuretoggle.UnleashClientImpl
import no.nav.common.job.leader_election.LeaderElectionClient
import no.nav.common.job.leader_election.LeaderElectionHttpClient
import no.nav.common.metrics.InfluxClient
import no.nav.common.metrics.MetricsClient
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient
import no.nav.common.utils.Credentials
import no.nav.common.utils.EnvironmentUtils
import no.nav.common.utils.UrlUtils
import no.nav.poao_tilgang.client.PoaoTilgangCachedClient
import no.nav.poao_tilgang.client.PoaoTilgangClient
import no.nav.poao_tilgang.client.PoaoTilgangHttpClient
import no.nav.veilarbvedtaksstotte.client.DownstreamAPIs.dokarkiv
import no.nav.veilarbvedtaksstotte.client.DownstreamAPIs.pdl
import no.nav.veilarbvedtaksstotte.client.DownstreamAPIs.regoppslag
import no.nav.veilarbvedtaksstotte.client.DownstreamAPIs.saf
import no.nav.veilarbvedtaksstotte.client.DownstreamAPIs.veilarbarena
import no.nav.veilarbvedtaksstotte.client.DownstreamAPIs.veilarboppfolging
import no.nav.veilarbvedtaksstotte.client.DownstreamAPIs.veilarbperson
import no.nav.veilarbvedtaksstotte.client.DownstreamAPIs.veilarbvedtakinfo
import no.nav.veilarbvedtaksstotte.client.DownstreamAPIs.veilarbveileder
import no.nav.veilarbvedtaksstotte.client.arena.VeilarbarenaClient
import no.nav.veilarbvedtaksstotte.client.arena.VeilarbarenaClientImpl
import no.nav.veilarbvedtaksstotte.client.dokarkiv.DokarkivClient
import no.nav.veilarbvedtaksstotte.client.dokarkiv.DokarkivClientImpl
import no.nav.veilarbvedtaksstotte.client.dokarkiv.SafClient
import no.nav.veilarbvedtaksstotte.client.dokarkiv.SafClientImpl
import no.nav.veilarbvedtaksstotte.client.dokdistfordeling.DokdistribusjonClient
import no.nav.veilarbvedtaksstotte.client.dokdistfordeling.DokdistribusjonClientImpl
import no.nav.veilarbvedtaksstotte.client.egenvurdering.VeilarbvedtakinfoClient
import no.nav.veilarbvedtaksstotte.client.egenvurdering.VeilarbvedtakinfoClientImpl
import no.nav.veilarbvedtaksstotte.client.norg2.Norg2Client
import no.nav.veilarbvedtaksstotte.client.norg2.Norg2ClientImpl
import no.nav.veilarbvedtaksstotte.client.pdf.PdfClient
import no.nav.veilarbvedtaksstotte.client.pdf.PdfClientImpl
import no.nav.veilarbvedtaksstotte.client.person.VeilarbpersonClient
import no.nav.veilarbvedtaksstotte.client.person.VeilarbpersonClientImpl
import no.nav.veilarbvedtaksstotte.client.registrering.VeilarbregistreringClient
import no.nav.veilarbvedtaksstotte.client.registrering.VeilarbregistreringClientImpl
import no.nav.veilarbvedtaksstotte.client.regoppslag.RegoppslagClient
import no.nav.veilarbvedtaksstotte.client.regoppslag.RegoppslagClientImpl
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.VeilarboppfolgingClient
import no.nav.veilarbvedtaksstotte.client.veilarboppfolging.VeilarboppfolgingClientImpl
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.VeilarbveilederClient
import no.nav.veilarbvedtaksstotte.client.veilederogenhet.VeilarbveilederClientImpl
import no.nav.veilarbvedtaksstotte.service.OboContexService
import no.nav.veilarbvedtaksstotte.utils.DownstreamApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ClientConfig {
    @Bean
    fun arenaClient(oboContexService: OboContexService): VeilarbarenaClient {
        val clientCluster = if (isProduction) "prod-fss" else "dev-fss"
        val userTokenSupplier = oboContexService.userTokenSupplier(
            veilarbarena.invoke(clientCluster)
        )
        return VeilarbarenaClientImpl(
            naisPreprodOrNaisAdeoIngress("veilarbarena", true),
            userTokenSupplier
        )
    }

    @Bean
    fun pdfClient(): PdfClient {
        return PdfClientImpl(UrlUtils.createServiceUrl("pto-pdfgen", false))
    }

    @Bean
    fun egenvurderingClient(oboContexService: OboContexService): VeilarbvedtakinfoClient {
        val clientCluster = if (isProduction) "prod-fss" else "dev-fss"
        val userTokenSupplier = oboContexService.userTokenSupplier(
            veilarbvedtakinfo.invoke(clientCluster)
        )
        return VeilarbvedtakinfoClientImpl(
            naisPreprodOrNaisAdeoIngress("veilarbvedtakinfo", true),
            userTokenSupplier
        )
    }

    @Bean
    fun oppfolgingClient(tokenClient: AzureAdMachineToMachineTokenClient): VeilarboppfolgingClient {
        val veilarboppfolging = veilarboppfolging.invoke(if (isProduction) "prod-fss" else "dev-fss")
        val url = UrlUtils.createServiceUrl(veilarboppfolging.serviceName, veilarboppfolging.namespace, true)
        return VeilarboppfolgingClientImpl(
            url
        ) { tokenClient.createMachineToMachineToken(tokenScope(veilarboppfolging)) }
    }

    @Bean
    fun personClient(oboContexService: OboContexService): VeilarbpersonClient {
        val clientCluster = if (isProduction) "prod-fss" else "dev-fss"
        val userTokenSupplier = oboContexService.userTokenSupplier(
            veilarbperson.invoke(clientCluster)
        )
        return VeilarbpersonClientImpl(naisPreprodOrNaisAdeoIngress("veilarbperson", true), userTokenSupplier)
    }

    @Bean
    fun registreringClient(oboContexService: OboContexService): VeilarbregistreringClient {
        val clientCluster = if (isProduction) "prod-fss" else "dev-fss"
        val userTokenSupplier = oboContexService.userTokenSupplier(
            veilarbperson.invoke(clientCluster)
        )
        return VeilarbregistreringClientImpl(
            naisPreprodOrNaisAdeoIngress("veilarbperson", true),
            userTokenSupplier
        )
    }

    @Bean
    fun safClient(oboContexService: OboContexService): SafClient {
        val safClient = saf.invoke(if (isProduction) "prod-fss" else "dev-fss")
        val userTokenSupplier = oboContexService.userTokenSupplier(safClient)
        val serviceNameForIngress = "saf"
        return SafClientImpl(
            naisPreprodOrNaisAdeoIngress(serviceNameForIngress, false),
            userTokenSupplier
        )
    }

    @Bean
    fun veilederOgEnhetClient(
        authContextHolder: AuthContextHolder?,
        oboContexService: OboContexService
    ): VeilarbveilederClient {
        val veilarbveileder = veilarbveileder.invoke(if (isProduction) "prod-fss" else "dev-fss")
        val userTokenSupplier = oboContexService.userTokenSupplier(veilarbveileder)
        return VeilarbveilederClientImpl(
            UrlUtils.createServiceUrl(veilarbveileder.serviceName, veilarbveileder.namespace, true),
            authContextHolder,
            userTokenSupplier
        )
    }

    @Bean
    fun dokarkivClient(oboContexService: OboContexService): DokarkivClient {
        val dokarkivClient = dokarkiv.invoke(if (isProduction) "prod-fss" else "dev-fss")
        val userTokenSupplier = oboContexService.userTokenSupplier(dokarkivClient)
        val url =
            if (isProduction) UrlUtils.createProdInternalIngressUrl(dokarkivClient.serviceName) else UrlUtils.createDevInternalIngressUrl(
                dokarkivClient.serviceName
            )
        return DokarkivClientImpl(
            url,
            userTokenSupplier
        )
    }

    @Bean
    fun regoppslagClient(tokenClient: AzureAdMachineToMachineTokenClient): RegoppslagClient {
        val regoppslag = regoppslag.invoke(if (isProduction) "prod-fss" else "dev-fss")
        val url =
            if (isProduction) UrlUtils.createProdInternalIngressUrl(regoppslag.serviceName) else UrlUtils.createDevInternalIngressUrl(
                regoppslag.serviceName
            )
        return RegoppslagClientImpl(url) { tokenClient.createMachineToMachineToken(tokenScope(regoppslag)) }
    }

    @Bean
    fun dokDistribusjonClient(tokenClient: AzureAdMachineToMachineTokenClient): DokdistribusjonClient {
        val appName = if (isProduction) "dokdistfordeling" else "dokdistfordeling-q1"
        val url =
            if (isProduction) UrlUtils.createProdInternalIngressUrl(appName) else UrlUtils.createDevInternalIngressUrl(
                appName
            )

        // dokdistfordeling bruker saf token scope
        val safTokenScope =
            if (isProduction) "api://prod-fss.teamdokumenthandtering.saf/.default" else "api://dev-fss.teamdokumenthandtering.saf-q1/.default"
        return DokdistribusjonClientImpl(url) { tokenClient.createMachineToMachineToken(safTokenScope) }
    }

    @Bean
    fun aktorOppslagClient(tokenClient: AzureAdMachineToMachineTokenClient): AktorOppslagClient {
        val pdl = pdl.invoke(if (isProduction) "prod-fss" else "dev-fss")
        val pdlUrl =
            if (isProduction) UrlUtils.createProdInternalIngressUrl(pdl.serviceName) else UrlUtils.createDevInternalIngressUrl(
                pdl.serviceName
            )
        val pdlClient = PdlClientImpl(pdlUrl) { tokenClient.createMachineToMachineToken(tokenScope(pdl)) }
        return CachedAktorOppslagClient(PdlAktorOppslagClient(pdlClient))
    }

    @Bean
    fun unleashClient(properties: EnvironmentProperties): UnleashClient {
        return UnleashClientImpl(properties.unleashUrl, ApplicationConfig.APPLICATION_NAME)
    }

    @Bean
    fun influxMetricsClient(): MetricsClient {
        return InfluxClient()
    }

    @Bean
    fun leaderElectionClient(): LeaderElectionClient {
        return LeaderElectionHttpClient()
    }

    @Bean
    fun norg2Client(properties: EnvironmentProperties): Norg2Client {
        return Norg2ClientImpl(properties.norg2Url)
    }

    @Bean
    fun abacClient(properties: EnvironmentProperties, serviceUserCredentials: Credentials): AbacClient {
        return AbacCachedClient(
            AbacHttpClient(
                properties.abacUrl,
                serviceUserCredentials.username,
                serviceUserCredentials.password
            )
        )
    }

    @Bean
    fun poaoTilgangClient(
        properties: EnvironmentProperties,
        tokenClient: AzureAdMachineToMachineTokenClient
    ): PoaoTilgangClient {
        return PoaoTilgangCachedClient(
            PoaoTilgangHttpClient(
                properties.poaoTilgangUrl,
                { tokenClient.createMachineToMachineToken(properties.poaoTilgangScope) })
        )
    }

    companion object {
        private val isProduction: Boolean
            get() = EnvironmentUtils.isProduction().orElseThrow()

        private fun naisPreprodOrNaisAdeoIngress(appName: String, withAppContextPath: Boolean): String {
            return if (isProduction) UrlUtils.createNaisAdeoIngressUrl(
                appName,
                withAppContextPath
            ) else UrlUtils.createNaisPreprodIngressUrl(appName, "q1", withAppContextPath)
        }

        private fun tokenScope(downstreamApi: DownstreamApi): String {
            return String.format(
                "api://%s.%s.%s/.default",
                downstreamApi.cluster,
                downstreamApi.namespace,
                downstreamApi.serviceName
            )
        }
    }
}
