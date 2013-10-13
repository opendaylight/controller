package org.opendaylight.controller.adapter.hosttracker;

import org.apache.felix.dm.Component;
import org.opendaylight.controller.hosttracker.IfIptoHost;
import org.opendaylight.controller.hosttracker.IfNewHostNotify;
import org.opendaylight.controller.sal.core.ComponentActivatorAbstractBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator extends ComponentActivatorAbstractBase {
    protected static final Logger logger = LoggerFactory
            .getLogger(Activator.class);
    
    /**
     * Function that is used to communicate to dependency manager the
     * list of known implementations for services inside a container
     *
     *
     * @return An array containing all the CLASS objects that will be
     * instantiated in order to get an fully working implementation
     * Object
     */
    public Object[] getImplementations() {
        Object[] res = { MdHostTrackerAdapter.class };
        return res;
    }
    
    public void configureInstance(Component c, Object imp, String containerName) {
        if (imp.equals(MdHostTrackerAdapter.class)) {
        	// export the service
            c.setInterface(new String[] { IfNewHostNotify.class.getName()}, null);
        	// the IfIptoHost is a required dependency
            c.add(createContainerServiceDependency(containerName).setService(
                    IfIptoHost.class).setCallbacks("setHostTracker",
                    "unsetHostTracker").setRequired(true));
        }
    }
}
