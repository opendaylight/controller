package org.opendaylight.controller.networkconfig.neutron.implementation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import org.apache.felix.dm.Component;
import org.opendaylight.controller.clustering.services.CacheConfigException;
import org.opendaylight.controller.clustering.services.CacheExistException;
import org.opendaylight.controller.clustering.services.IClusterContainerServices;
import org.opendaylight.controller.clustering.services.IClusterServices;
import org.opendaylight.controller.networkconfig.neutron.INeutronCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronObject;
import org.slf4j.Logger;

public abstract class AbstractNeutronInterface<N extends INeutronObject>
        implements INeutronCRUD<N> {
    protected IClusterContainerServices clusterContainerService = null;
    protected ConcurrentMap<String, N> db;

    protected abstract String getCacheName();
    protected abstract Logger getLogger();

    // methods needed for creating caches

    void setClusterContainerService(IClusterContainerServices s) {
        getLogger().debug("Cluster Service set");
        clusterContainerService = s;
    }

    void unsetClusterContainerService(IClusterContainerServices s) {
        if (clusterContainerService == s) {
            getLogger().debug("Cluster Service removed!");
            clusterContainerService = null;
        }
    }

    private void startUp() {
        allocateCache();
        retrieveCache();
    }

    @SuppressWarnings("deprecation")
    private void allocateCache() {
        if (this.clusterContainerService == null) {
            getLogger().error("un-initialized clusterContainerService, can't create cache");
            return;
        }

        getLogger().debug("Creating Cache {}", getCacheName());
        try {
            this.clusterContainerService.createCache(getCacheName(),
                    EnumSet.of(IClusterServices.cacheMode.NON_TRANSACTIONAL));
        } catch (CacheConfigException cce) {
            getLogger().error("Cache {} couldn't be created - check cache mode", getCacheName());
        } catch (CacheExistException cce) {
            getLogger().error("Cache {} already exists, destroy and recreate", getCacheName());
        }
        getLogger().debug("Cache {} successfully created", getCacheName());
    }

    @SuppressWarnings({ "unchecked", "deprecation" })
    private void retrieveCache() {
        if (this.clusterContainerService == null) {
            getLogger().error("un-initialized clusterContainerService, can't retrieve cache");
            return;
        }

        getLogger().debug("Retrieving cache {}", getCacheName());
        db = (ConcurrentMap<String, N>) this.clusterContainerService.getCache(getCacheName());
        if (db == null) {
            getLogger().error("Cache {} couldn't be retrieved", getCacheName());
        }
        getLogger().debug("Cache {} successfully retrieved", getCacheName());
    }

    @SuppressWarnings("deprecation")
    private void destroyCache() {
        if (this.clusterContainerService == null) {
            getLogger().error("un-initialized clusterContainerService, can't destroy cache");
            return;
        }
        getLogger().debug("Destroying cache {}", getCacheName());
        this.clusterContainerService.destroyCache("neutronSecurityGroups");
    }

    /**
     * Function called by the dependency manager when all the required
     * dependencies are satisfied
     *
     */
    void init(Component c) {
        Dictionary<?, ?> props = c.getServiceProperties();
        if (props != null) {
            String containerName = (String) props.get("containerName");
            getLogger().debug("Running containerName: {}", containerName);
        }
        startUp();
    }

    /**
     * Function called by the dependency manager when at least one dependency
     * become unsatisfied or when the component is shutting down because for
     * example bundle is being stopped.
     *
     */
    void destroy() {
        destroyCache();
    }

    /**
     * Function called by dependency manager after "init ()" is called and after
     * the services provided by the class are registered in the service registry
     *
     */
    void start() {
    }

    /**
     * Function called by the dependency manager before the services exported by
     * the component are unregistered, this will be followed by a "destroy ()"
     * calls
     *
     */
    void stop() {
    }

    // this method uses reflection to update an object from it's delta.

    protected boolean overwrite(Object target, Object delta) {
        if (target == null)
            return false;

        Method[] methods = target.getClass().getMethods();

        for(Method toMethod: methods){
            if(toMethod.getDeclaringClass().equals(target.getClass())
                    && toMethod.getName().startsWith("set")){

                String toName = toMethod.getName();
                String fromName = toName.replace("set", "get");

                try {
                    Method fromMethod = delta.getClass().getMethod(fromName);
                    Object value = fromMethod.invoke(delta, (Object[])null);
                    if(value != null){
                        toMethod.invoke(target, value);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean exists(String uuid) {
        return db.containsKey(uuid);
    }

    @Override
    public N get(String uuid) {
        return db.get(uuid);
    }

    @Override
    public List<N> getAll() {
        getLogger().debug("Exiting getAll(), found {} objects", db.entrySet().size());
        return new ArrayList<>(db.values());
    }

    @Override
    public boolean add(N input) {
        return db.putIfAbsent(input.getID(), input) == null;
    }

    @Override
    public boolean remove(String uuid) {
        return db.remove(uuid) != null;
    }

    @Override
    public boolean update(String uuid, N delta) {
        return overwrite(get(uuid), delta);
    }
}
