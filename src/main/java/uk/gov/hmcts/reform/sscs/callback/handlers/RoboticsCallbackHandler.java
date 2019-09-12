package uk.gov.hmcts.reform.sscs.callback.handlers;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;

import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.callback.CallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.model.dwp.OfficeMapping;
import uk.gov.hmcts.reform.sscs.service.DwpAddressLookupService;
import uk.gov.hmcts.reform.sscs.service.RoboticsService;

@Slf4j
@Service
public class RoboticsCallbackHandler implements CallbackHandler<SscsCaseData> {

    private final RoboticsService roboticsService;
    private final DwpAddressLookupService dwpAddressLookupService;
    private final DispatchPriority dispatchPriority;
    private final boolean readyToListFeatureEnabled;
    private List<String> offices;

    @Autowired
    public RoboticsCallbackHandler(RoboticsService roboticsService,
                                   DwpAddressLookupService dwpAddressLookupService,
                                   @Value("${robotics.readyToList.feature}") boolean readyToListFeatureEnabled,
                                   @Value("#{'${robotics.readyToList.offices}'.split(',')}") List<String> offices) {
        this.roboticsService = roboticsService;
        this.dwpAddressLookupService = dwpAddressLookupService;
        this.dispatchPriority = DispatchPriority.EARLIEST;
        this.readyToListFeatureEnabled = readyToListFeatureEnabled;
        this.offices = offices;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.SUBMITTED)
            && (callback.getEvent() == SEND_TO_DWP
            || callback.getEvent() == VALID_APPEAL
            || callback.getEvent() == INTERLOC_VALID_APPEAL
            || callback.getEvent() == RESEND_CASE_TO_GAPS2);
    }

    @Override
    public void handle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        log.info("Processing robotics for case id {} in evidence share service", callback.getCaseDetails().getId());

        try {
            if (checkCaseValidToSendToRobotics(callback)) {
                roboticsService.sendCaseToRobotics(callback.getCaseDetails());
            }
        } catch (Exception e) {
            log.error("Error when sending to robotics: {}", callback.getCaseDetails().getId(), e);
        }
    }

    private boolean checkCaseValidToSendToRobotics(Callback<SscsCaseData> callback) {
        if (readyToListFeatureEnabled && callback.getEvent() != RESEND_CASE_TO_GAPS2 && callback.getCaseDetails().getCaseData().getAppeal().getBenefitType().getCode().equalsIgnoreCase("pip")) {
            CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
            Optional<OfficeMapping> selectedOfficeMapping = dwpAddressLookupService.getDwpMappingByOffice("pip", caseDetails.getCaseData().getAppeal().getMrnDetails().getDwpIssuingOffice());

            if (!selectedOfficeMapping.isPresent()) {
                log.error("Selected DWP office {} could not be found so skipping robotics for case : {}", callback.getCaseDetails().getCaseData().getAppeal().getMrnDetails().getDwpIssuingOffice(), callback.getCaseDetails().getId());
                return false;
            }
            for (String office : offices) {
                Optional<OfficeMapping> officeMapping = dwpAddressLookupService.getDwpMappingByOffice("pip", office);
                if (selectedOfficeMapping.equals(officeMapping)) {
                    return caseDetails.getState().equals(READY_TO_LIST) ? true : false;
                }
            }
            if (caseDetails.getState().equals(READY_TO_LIST)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public DispatchPriority getPriority() {
        return this.dispatchPriority;
    }
}
