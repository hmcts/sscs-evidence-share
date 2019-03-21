package uk.gov.hmcts.reform.sscs.domain;

import static org.junit.Assert.assertArrayEquals;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(JUnitParamsRunner.class)
public class DwpAddressTest {

    private static final String LINE_1 = "line1";
    private static final String LINE_2 = "line2";
    private static final String LINE_3 = "line3";
    private static final String POSTCODE = "postcode";

    @Test
    public void linesWith3LinesAndPostCode() {
        DwpAddress address = new DwpAddress(LINE_1, LINE_2, LINE_3, POSTCODE);
        assertArrayEquals(new String[] {LINE_1, LINE_2, LINE_3, POSTCODE}, address.lines());
    }

    @Test
    public void linesWith2LinesAndPostCode() {
        DwpAddress address = new DwpAddress(LINE_1, LINE_2, POSTCODE);
        assertArrayEquals(new String[] {LINE_1, LINE_2, POSTCODE}, address.lines());
    }

    @Test
    @Parameters({" ", ""})
    public void linesWith2LinesAndPostCodeWithOneLineBlank(final String line2) {
        DwpAddress address = new DwpAddress(LINE_1, line2, POSTCODE);
        assertArrayEquals(new String[] {LINE_1, POSTCODE}, address.lines());
    }

    @Test
    public void linesWith2LinesAndPostCodeWithOneLineNull() {
        DwpAddress address = new DwpAddress(LINE_1, null, POSTCODE);
        assertArrayEquals(new String[] {LINE_1, POSTCODE}, address.lines());
    }
}
