package org.opendaylight.controller.sal.binding.dom.serializer.api;

import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import java.util.EventListener;

public interface SchemaServiceListener extends EventListener {

    void onGlobalContextUpdated(SchemaContext context);

}
