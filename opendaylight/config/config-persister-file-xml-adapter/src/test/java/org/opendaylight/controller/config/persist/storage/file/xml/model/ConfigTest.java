package org.opendaylight.controller.config.persist.storage.file.xml.model;

import java.io.File;
import org.junit.Test;

public class ConfigTest {

    @Test(expected = IllegalArgumentException.class)
    public void testFromXml() throws Exception {
        Config.fromXml(new File(getClass().getResource("/illegalSnapshot.xml").getFile()));
    }
}