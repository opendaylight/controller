package org.opendaylight.controller.sal.core.api.model;

import java.util.EventListener;

import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public interface SchemaServiceListener extends EventListener {

    
    void onGlobalContextUpdated(SchemaContext context);
    
}
