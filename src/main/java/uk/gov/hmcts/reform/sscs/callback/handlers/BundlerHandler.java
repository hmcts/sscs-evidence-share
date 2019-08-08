package uk.gov.hmcts.reform.sscs.callback.handlers;

import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import java.net.URI;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.bundling.SscsBundlePopulator;
import uk.gov.hmcts.reform.sscs.bundling.SscsBundlingAndStitchingService;
import uk.gov.hmcts.reform.sscs.callback.CallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.config.EvidenceShareConfig;
import uk.gov.hmcts.reform.sscs.docmosis.domain.DocumentHolder;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.exception.BulkPrintException;
import uk.gov.hmcts.reform.sscs.factory.DocumentRequestFactory;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.model.BulkPrintInfo;
import uk.gov.hmcts.reform.sscs.service.BulkPrintService;
import uk.gov.hmcts.reform.sscs.service.DocumentManagementServiceWrapper;
import uk.gov.hmcts.reform.sscs.service.EvidenceManagementService;

@Slf4j
@Service
public class BundlerHandler implements CallbackHandler<SscsCaseData> {

    private final SscsBundlingAndStitchingService sscsBundlingAndStitchingService;


    @Autowired
    public BundlerHandler(SscsBundlingAndStitchingService sscsBundlingAndStitchingService) {
        this.sscsBundlingAndStitchingService = sscsBundlingAndStitchingService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.SUBMITTED)
            && callback.getEvent() == EventType.CREATE_BUNDLE;
    }

    @Override
    public void handle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        sscsBundlingAndStitchingService.bundleAndStitch(caseData);
    }

    @Override
    public DispatchPriority getPriority() {
        return DispatchPriority.EARLY;
    }
}
