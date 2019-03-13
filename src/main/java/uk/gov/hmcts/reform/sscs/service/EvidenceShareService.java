package uk.gov.hmcts.reform.sscs.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.docmosis.domain.DocumentHolder;
import uk.gov.hmcts.reform.sscs.docmosis.factory.DocumentRequestFactory;
import uk.gov.hmcts.reform.sscs.docmosis.service.DocumentManagementService;
import uk.gov.hmcts.reform.sscs.domain.Pdf;

@Component
@Slf4j
public class EvidenceShareService {
    @Autowired
    private SscsCaseCallbackDeserializer sscsCaseCallbackDeserializer;

    @Autowired
    private DocumentManagementService documentManagementService;

    @Autowired
    private DocumentRequestFactory documentRequestFactory;

    public long processMessage(final String message) {
        Callback<SscsCaseData> sscsCaseDataCallback = sscsCaseCallbackDeserializer.deserialize(message);

        log.info("Processing callback event {} for case id {}", sscsCaseDataCallback.getEvent(),
            sscsCaseDataCallback.getCaseDetails().getId());

        DocumentHolder holder = documentRequestFactory.create(sscsCaseDataCallback.getCaseDetails().getCaseData());

        Pdf pdf = documentManagementService.generateDocumentAndAddToCcd(holder, sscsCaseDataCallback.getCaseDetails().getCaseData());

        return sscsCaseDataCallback.getCaseDetails().getId();
    }
}
