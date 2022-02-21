package uk.gov.hmcts.reform.sscs.callback.handlers;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.callback.CallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@Slf4j
@Service
public class ManualCaseCreatedHandler implements CallbackHandler<SscsCaseData> {

    private final DispatchPriority dispatchPriority;

    private final CcdService ccdService;

    private final IdamService idamService;

    @Autowired
    public ManualCaseCreatedHandler(CcdService ccdService,
                                 IdamService idamService) {
        this.dispatchPriority = DispatchPriority.LATEST;
        this.ccdService = ccdService;
        this.idamService = idamService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");


        return callbackType.equals(CallbackType.SUBMITTED)
            && (callback.getEvent() == EventType.NON_COMPLIANT
            || callback.getEvent() == EventType.INCOMPLETE_APPLICATION_RECEIVED
            || callback.getEvent() == EventType.VALID_APPEAL_CREATED);
    }

    @Override
    public void handle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        Long caseId = callback.getCaseDetails().getId();
        log.info("Manually created case handler for case id {}", caseId);

        IdamTokens idamTokens = idamService.getIdamTokens();

        //get the supplementary data
        CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        Map<String, Map<String, Object>> supplementaryData = caseDetails.getSupplementaryData();
        //Map<String, Map<String, Object>> supplementaryData = supplementaryDataRequestMap;

        boolean changed = false;
        if (supplementaryData == null) {
            supplementaryData = supplementaryDataRequestMap;
            changed = true;
        }
        if (supplementaryData.get("$set") == null) {
            supplementaryData.put("$set", hmctsServiceIdMap);
            changed = true;
        }
        if (supplementaryData.get("$set").get("HMCTSServiceId") == null) {
            supplementaryData.get("$set").put("HMCTSServiceId", "BBA3");
            changed = true;
        }
        if (changed) {
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                log.info(objectMapper.writeValueAsString(supplementaryData));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            try {
                Map<String, Map<String, Map<String, Object>>> supplementaryDataUpdates = new HashMap<>();
                supplementaryDataUpdates.put("supplementary_data_updates", supplementaryData);
                log.info(objectMapper.writeValueAsString(supplementaryDataUpdates));
                ccdService.setSupplementaryData(idamTokens, caseId, supplementaryDataUpdates);
            } catch (Exception e) {
                log.error("Error sending supplementary for caseId {}", caseId, e);
            }
        }

    }

    private Map<String, Object> hmctsServiceIdMap = new HashMap<>() {
        {
            put("HMCTSServiceId", "BBA3");
        }
    };

    private Map<String, Map<String, Object>> supplementaryDataRequestMap = new HashMap<>() {
        {
            put("$set", hmctsServiceIdMap);
        }
    };


    @Override
    public DispatchPriority getPriority() {
        return this.dispatchPriority;
    }
}
