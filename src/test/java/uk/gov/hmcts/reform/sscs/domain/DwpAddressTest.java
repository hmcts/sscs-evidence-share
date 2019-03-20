package uk.gov.hmcts.reform.sscs.domain;

import static org.junit.Assert.assertArrayEquals;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(JUnitParamsRunner.class)
public class DwpAddressTest {

    @Test
    public void linesWith3LinesAndPostCode() {
        DwpAddress address = new DwpAddress("line1", "line2", "line3", "postcode");
        assertArrayEquals(new String[] {"line1", "line2", "line3", "postcode"}, address.lines());
    }

    @Test
    public void linesWith2LinesAndPostCode() {
        DwpAddress address = new DwpAddress("line1", "line2", "postcode");
        assertArrayEquals(new String[] {"line1", "line2", "postcode"}, address.lines());
    }

    @Test
    @Parameters({" ", ""})
    public void linesWith2LinesAndPostCodeWithOneLineBlank(final String line2) {
        DwpAddress address = new DwpAddress("line1", line2, "postcode");
        assertArrayEquals(new String[] {"line1", "postcode"}, address.lines());
    }

    @Test
    public void linesWith2LinesAndPostCodeWithOneLineNull() {
        DwpAddress address = new DwpAddress("line1", null, "postcode");
        assertArrayEquals(new String[] {"line1", "postcode"}, address.lines());
    }
}
