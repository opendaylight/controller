package org.opendaylight.controller.cluster.datastore.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import org.junit.Test;
import org.opendaylight.controller.cluster.raft.election.DefaultElectionStrategy;
import org.opendaylight.controller.cluster.raft.election.ElectionStrategy;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

public class ElectionStrategyFactoryTest {

    @Test
    public void testGetElectionStrategy() throws IOException {
        ElectionStrategy electionStrategy = ElectionStrategyFactory.getElectionStrategy();

        assertEquals(DefaultElectionStrategy.class, electionStrategy.getClass());

        BundleContext bundleContext = mock(BundleContext.class);

        ServiceReference serviceReference = mock(ServiceReference.class);
        ConfigurationAdmin configurationAdmin = mock(ConfigurationAdmin.class);
        Configuration configuration = mock(Configuration.class);

        doReturn(serviceReference).when(bundleContext).getServiceReference(ConfigurationAdmin.class);
        doReturn(configurationAdmin).when(bundleContext).getService(serviceReference);

        Dictionary<String, Object> leaders = new Hashtable<>();

        doReturn(configuration).when(configurationAdmin).getConfiguration(anyString());
        doReturn(leaders).when(configuration).getProperties();

        ElectionStrategyFactory.setBundleContext(bundleContext);

        ElectionStrategy fixedElectionStrategy = ElectionStrategyFactory.getElectionStrategy();

        assertEquals(FixedLeaderElectionStrategy.class, ElectionStrategyFactory.getElectionStrategy().getClass());

        doThrow(IOException.class).when(configurationAdmin).getConfiguration(anyString());

        ElectionStrategyFactory.setBundleContext(bundleContext);

        assertEquals(fixedElectionStrategy, ElectionStrategyFactory.getElectionStrategy());

        doReturn(configuration).when(configurationAdmin).getConfiguration(anyString());

        ElectionStrategyFactory.setBundleContext(bundleContext);

        assertNotEquals(fixedElectionStrategy, ElectionStrategyFactory.getElectionStrategy());
    }
}