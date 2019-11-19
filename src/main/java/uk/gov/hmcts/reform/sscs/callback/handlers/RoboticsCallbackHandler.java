package uk.gov.hmcts.reform.sscs.callback.handlers;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.*;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.callback.CallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.service.RoboticsService;

@Slf4j
@Service
public class RoboticsCallbackHandler implements CallbackHandler<SscsCaseData> {

    private final RoboticsService roboticsService;
    private final DispatchPriority dispatchPriority;

    @Autowired
    public RoboticsCallbackHandler(RoboticsService roboticsService
    ) {
        this.roboticsService = roboticsService;
        this.dispatchPriority = DispatchPriority.EARLIEST;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.SUBMITTED)
            && (callback.getEvent() == SEND_TO_DWP
            || callback.getEvent() == READY_TO_LIST
            || callback.getEvent() == VALID_APPEAL
            || callback.getEvent() == INTERLOC_VALID_APPEAL
            || callback.getEvent() == RESEND_CASE_TO_GAPS2
            || callback.getEvent() == APPEAL_TO_PROCEED);
    }

    @Override
    public void handle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        log.info("Processing robotics for case id {} in evidence share service", callback.getCaseDetails().getId());

        try {
            boolean isCaseValidToSendToRobotics = checkCaseValidToSendToRobotics(callback);
            log.info("Is case valid to send to robotics {} for case id {}", isCaseValidToSendToRobotics, callback.getCaseDetails().getId());

            if (isCaseValidToSendToRobotics) {
                roboticsService.sendCaseToRobotics(callback.getCaseDetails());
            }
        } catch (Exception e) {
            log.error("Error when sending to robotics: {}", callback.getCaseDetails().getId(), e);
        }
    }

    private boolean checkCaseValidToSendToRobotics(Callback<SscsCaseData> callback) {

        log.info("The callback event is {} and the createdInGapsFrom field is {} for case id {}", callback.getEvent(), callback.getCaseDetails().getCaseData().getCreatedInGapsFrom(), callback.getCaseDetails().getId());

        return callback.getEvent() == RESEND_CASE_TO_GAPS2
            || callback.getCaseDetails().getCaseData().getCreatedInGapsFrom() == null
            || StringUtils.equalsIgnoreCase(callback.getCaseDetails().getCaseData().getCreatedInGapsFrom(), callback.getCaseDetails().getState().getId()) ? true : false;
    }

    @Override
    public DispatchPriority getPriority() {
        return this.dispatchPriority;
    }
}
