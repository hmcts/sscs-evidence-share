package uk.gov.hmcts.reform.sscs.callback;

import static java.util.Objects.requireNonNull;

import java.util.List;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseData;

@Component
public class CallbackDispatcher<T extends CaseData> {

    private final List<CallbackHandler<T>> callbackHandlers;

    public CallbackDispatcher(
        List<CallbackHandler<T>> callbackHandlers
    ) {
        requireNonNull(callbackHandlers, "callbackHandlers must not be null");
        this.callbackHandlers = callbackHandlers;
    }

    public void handle(CallbackType callbackType, Callback<T> callback) {
        requireNonNull(callback, "callback must not be null");

        dispatchToHandlers(callbackType, callback, callbackHandlers, DispatchPriority.EARLIEST);
        dispatchToHandlers(callbackType, callback, callbackHandlers, DispatchPriority.LATEST);
    }

    private void dispatchToHandlers(
        CallbackType callbackType, Callback<T> callback,
        List<CallbackHandler<T>> callbackHandlers, DispatchPriority dispatchPriority) {

        for (CallbackHandler<T> callbackHandler : callbackHandlers) {

            if (callbackHandler.canHandle(callbackType, callback, dispatchPriority)) {
                callbackHandler.handle(callbackType, callback, dispatchPriority);
            }
        }
    }
}
