package uk.gov.hmcts.reform.sscs.service.placeholders;

import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderHelper.buildCaseData;

import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class OriginalSender60997PlaceholderServiceTest {

    @Mock
    private RpcPlaceholderService rpcPlaceholderService;
    @Mock
    private CommonPlaceholderService commonPlaceholderService;

    @InjectMocks
    private OriginalSender60997PlaceholderService originalSender60997PlaceholderService;

    @Test
    public void populatePlaceHolders() {
        Map<String, Object> actual = originalSender60997PlaceholderService.populatePlaceHolders(buildCaseData());

        String expected = "{original_sender_address_line3=Prudential Buildings, "
            + "original_sender_address_line4=L2 5UZ, "
            + "original_sender_address_line1=HM Courts & Tribunals Service, "
            + "original_sender_address_line2=Social Security & Child Support Appeals}";
        assertEquals(expected, actual.toString());
    }
}
