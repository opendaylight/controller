package org.opendaylight.controller.config;

import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.configureConsole;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.logLevel;

import java.io.File;
//import java.lang.System;
import java.net.URI;
import java.util.EnumSet;

import javax.inject.Inject;

import junit.framework.Assert;

import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Repository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.LogLevelOption.LogLevel;


@RunWith(PaxExam.class)
public class ConfigFeatureTest {
   @Inject
   private FeaturesService featuresService;
    @Configuration
    public Option[] config() {
       return new Option[] {
             // Provision and launch a container based on a distribution of Karaf (Apache ServiceMix).
             karafDistributionConfiguration()
                 .frameworkUrl(
                          maven()
                                  .groupId("org.opendaylight.controller")
                                  .artifactId("distribution.opendaylight-karaf")
                                  .type("tar.gz")
                                  .version("1.4.2-SNAPSHOT"))
                 .name("OpenDaylight")
                 .unpackDirectory(new File("target/pax"))
                 .useDeployFolder(false),
             // It is really nice if the container sticks around after the test so you can check the contents
             // of the data directory when things go wrong.
             keepRuntimeFolder(),
             // Don't bother with local console output as it just ends up cluttering the logs
             configureConsole().ignoreLocalConsole(),
             // Force the log level to INFO so we have more details during the test.  It defaults to WARN.
             logLevel(LogLevel.WARN),
             // Remember that the test executes in another process.  If you want to debug it, you need
             // to tell Pax Exam to launch that process with debugging enabled.  Launching the test class itself with
             // debugging enabled (for example in Eclipse) will not get you the desired results.
             //debugConfiguration("5000", true),

       };
    }

    @Test
    public void testConfigAPI() throws Exception {
      featuresService.removeRepository(new URI("mvn:org.opendaylight.controller/config-features/0.2.5-SNAPSHOT/xml/features"));
      URI myURI = new URI("file:/Users/vmithbao/controllerLatest/features/config/target/classes/features.xml");
      featuresService.addRepository(myURI);
      System.out.println(featuresService.listFeatures()); // this is just to add some delay, since after adding repo, imeediately its not available
       Repository repoUnderTest = featuresService.getRepository("odl-config-0.2.5-SNAPSHOT");
       Assert.assertNotNull(repoUnderTest);
       Feature[] featuresUnderTest = repoUnderTest.getFeatures();
       for(int i=0; i< featuresUnderTest.length; i++)
       {
          Feature feature = featuresUnderTest[i];
         if(feature.getName().equalsIgnoreCase("odl-config-api")){
          featuresService.installFeature(feature,EnumSet.of(FeaturesService.Option.Verbose));
          System.out.println("Testing Feature:"+feature.getName());
          Assert.assertTrue(featuresService.isInstalled(feature));
         }
       }
    }
  @Test
  public void testConfigNettyAPI() throws Exception {

    Repository repoUnderTest = featuresService.getRepository("odl-config-0.2.5-SNAPSHOT");
    Assert.assertNotNull(repoUnderTest);
    Feature[] featuresUnderTest = repoUnderTest.getFeatures();
    for(int i=0; i< featuresUnderTest.length; i++)
    {
      Feature feature = featuresUnderTest[i];
      if(feature.getName().equalsIgnoreCase("odl-config-netty-config-api")){
        featuresService.installFeature(feature,EnumSet.of(FeaturesService.Option.Verbose));
        System.out.println("Testing Feature:"+feature.getName());
        Assert.assertTrue(featuresService.isInstalled(feature));
      }
    }
  }
}