package org.opendaylight.controller.clustering.services_implementation.internal;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.ConcurrentMap;

import org.apache.felix.service.command.Descriptor;
import org.infinispan.AdvancedCache;
import org.infinispan.distribution.DistributionManager;
import org.opendaylight.controller.clustering.services.IClusterContainerServices;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClusterManagerCLI {
    protected static final Logger logger = LoggerFactory
            .getLogger(ClusterManagerCLI.class);
    @SuppressWarnings("rawtypes")
    private ServiceRegistration sr = null;

    public void init() {
    }

    public void destroy() {
    }

    public void start() {
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("osgi.command.scope", "odpcontroller");
        props.put("osgi.command.function", new String[] { "getContainerAdvancedCacheInfo" });
        this.sr = ServiceHelper.registerGlobalServiceWReg(ClusterManagerCLI.class, this, props);
    }

    public void stop() {
        if (this.sr != null) {
            this.sr.unregister();
            this.sr = null;
        }
    }

    @Descriptor("Get advanced cache infos")
    public void getContainerAdvancedCacheInfo(@Descriptor("Container for the cache to be fetched") String container,
            @Descriptor("cache to get information about") String cacheName) {
        IClusterContainerServices s =
                (IClusterContainerServices) ServiceHelper.getInstance(IClusterContainerServices.class, container, this);
        if (s == null) {
            logger.error("Could not get an handle to the container cluster service:", container);
            return;
        }
        if (!s.existCache(cacheName)) {
            logger.error("Could not get cache named:", cacheName);
        }
        ConcurrentMap<?, ?> aC = s.getCache(cacheName);
        if (aC == null) {
            logger.error("Could not get cache named:", cacheName);
            return;
        }
        if (aC instanceof AdvancedCache) {
            @SuppressWarnings("rawtypes")
            AdvancedCache advCache = (AdvancedCache) aC;
            logger.info("AdvancedCache retrieved!");
            DistributionManager dMgr = advCache.getDistributionManager();
            if (dMgr == null) {
                return;
            }
            logger.info("Routing Table for the Hash:", dMgr.getConsistentHash()
                    .getRoutingTableAsString());
            logger.info("Get Members:", dMgr.getConsistentHash()
                    .getMembers());
        }
    }
}
