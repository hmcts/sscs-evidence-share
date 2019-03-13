package uk.gov.hmcts.reform.sscs.docmosis.service;

import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DocumentPlaceholderService {

    public Map<String, Object> generatePlaceholders() {
        Map<String, Object> placeholders = new HashMap<>();

        placeholders.put("Placeholder", "Value");
        return placeholders;
    }
}
