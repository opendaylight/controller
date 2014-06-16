/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.rest.doc.jaxrs;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import org.opendaylight.controller.sal.rest.doc.swagger.ApiDeclaration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;

@Provider
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class JaxbContextResolver implements ContextResolver<ObjectMapper> {

    private final ObjectMapper ctx;

    public JaxbContextResolver() {
        ctx = new ObjectMapper();
        ctx.registerModule(new JsonOrgModule());
        ctx.getSerializationConfig().withSerializationInclusion(JsonInclude.Include.ALWAYS);
    }

    @Override
    public ObjectMapper getContext(Class<?> aClass) {

        if (ApiDeclaration.class.isAssignableFrom(aClass)) {
            return ctx;
        }

        return null;// must return null so that jax-rs can continue context
                    // search
    }
}
