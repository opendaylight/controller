package org.opendaylight.controller.sal.restconf.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;

import org.opendaylight.controller.sal.rest.impl.RestconfInstanceIdentifierCodec;

@Provider
public class RestconfIdentifierCodecImpl implements ParamConverterProvider, RestconfInstanceIdentifierCodec, ContextResolver<RestconfInstanceIdentifierCodec> {


    private final ControllerContext controllerContext;

    public RestconfIdentifierCodecImpl(final ControllerContext controllerContext) {
        this.controllerContext = controllerContext;
    }

    @Override
    public <T> ParamConverter<T> getConverter(final Class<T> rawType, final Type genericType,
            final Annotation[] annotations) {
        // TODO Auto-generated method stub
        if (rawType.equals(InstanceIdWithSchemaNode.class)) {
            return (ParamConverter<T>) this;
        }
        return null;
    }

    @Override
    public InstanceIdWithSchemaNode fromString(final String value) {
        return controllerContext.toInstanceIdentifier(value);
    }

    @Override
    public String toString(final InstanceIdWithSchemaNode value) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RestconfIdentifierCodecImpl getContext(final Class<?> type) {
        if(type.equals(RestconfInstanceIdentifierCodec.class)) {
            return this;
        }
        return null;
    }
}
