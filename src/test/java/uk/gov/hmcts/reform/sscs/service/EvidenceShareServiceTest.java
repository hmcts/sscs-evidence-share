package uk.gov.hmcts.reform.sscs.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.APPEAL_CREATED;

@RunWith(MockitoJUnitRunner.class)
public class EvidenceShareServiceTest {

    private static final String MY_JSON_DATA = "{myJson: true}";

    @Mock
    private SscsCaseCallbackDeserializer sscsCaseCallbackDeserializer;

    private EvidenceShareService evidenceShareService;

    @Before
    public void setUp() {
        evidenceShareService = new EvidenceShareService(sscsCaseCallbackDeserializer);
    }

    @Test
    public void willProcessAJsonSscsCaseData() {

        long expectedId = 123L;
        SscsCaseData caseData = SscsCaseData.builder().build();
        CaseDetails<SscsCaseData> caseDetails = new CaseDetails<>(
            expectedId,
            "jurisdiction",
            APPEAL_CREATED,
            caseData,
            LocalDateTime.now()
        );
        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), EventType.EVIDENCE_RECEIVED);
        when(sscsCaseCallbackDeserializer.deserialize(eq(MY_JSON_DATA))).thenReturn(callback);

        long id = evidenceShareService.processMessage(MY_JSON_DATA);

        assertEquals("the id should be " + expectedId, expectedId, id);

    }

}
