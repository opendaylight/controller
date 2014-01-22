package org.opendaylight.controller.sal.binding.dom.serializer.api;

import java.util.EventListener;

import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public interface SchemaContextListener extends EventListener {


    void onGlobalContextUpdated(SchemaContext context);

}
