package uk.gov.hmcts.reform.sscs.service.placeholders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.DWP_ADDRESS_LINE1_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.DWP_ADDRESS_LINE2_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.DWP_ADDRESS_LINE3_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderConstants.DWP_ADDRESS_LINE4_LITERAL;
import static uk.gov.hmcts.reform.sscs.service.placeholders.PlaceholderHelper.buildCaseData;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.domain.DwpAddress;
import uk.gov.hmcts.reform.sscs.service.DwpAddressLookup;

@RunWith(MockitoJUnitRunner.class)
public class DwpAddressPlaceholderServiceTest {

    private static final String RPC_ADDRESS1 = "HM Courts & Tribunals Service";
    private static final String POSTCODE = "L2 5UZ";

    private SscsCaseData caseData;

    @Mock
    private DwpAddressLookup dwpAddressLookup;

    @InjectMocks
    private DwpAddressPlaceholderService dwpAddressPlaceholderService;
    private Map<String, Object> placeholders;
    private DwpAddress dwpAddress;

    @Before
    public void setUp() {
        caseData = buildCaseData();
        placeholders = new ConcurrentHashMap<>();
    }

    @Test
    public void anAddressWithOneLineAndPostCodeWillNotHaveRow3AndRow4() {
        dwpAddress = new DwpAddress(RPC_ADDRESS1, "", POSTCODE);
        when(dwpAddressLookup.lookup("PIP", "1")).thenReturn(dwpAddress);

        dwpAddressPlaceholderService.populatePlaceholders(placeholders, caseData);

        assertEquals(dwpAddress.lines()[0], placeholders.get(DWP_ADDRESS_LINE1_LITERAL));
        assertEquals(dwpAddress.lines()[1], placeholders.get(DWP_ADDRESS_LINE2_LITERAL));
        assertNull(placeholders.get(DWP_ADDRESS_LINE3_LITERAL));
        assertNull(placeholders.get(DWP_ADDRESS_LINE4_LITERAL));
    }

    @Test
    public void addressWithOneLineWillNotHaveRow2AndRow3AndRow4() {
        dwpAddress = new DwpAddress("", "", POSTCODE);
        when(dwpAddressLookup.lookup("PIP", "1")).thenReturn(dwpAddress);

        dwpAddressPlaceholderService.populatePlaceholders(placeholders, caseData);

        assertEquals(dwpAddress.lines()[0], placeholders.get(DWP_ADDRESS_LINE1_LITERAL));
        assertNull(placeholders.get(DWP_ADDRESS_LINE2_LITERAL));
        assertNull(placeholders.get(DWP_ADDRESS_LINE3_LITERAL));
        assertNull(placeholders.get(DWP_ADDRESS_LINE4_LITERAL));
    }

    @Test
    public void addressWithNoLinesWillNotHaveADwpAddress() {
        dwpAddress = new DwpAddress("", "", "");
        when(dwpAddressLookup.lookup("PIP", "1")).thenReturn(dwpAddress);

        dwpAddressPlaceholderService.populatePlaceholders(placeholders, caseData);

        assertNull(placeholders.get(DWP_ADDRESS_LINE1_LITERAL));
        assertNull(placeholders.get(DWP_ADDRESS_LINE2_LITERAL));
        assertNull(placeholders.get(DWP_ADDRESS_LINE3_LITERAL));
        assertNull(placeholders.get(DWP_ADDRESS_LINE4_LITERAL));
    }


}
