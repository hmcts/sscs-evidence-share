package uk.gov.hmcts.reform.sscs.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@Component
@Slf4j
public class EvidenceShareService {
    private final SscsCaseCallbackDeserializer sscsCaseCallbackDeserializer;

    @Autowired
    public EvidenceShareService(SscsCaseCallbackDeserializer sscsCaseCallbackDeserializer) {
        this.sscsCaseCallbackDeserializer = sscsCaseCallbackDeserializer;
    }

    public long processMessage(final String message) {
        Callback<SscsCaseData> sscsCaseDataCallback = deserialise(message);
        log.info("Callback for event {} with state {}", sscsCaseDataCallback.getEvent(),
            sscsCaseDataCallback.getCaseDetails().getState());

        return sscsCaseDataCallback.getCaseDetails().getId();
    }

    private Callback<SscsCaseData> deserialise(final String message) {
        return sscsCaseCallbackDeserializer.deserialize(message);
    }
}
