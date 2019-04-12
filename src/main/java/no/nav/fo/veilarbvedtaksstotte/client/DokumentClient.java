package no.nav.fo.veilarbvedtaksstotte.client;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbvedtaksstotte.domain.DokumentSendtDTO;
import no.nav.fo.veilarbvedtaksstotte.domain.SendDokumentDTO;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;

import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Slf4j
public class DokumentClient extends BaseClient {

    public static final String DOKUMENT_API_PROPERTY_NAME = "VEILARBDOKUMENTAPI_URL";

    @Inject
    public DokumentClient(Provider<HttpServletRequest> httpServletRequestProvider) {
        super(getRequiredProperty(DOKUMENT_API_PROPERTY_NAME), httpServletRequestProvider);
    }

    public DokumentSendtDTO sendDokument(SendDokumentDTO sendDokumentDTO) {
        return postWithClient(baseUrl + "/bestilldokument", sendDokumentDTO, DokumentSendtDTO.class);
    }

}
