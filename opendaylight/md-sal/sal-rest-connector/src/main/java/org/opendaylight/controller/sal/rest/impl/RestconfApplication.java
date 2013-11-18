package org.opendaylight.controller.sal.rest.impl;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.opendaylight.controller.sal.restconf.impl.BrokerFacade;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.RestconfImpl;

public class RestconfApplication extends Application {

    @Override
    public Set<Object> getSingletons() {
        Set<Object> singletons = new HashSet<>();
        ControllerContext controllerContext = ControllerContext.getInstance();
        BrokerFacade brokerFacade = BrokerFacade.getInstance();
        RestconfImpl restconfImpl = RestconfImpl.getInstance();
        restconfImpl.setBroker(brokerFacade);
        restconfImpl.setControllerContext(controllerContext);
        singletons.add(controllerContext);
        singletons.add(brokerFacade);
        singletons.add(restconfImpl);
        singletons.add(XmlToCompositeNodeProvider.INSTANCE);
        singletons.add(StructuredDataToXmlProvider.INSTANCE);
        singletons.add(JsonToCompositeNodeProvider.INSTANCE);
        singletons.add(StructuredDataToJsonProvider.INSTANCE);
        return singletons;
    }

}
