package uk.gov.hmcts.reform.sscs.callback.handlers;

import static java.util.Objects.requireNonNull;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.callback.CallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.service.RoboticsHandler;

@Component
@Slf4j

@Service
public class RoboticsCallbackHandler implements CallbackHandler<SscsCaseData> {

    private final RoboticsHandler roboticsHandler;

    @Autowired
    public RoboticsCallbackHandler(RoboticsHandler roboticsHandler) {
        this.roboticsHandler = roboticsHandler;
    }

    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback, DispatchPriority priority) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.SUBMITTED)
            && priority == DispatchPriority.EARLIEST
            && (callback.getEvent() == EventType.SEND_TO_DWP
            || callback.getEvent() == EventType.VALID_APPEAL
            || callback.getEvent() == EventType.INTERLOC_VALID_APPEAL);
    }

    public void handle(CallbackType callbackType, Callback<SscsCaseData> callback, DispatchPriority priority) {
        log.info("Processing robotics for case id {} in evidence share service", callback.getCaseDetails().getId());

        try {
            roboticsHandler.sendCaseToRobotics(callback.getCaseDetails().getCaseData());
        } catch (Exception e) {
            log.error("Error when sending to robotics: {}", callback.getCaseDetails().getId(), e);
        }
    }
}
