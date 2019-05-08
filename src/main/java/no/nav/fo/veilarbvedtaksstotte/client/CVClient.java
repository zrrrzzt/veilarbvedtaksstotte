package no.nav.fo.veilarbvedtaksstotte.client;

import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;

import static no.nav.apiapp.util.UrlUtils.joinPaths;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Slf4j
public class CVClient extends BaseClient {

    public static final String CV_API_PROPERTY_NAME = "CV_API_URL";
    public static final String PAM_CV_API = "pam-cv-api";

    @Inject
    public CVClient(Provider<HttpServletRequest> httpServletRequestProvider) {
        super(getRequiredProperty(CV_API_PROPERTY_NAME), httpServletRequestProvider);
    }

    public String hentCV(String fnr) {
        return get(joinPaths(baseUrl, "rest", "v1", "arbeidssoker", fnr), String.class)
                .withStatusCheck()
                .getData()
                .orElseThrow(() -> new RuntimeException("Feil ved kall mot pam-cv-api/rest/v1/arbeidssoker/{fnr}"));
    }
}
