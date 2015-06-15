package org.opendaylight.controller.sal.restconf.impl.test;

import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.opendaylight.controller.sal.restconf.impl.RestconfError;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorTag;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorType;

public class ErrorTypesTest {

    @Test
    public void errorType() {
        final ErrorType errType = RestconfError.ErrorType.valueOfCaseInsensitive("transport");
        assertTrue(errType.toString().equals("transport".toUpperCase()));
    }

    @Test
    public void errorTypeBad() {
        final ErrorType errType = RestconfError.ErrorType.valueOfCaseInsensitive("bad type");
        assertTrue(errType.equals(ErrorType.APPLICATION));
    }

    @Test(expected = NullPointerException.class)
    public void errorTypeNull() {
        RestconfError.ErrorType.valueOfCaseInsensitive(null);
    }

    @Test
    public void errorTag() {
        final ErrorTag errType = RestconfError.ErrorTag.valueOfCaseInsensitive("in_use");
        assertTrue(errType.toString().equals("in_use".toUpperCase()));
    }

    @Test
    public void errorTagBad() {
        final ErrorTag errType = RestconfError.ErrorTag.valueOfCaseInsensitive("bad tag");
        assertTrue(errType.equals(ErrorTag.OPERATION_FAILED));
    }

    @Test(expected = NullPointerException.class)
    public void errorTagNull() {
        RestconfError.ErrorTag.valueOfCaseInsensitive(null);
    }
}
