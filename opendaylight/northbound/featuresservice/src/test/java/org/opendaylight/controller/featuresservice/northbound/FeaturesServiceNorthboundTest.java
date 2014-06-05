
package org.opendaylight.controller.featuresservice.northbound;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;

public class FeaturesServiceNorthboundTest {
    private FeaturesService featuresService;

    @Test
    public void testList() throws Exception {
        featuresService = mock(FeaturesService.class);
        Feature[] features = new Feature[1];
        when(featuresService.listFeatures()).thenReturn(features);
        when(featuresService.listInstalledFeatures()).thenReturn(features);
        Feature ftest = mock(Feature.class);
        when(ftest.getName()).thenReturn("test");
        when(ftest.getId()).thenReturn("1234");
        when(ftest.getDescription()).thenReturn("test desc");
        when(ftest.getDetails()).thenReturn("test details");
        when(ftest.getVersion()).thenReturn("test version");
        when(ftest.hasVersion()).thenReturn(true);
        when(ftest.getResolver()).thenReturn("test resorver");
        features[0] = ftest;

        FeaturesServiceNorthbound fsNorthbound = new FeaturesServiceNorthbound(featuresService);
        List<FeatureJson> featuresJson = fsNorthbound.listFeatures();

        int i = 0;
        for(FeatureJson featureJson : featuresJson) {
            if(featureJson.name != features[i].getName()) {
                Assert.fail("Json feature name does not match feature name: \n"
                        +featureJson.name
                        + " =/= "+features[i].getName());
            }
            i++;
        }
    }

    @Test
    public void testInstalled() throws Exception{
        featuresService = mock(FeaturesService.class);
        Feature[] features = new Feature[1];
        when(featuresService.listFeatures()).thenReturn(features);
        when(featuresService.listInstalledFeatures()).thenReturn(features);
        Feature ftest = mock(Feature.class);
        when(ftest.getName()).thenReturn("test");
        when(ftest.getId()).thenReturn("1234");
        when(ftest.getDescription()).thenReturn("test desc");
        when(ftest.getDetails()).thenReturn("test details");
        when(ftest.getVersion()).thenReturn("test version");
        when(ftest.hasVersion()).thenReturn(true);
        when(ftest.getResolver()).thenReturn("test resorver");
        features[0] = ftest;

        FeaturesServiceNorthbound fsNorthbound = new FeaturesServiceNorthbound(featuresService);
        List<FeatureJson> featuresJson = fsNorthbound.installedFeatures();

        int i = 0;
        for(FeatureJson featureJson : featuresJson) {
            if(featureJson.name != features[i].getName()) {
                Assert.fail("Json feature name does not match feature name: \n"
                        +featureJson.name
                        + " =/= "+features[i].getName());
            }
            i++;
        }
    }

    @Test
    public void testInstall() throws Exception {
        featuresService = mock(FeaturesService.class);
        doNothing().when(featuresService).installFeature("odl-test");

        FeaturesServiceNorthbound fsNorthbound = new FeaturesServiceNorthbound(featuresService);
        try {
            fsNorthbound.installFeature(new FeatureName("odl-test"));
        } catch(RuntimeException e) {
            Mockito.verify(featuresService).installFeature(Matchers.eq("odl-test"));
        }
    }

    @Test
    public void testUninstall() throws Exception {
        featuresService = mock(FeaturesService.class);
        doNothing().when(featuresService).uninstallFeature("odl-test");

        FeaturesServiceNorthbound fsNorthbound = new FeaturesServiceNorthbound(featuresService);
        try {
            fsNorthbound.uninstallFeature(new FeatureName("odl-test"));
        } catch(RuntimeException e) {
            Mockito.verify(featuresService).uninstallFeature(Matchers.eq("odl-test"));
        }
    }
}
