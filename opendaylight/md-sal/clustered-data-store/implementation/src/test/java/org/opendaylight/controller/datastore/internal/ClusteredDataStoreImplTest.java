package org.opendaylight.controller.datastore.internal;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.clustering.services.CacheConfigException;
import org.opendaylight.controller.clustering.services.CacheExistException;
import org.opendaylight.controller.clustering.services.IClusterGlobalServices;
import org.opendaylight.controller.clustering.services.IClusterServices;
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler;
import org.opendaylight.controller.md.sal.common.api.data.DataModification;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ClusteredDataStoreImplTest {
    @Before
    public void setUp(){

    }

    @Test
    public void constructor_WhenPassedANullClusteringServices_ShouldThrowANullPointerException() throws CacheExistException, CacheConfigException {
        try {
            new ClusteredDataStoreImpl(null);
        } catch(NullPointerException npe){
            assertEquals("clusterGlobalServices cannot be null", npe.getMessage());
        }
    }

    @Test
    public void constructor_WhenClusteringServicesReturnsANullOperationalDataCache_ShouldThrowANullPointerException() throws CacheExistException, CacheConfigException {
        try {
            new ClusteredDataStoreImpl(mock(IClusterGlobalServices.class));
        } catch(NullPointerException npe){
            assertEquals("operationalDataCache cannot be null", npe.getMessage());
        }
    }

    @Test
    public void constructor_WhenClusteringServicesReturnsANullOConfigurationDataCache_ShouldThrowANullPointerException() throws CacheExistException, CacheConfigException {
        IClusterGlobalServices mockClusterGlobalServices = mock(IClusterGlobalServices.class);

        // Confused about the following line?
        // See this http://stackoverflow.com/questions/10952629/a-strange-generics-edge-case-with-mockito-when-and-generic-type-inference
        Mockito.<ConcurrentMap<?,?>>when(mockClusterGlobalServices.createCache(ClusteredDataStoreImpl.OPERATIONAL_DATA_CACHE, EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL))).thenReturn(new ConcurrentHashMap<Object, Object>());


        try {
            new ClusteredDataStoreImpl(mockClusterGlobalServices);
        } catch(NullPointerException npe){
            assertEquals("configurationDataCache cannot be null", npe.getMessage());
        }
    }

    @Test
    public void constructor_WhenOperationalDataCacheIsAlreadyPresent_ShouldNotAttemptToCreateCache() throws CacheExistException, CacheConfigException {
        IClusterGlobalServices mockClusterGlobalServices = mock(IClusterGlobalServices.class);

        Mockito.<ConcurrentMap<?,?>>when(mockClusterGlobalServices.getCache(ClusteredDataStoreImpl.OPERATIONAL_DATA_CACHE)).thenReturn(new ConcurrentHashMap<Object, Object>());
        Mockito.<ConcurrentMap<?,?>>when(mockClusterGlobalServices.getCache(ClusteredDataStoreImpl.CONFIGURATION_DATA_CACHE)).thenReturn(new ConcurrentHashMap<Object, Object>());

        new ClusteredDataStoreImpl(mockClusterGlobalServices);

        verify(mockClusterGlobalServices, never()).createCache(ClusteredDataStoreImpl.OPERATIONAL_DATA_CACHE, EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));
    }

    @Test
    public void constructor_WhenConfigurationDataCacheIsAlreadyPresent_ShouldNotAttemptToCreateCache() throws CacheExistException, CacheConfigException {
        IClusterGlobalServices mockClusterGlobalServices = mock(IClusterGlobalServices.class);

        Mockito.<ConcurrentMap<?,?>>when(mockClusterGlobalServices.getCache(ClusteredDataStoreImpl.OPERATIONAL_DATA_CACHE)).thenReturn(new ConcurrentHashMap<Object, Object>());
        Mockito.<ConcurrentMap<?,?>>when(mockClusterGlobalServices.getCache(ClusteredDataStoreImpl.CONFIGURATION_DATA_CACHE)).thenReturn(new ConcurrentHashMap<Object, Object>());

        new ClusteredDataStoreImpl(mockClusterGlobalServices);

        verify(mockClusterGlobalServices, never()).createCache(ClusteredDataStoreImpl.CONFIGURATION_DATA_CACHE, EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));
    }


    @Test
    public void constructor_WhenPassedAValidClusteringServices_ShouldNotThrowAnyExceptions() throws CacheExistException, CacheConfigException {
        IClusterGlobalServices mockClusterGlobalServices = createClusterGlobalServices();

        new ClusteredDataStoreImpl(mockClusterGlobalServices);
    }


    @Test
    public void readOperationalData_WhenPassedANullPath_ShouldThrowANullPointerException() throws CacheExistException, CacheConfigException {
        IClusterGlobalServices mockClusterGlobalServices = createClusterGlobalServices();

        ClusteredDataStoreImpl store = new ClusteredDataStoreImpl(mockClusterGlobalServices);

        try {
            store.readOperationalData(null);
        } catch(NullPointerException npe){
            assertEquals("path cannot be null", npe.getMessage());
        }
    }

    @Test
    public void readOperationalData_WhenPassedAKeyThatDoesNotExistInTheCache_ShouldReturnNull() throws CacheExistException, CacheConfigException {
        InstanceIdentifier path = InstanceIdentifier.builder().toInstance();

        IClusterGlobalServices mockClusterGlobalServices = createClusterGlobalServices();

        ClusteredDataStoreImpl store = new ClusteredDataStoreImpl(mockClusterGlobalServices);

        assertNull(store.readOperationalData(path));
    }

    @Test
    public void readOperationalData_WhenPassedAKeyThatDoesExistInTheCache_ShouldReturnTheValueObject() throws CacheExistException, CacheConfigException {
        InstanceIdentifier path = InstanceIdentifier.builder().toInstance();

        IClusterGlobalServices mockClusterGlobalServices = createClusterGlobalServices();

        ConcurrentMap mockOperationalDataCache = mock(ConcurrentMap.class);

        Object valueObject = mock(Object.class);

        when(mockOperationalDataCache.get(path)).thenReturn(valueObject);

        Mockito.<ConcurrentMap<?,?>>when(mockClusterGlobalServices.createCache(ClusteredDataStoreImpl.OPERATIONAL_DATA_CACHE, EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL))).thenReturn(mockOperationalDataCache);
        Mockito.<ConcurrentMap<?,?>>when(mockClusterGlobalServices.createCache(ClusteredDataStoreImpl.CONFIGURATION_DATA_CACHE, EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL))).thenReturn(new ConcurrentHashMap<Object, Object>());


        ClusteredDataStoreImpl store = new ClusteredDataStoreImpl(mockClusterGlobalServices);

        assertEquals(valueObject, store.readOperationalData(path));
    }



    @Test
    public void readConfigurationData_WhenPassedANullPath_ShouldThrowANullPointerException() throws CacheExistException, CacheConfigException {

        IClusterGlobalServices mockClusterGlobalServices = createClusterGlobalServices();

        ClusteredDataStoreImpl store = new ClusteredDataStoreImpl(mockClusterGlobalServices);

        try {
            store.readConfigurationData(null);
        } catch(NullPointerException npe){
            assertEquals("path cannot be null", npe.getMessage());
        }
    }


    @Test
    public void readConfigurationData_WhenPassedAKeyThatDoesNotExistInTheCache_ShouldReturnNull() throws CacheExistException, CacheConfigException {
        InstanceIdentifier path = InstanceIdentifier.builder().toInstance();

        IClusterGlobalServices mockClusterGlobalServices = createClusterGlobalServices();

        ClusteredDataStoreImpl store = new ClusteredDataStoreImpl(mockClusterGlobalServices);

        assertNull(store.readConfigurationData(path));
    }

    @Test
    public void readConfigurationData_WhenPassedAKeyThatDoesExistInTheCache_ShouldReturnTheValueObject() throws CacheExistException, CacheConfigException {
        InstanceIdentifier path = InstanceIdentifier.builder().toInstance();

        IClusterGlobalServices mockClusterGlobalServices = createClusterGlobalServices();

        ConcurrentMap mockConfigurationDataCache = mock(ConcurrentMap.class);

        Object valueObject = mock(Object.class);

        when(mockConfigurationDataCache.get(path)).thenReturn(valueObject);

        Mockito.<ConcurrentMap<?,?>>when(mockClusterGlobalServices.createCache(ClusteredDataStoreImpl.OPERATIONAL_DATA_CACHE, EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL))).thenReturn(mock(ConcurrentMap.class));
        Mockito.<ConcurrentMap<?,?>>when(mockClusterGlobalServices.createCache(ClusteredDataStoreImpl.CONFIGURATION_DATA_CACHE, EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL))).thenReturn(mockConfigurationDataCache);


        ClusteredDataStoreImpl store = new ClusteredDataStoreImpl(mockClusterGlobalServices);

        assertEquals(valueObject, store.readConfigurationData(path));
    }


    @Test
    public void requestCommit_ShouldReturnADataTransaction() throws CacheExistException, CacheConfigException {
        IClusterGlobalServices mockClusterGlobalServices = createClusterGlobalServices();

        ClusteredDataStoreImpl store = new ClusteredDataStoreImpl(mockClusterGlobalServices);

        assertNotNull(store.requestCommit(mock(DataModification.class)));


    }

    @Test
    public void finishingADataTransaction_ShouldUpdateTheUnderlyingCache() throws CacheExistException, CacheConfigException {
        IClusterGlobalServices mockClusterGlobalServices = mock(IClusterGlobalServices.class);

        ConcurrentMap mockConfigurationDataCache = mock(ConcurrentMap.class);
        ConcurrentMap mockOperationalDataCache = mock(ConcurrentMap.class);

        Mockito.<ConcurrentMap<?,?>>when(mockClusterGlobalServices.createCache(ClusteredDataStoreImpl.OPERATIONAL_DATA_CACHE, EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL))).thenReturn(mockOperationalDataCache);
        Mockito.<ConcurrentMap<?,?>>when(mockClusterGlobalServices.createCache(ClusteredDataStoreImpl.CONFIGURATION_DATA_CACHE, EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL))).thenReturn(mockConfigurationDataCache);

        ClusteredDataStoreImpl store = new ClusteredDataStoreImpl(mockClusterGlobalServices);

        DataModification mockModification = mock(DataModification.class);

        Map configurationData = mock(Map.class);
        Map operationalData = mock(Map.class);

        when(mockModification.getUpdatedConfigurationData()).thenReturn(configurationData);
        when(mockModification.getUpdatedOperationalData()).thenReturn(operationalData);

        DataCommitHandler.DataCommitTransaction<InstanceIdentifier<? extends Object>, Object> transaction = store.requestCommit(mockModification);

        transaction.finish();

        verify(mockConfigurationDataCache).putAll(mockModification.getUpdatedConfigurationData());
        verify(mockOperationalDataCache).putAll(mockModification.getUpdatedOperationalData());
    }


    @Test
    public void rollingBackADataTransaction_ShouldDoNothing() throws CacheExistException, CacheConfigException {
        IClusterGlobalServices mockClusterGlobalServices = mock(IClusterGlobalServices.class);

        ConcurrentMap mockConfigurationDataCache = mock(ConcurrentMap.class);
        ConcurrentMap mockOperationalDataCache = mock(ConcurrentMap.class);

        Mockito.<ConcurrentMap<?,?>>when(mockClusterGlobalServices.createCache(ClusteredDataStoreImpl.OPERATIONAL_DATA_CACHE, EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL))).thenReturn(mockOperationalDataCache);
        Mockito.<ConcurrentMap<?,?>>when(mockClusterGlobalServices.createCache(ClusteredDataStoreImpl.CONFIGURATION_DATA_CACHE, EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL))).thenReturn(mockConfigurationDataCache);

        ClusteredDataStoreImpl store = new ClusteredDataStoreImpl(mockClusterGlobalServices);

        DataModification mockModification = mock(DataModification.class);

        Map configurationData = mock(Map.class);
        Map operationalData = mock(Map.class);

        when(mockModification.getUpdatedConfigurationData()).thenReturn(configurationData);
        when(mockModification.getUpdatedOperationalData()).thenReturn(operationalData);

        DataCommitHandler.DataCommitTransaction<InstanceIdentifier<? extends Object>, Object> transaction = store.requestCommit(mockModification);

        transaction.rollback();

        verify(mockConfigurationDataCache, never()).putAll(mockModification.getUpdatedConfigurationData());
        verify(mockOperationalDataCache, never()).putAll(mockModification.getUpdatedOperationalData());

    }


    private IClusterGlobalServices createClusterGlobalServices() throws CacheExistException, CacheConfigException {
        IClusterGlobalServices mockClusterGlobalServices = mock(IClusterGlobalServices.class);

        Mockito.<ConcurrentMap<?,?>>when(mockClusterGlobalServices.createCache(ClusteredDataStoreImpl.OPERATIONAL_DATA_CACHE, EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL))).thenReturn(mock(ConcurrentMap.class));
        Mockito.<ConcurrentMap<?,?>>when(mockClusterGlobalServices.createCache(ClusteredDataStoreImpl.CONFIGURATION_DATA_CACHE, EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL))).thenReturn(mock(ConcurrentMap.class));

        return mockClusterGlobalServices;
    }
}
