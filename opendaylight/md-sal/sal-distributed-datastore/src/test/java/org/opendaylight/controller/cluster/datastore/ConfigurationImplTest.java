package org.opendaylight.controller.cluster.datastore;

import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertTrue;

public class ConfigurationImplTest {

    private static ConfigurationImpl configuration;

    @BeforeClass
    public static void staticSetup(){
        configuration = new ConfigurationImpl("module-shards.conf", "modules.conf");
    }

    @Test
    public void testConstructor(){
        Assert.assertNotNull(configuration);
    }

    @Test
    public void testGetMemberShardNames(){
        List<String> memberShardNames =
            configuration.getMemberShardNames("member-1");

        assertTrue(memberShardNames.contains("people-1"));
        assertTrue(memberShardNames.contains("cars-1"));
    }
}
