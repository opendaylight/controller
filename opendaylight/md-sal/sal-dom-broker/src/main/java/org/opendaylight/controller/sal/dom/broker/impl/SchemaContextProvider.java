package org.opendaylight.controller.sal.dom.broker.impl;

import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import com.google.common.base.Optional;

public interface SchemaContextProvider {

    SchemaContext getSchemaContext();
    
}
