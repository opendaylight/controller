/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.rest.impl;

import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.Set;
import javax.ws.rs.core.Application;
import org.opendaylight.controller.md.sal.rest.schema.SchemaExportContentYangBodyWriter;
import org.opendaylight.controller.md.sal.rest.schema.SchemaExportContentYinBodyWriter;
import org.opendaylight.controller.md.sal.rest.schema.SchemaRetrievalServiceImpl;
import org.opendaylight.controller.sal.restconf.impl.BrokerFacade;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.controller.sal.restconf.impl.RestconfImpl;
import org.opendaylight.controller.sal.restconf.impl.StatisticsRestconfServiceWrapper;

public class RestconfApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        return ImmutableSet.<Class<?>> builder()
                .add(RestconfDocumentedExceptionMapper.class)
                .add(XmlNormalizedNodeBodyReader.class)
                .add(JsonNormalizedNodeBodyReader.class)
                .add(NormalizedNodeJsonBodyWriter.class)
                .add(NormalizedNodeXmlBodyWriter.class)
                .add(SchemaExportContentYinBodyWriter.class)
                .add(SchemaExportContentYangBodyWriter.class)
                .build();
    }

    @Override
    public Set<Object> getSingletons() {
        final Set<Object> singletons = new HashSet<>();
        final ControllerContext controllerContext = ControllerContext.getInstance();
        final BrokerFacade brokerFacade = BrokerFacade.getInstance();
        final RestconfImpl restconfImpl = RestconfImpl.getInstance();
        final SchemaRetrievalServiceImpl schemaRetrieval = new SchemaRetrievalServiceImpl(controllerContext);
        restconfImpl.setBroker(brokerFacade);
        restconfImpl.setControllerContext(controllerContext);
        singletons.add(controllerContext);
        singletons.add(brokerFacade);
        singletons.add(schemaRetrieval);
        singletons.add(new RestconfCompositeWrapper(StatisticsRestconfServiceWrapper.getInstance(), schemaRetrieval));
        singletons.add(StructuredDataToXmlProvider.INSTANCE);
        singletons.add(StructuredDataToJsonProvider.INSTANCE);
        singletons.add(JsonToCompositeNodeProvider.INSTANCE);
        singletons.add(XmlToCompositeNodeProvider.INSTANCE);
        return singletons;
    }

}
