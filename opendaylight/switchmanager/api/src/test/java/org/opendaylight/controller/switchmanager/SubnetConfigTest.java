package org.opendaylight.controller.switchmanager;

import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.sal.utils.Status;

public class SubnetConfigTest {

    @Test
    public void configuration() {
        List<String> portList = new ArrayList<String>();
        portList.add("OF|1@OF|1");
        portList.add("OF|2@OF|00:00:00:00:00:00:00:02");
        portList.add("OF|3@OF|00:00:00:00:00:00:00:01");

        // Full subnet creation
        SubnetConfig config = new SubnetConfig("eng", "11.1.1.254/16", portList);
        Status status = config.validate();
        Assert.assertTrue(status.isSuccess());

        // No port set specified
        config = new SubnetConfig("eng", "11.1.1.254/16", null);
        status = config.validate();
        Assert.assertTrue(status.isSuccess());

        // Empty port set
        config = new SubnetConfig("eng", "11.1.1.254/16", new ArrayList<String>(0));
        status = config.validate();
        Assert.assertTrue(status.isSuccess());

        // Zero subnet
        config = new SubnetConfig("eng", "1.2.3.254/1", null);
        status = config.validate();
        Assert.assertFalse(status.isSuccess());

        // Port set with invalid port notation
        List<String> badPortList = new ArrayList<String>();
        badPortList.add("1/1");
        config = new SubnetConfig("eng", "1.2.3.254/1", badPortList);
        status = config.validate();
        Assert.assertFalse(status.isSuccess());
    }
}
