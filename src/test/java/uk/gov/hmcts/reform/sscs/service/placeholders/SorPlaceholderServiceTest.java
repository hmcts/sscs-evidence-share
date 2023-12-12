package uk.gov.hmcts.reform.sscs.service.placeholders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseData;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.ENTITY_TYPE;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.domain.FurtherEvidenceLetterType;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class SorPlaceholderServiceTest {
    private SscsCaseData caseData;

    @Mock
    PlaceholderService placeholderService;

    SorPlaceholderService sorPlaceholderService;

    @BeforeEach
    void setup() {
        sorPlaceholderService = new SorPlaceholderService(placeholderService);
        caseData = buildCaseData();
    }

    @Test
    void caseDataNull() {
        assertThrows(NullPointerException.class, () ->
            sorPlaceholderService.populatePlaceholders(null, null, null, null));
    }


    @Test
    void appellantTest() {
        var placeholders = sorPlaceholderService.populatePlaceholders(caseData, FurtherEvidenceLetterType.APPELLANT_LETTER,
            Appellant.class.getSimpleName(), null);

        assertEquals(caseData.getAppeal().getAppellant().getName().getFullNameNoTitle(), placeholders.get(PlaceholderConstants.NAME));
        assertEquals(Appellant.class.getSimpleName(), placeholders.get(ENTITY_TYPE));
    }
}
