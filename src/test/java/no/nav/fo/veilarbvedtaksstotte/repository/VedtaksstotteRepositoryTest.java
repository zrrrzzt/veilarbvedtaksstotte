package no.nav.fo.veilarbvedtaksstotte.repository;

import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import no.nav.fo.veilarbvedtaksstotte.domain.DokumentSendtDTO;
import no.nav.fo.veilarbvedtaksstotte.domain.Vedtak;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.Hovedmal;
import no.nav.fo.veilarbvedtaksstotte.domain.enums.Innsatsgruppe;
import no.nav.fo.veilarbvedtaksstotte.utils.DbTestUtils;
import org.junit.*;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Arrays;
import java.util.List;

import static no.nav.fo.veilarbvedtaksstotte.utils.TestData.*;
import static org.junit.Assert.*;

@Ignore
public class VedtaksstotteRepositoryTest {

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private static JdbcTemplate db;
    private static KilderRepository kilderRepository;
    private static VedtaksstotteRepository vedtaksstotteRepository;

    @BeforeClass
    public static void setup() {
        db = DbTestUtils.setupDb(pg.getEmbeddedPostgres());
        kilderRepository = new KilderRepository(db);
        vedtaksstotteRepository = new VedtaksstotteRepository(db, kilderRepository);
    }

    @Before
    public void cleanup() {
        DbTestUtils.cleanupDb(db);
    }

    @Test
    public void testSlettUtkast() {
        vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_VEILEDER_ENHET_ID, TEST_VEILEDER_ENHET_NAVN);

        Vedtak utkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);

        kilderRepository.lagKilder(TEST_KILDER, utkast.getId());

        vedtaksstotteRepository.slettUtkast(TEST_AKTOR_ID);

        assertNull(vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID));
    }

    @Test
    public void testHentUtkast() {
        vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_VEILEDER_ENHET_ID, TEST_VEILEDER_ENHET_NAVN);

        Vedtak utkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);

        assertEquals(TEST_AKTOR_ID, utkast.getAktorId());
        assertEquals(TEST_VEILEDER_IDENT, utkast.getVeilederIdent());
        assertEquals(TEST_VEILEDER_ENHET_ID, utkast.getVeilederEnhetId());
        assertEquals(TEST_VEILEDER_ENHET_NAVN, utkast.getVeilederEnhetNavn());
    }

    @Test
    public void testHentUtkastHvisIkkeFinnes() {
        assertNull(vedtaksstotteRepository.hentUtkast("54385638405"));
    }

    @Test
    public void testUtkastTilSendtVedtakMedBeslutterFlyt() {
        vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_VEILEDER_ENHET_ID, TEST_VEILEDER_ENHET_NAVN);

        Vedtak utkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);

        utkast.setBegrunnelse(TEST_BEGRUNNELSE);
        utkast.setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS);
        utkast.setHovedmal(Hovedmal.SKAFFE_ARBEID);

        vedtaksstotteRepository.oppdaterUtkast(utkast.getId(), utkast);

        kilderRepository.lagKilder(TEST_KILDER, utkast.getId());

        vedtaksstotteRepository.markerUtkastSomSendtTilBeslutter(TEST_AKTOR_ID, TEST_BESLUTTER);

        DokumentSendtDTO dokumentSendtDTO = new DokumentSendtDTO(TEST_JOURNALPOST_ID, TEST_DOKUMENT_ID);

        vedtaksstotteRepository.ferdigstillVedtak(utkast.getId(), dokumentSendtDTO, TEST_BESLUTTER);

        Vedtak fattetVedtak = vedtaksstotteRepository.hentVedtak(utkast.getId());

        assertNotNull(fattetVedtak);

        assertTrue(fattetVedtak.isGjeldende());

        assertTrue(fattetVedtak.isSendtTilBeslutter());
        assertEquals(fattetVedtak.getBeslutterNavn(), TEST_BESLUTTER);

        assertEquals(fattetVedtak.getBegrunnelse(), TEST_BEGRUNNELSE);
        assertEquals(fattetVedtak.getInnsatsgruppe(), Innsatsgruppe.STANDARD_INNSATS);
        assertEquals(fattetVedtak.getHovedmal(), Hovedmal.SKAFFE_ARBEID);

        assertEquals(fattetVedtak.getJournalpostId(), dokumentSendtDTO.getJournalpostId());
        assertEquals(fattetVedtak.getDokumentInfoId(), dokumentSendtDTO.getDokumentId());
    }

    @Test
    public void testSettGjeldendeTilHistorisk() {

        DokumentSendtDTO dokumentSendtDTO = new DokumentSendtDTO(TEST_JOURNALPOST_ID, TEST_DOKUMENT_ID);

        vedtaksstotteRepository.opprettUtkast(TEST_AKTOR_ID, TEST_VEILEDER_IDENT, TEST_VEILEDER_ENHET_ID, TEST_VEILEDER_ENHET_NAVN);
        Vedtak utkast = vedtaksstotteRepository.hentUtkast(TEST_AKTOR_ID);

        utkast.setBegrunnelse(TEST_BEGRUNNELSE);
        utkast.setInnsatsgruppe(Innsatsgruppe.STANDARD_INNSATS);
        utkast.setHovedmal(Hovedmal.SKAFFE_ARBEID);
        vedtaksstotteRepository.oppdaterUtkast(utkast.getId(), utkast);
        kilderRepository.lagKilder(TEST_KILDER, utkast.getId());

        vedtaksstotteRepository.ferdigstillVedtak(utkast.getId(), dokumentSendtDTO, TEST_BESLUTTER);

        vedtaksstotteRepository.settGjeldendeVedtakTilHistorisk(TEST_AKTOR_ID);

        List<Vedtak> alleVedtak = vedtaksstotteRepository.hentVedtak(TEST_AKTOR_ID);

        alleVedtak.forEach((v) -> {
            System.out.println(v.toString());
        });

        Vedtak fattetVedtak = vedtaksstotteRepository.hentVedtak(utkast.getId());

        assertNotNull(fattetVedtak);
        assertFalse(fattetVedtak.isGjeldende());

    }

}