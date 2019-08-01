package uk.gov.hmcts.reform.sscs.callback.handlers;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.CREATE_APPEAL_PDF;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.INTERLOC_VALID_APPEAL;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.SEND_TO_DWP;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.VALID_APPEAL;

import lombok.extern.slf4j.Slf4j;
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
    public RoboticsCallbackHandler(RoboticsService roboticsService) {
        this.roboticsService = roboticsService;
        this.dispatchPriority = DispatchPriority.EARLIEST;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.SUBMITTED)
            && (callback.getEvent() == SEND_TO_DWP
            || callback.getEvent() == VALID_APPEAL
            || callback.getEvent() == INTERLOC_VALID_APPEAL
            || callback.getEvent() == CREATE_APPEAL_PDF);
    }

    @Override
    public void handle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        log.info("Processing robotics for case id {} in evidence share service", callback.getCaseDetails().getId());

        try {
            roboticsService.sendCaseToRobotics(callback.getCaseDetails().getCaseData());
        } catch (Exception e) {
            log.error("Error when sending to robotics: {}", callback.getCaseDetails().getId(), e);
        }
    }

    @Override
    public DispatchPriority getPriority() {
        return this.dispatchPriority;
    }
}
