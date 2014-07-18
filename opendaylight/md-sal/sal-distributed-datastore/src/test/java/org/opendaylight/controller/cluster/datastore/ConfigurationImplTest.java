package org.opendaylight.controller.cluster.datastore;

import com.typesafe.config.ConfigFactory;
import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
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

    @Test
    public void testReadConfigurationFromFile(){
        File f = new File("./module-shards.conf");
        ConfigFactory.parseFile(f);
    }
}
