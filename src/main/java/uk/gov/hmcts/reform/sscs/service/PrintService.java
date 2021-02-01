package uk.gov.hmcts.reform.sscs.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.domain.FurtherEvidenceLetterType;

public interface PrintService {
    Optional<UUID> sendToBulkPrint(List<Pdf> pdfs, SscsCaseData sscsCaseData, FurtherEvidenceLetterType letterType, EventType event);

    Optional<UUID> sendToBulkPrint(List<Pdf> pdfs, SscsCaseData sscsCaseData);
}
