package org.opendaylight.controller.sal.restconf.impl

import org.opendaylight.yangtools.yang.data.api.CompositeNode
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode

class JsonMapper {
    
    @Property
    ControllerContext controllerContext
    
    def convert(DataSchemaNode schema, CompositeNode data) {
        return ""
    }
    
}