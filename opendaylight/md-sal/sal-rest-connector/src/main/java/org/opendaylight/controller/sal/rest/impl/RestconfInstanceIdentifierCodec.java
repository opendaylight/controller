package org.opendaylight.controller.sal.rest.impl;

import javax.ws.rs.ext.ParamConverter;

import org.opendaylight.controller.sal.restconf.impl.InstanceIdWithSchemaNode;


public interface RestconfInstanceIdentifierCodec extends ParamConverter<InstanceIdWithSchemaNode> {

    @Override
    public InstanceIdWithSchemaNode fromString(String value);

    @Override
    public String toString(InstanceIdWithSchemaNode value);
}
