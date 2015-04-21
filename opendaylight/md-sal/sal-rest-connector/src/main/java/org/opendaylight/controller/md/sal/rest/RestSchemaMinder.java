/**
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.rest;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.sal.restconf.impl.InstanceIdentifierContext;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * sal-rest-connector
 * org.opendaylight.controller.md.sal.rest
 *
 * Internal interface contains all needed {@link SchemaContext} methods which are
 * referenced by {@link org.opendaylight.controller.sal.rest.api.Draft02} and which
 * are used in REST services.
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 * Created: Mar 22, 2015
 */
public interface RestSchemaMinder {

    void tell(@Nonnull final SchemaContext schemaContext);

    /**
     * Method returns RpcDefinition for Rpc LocalName with moduleName.
     * So input name has to have next form module:rpc-name.
     * Module name is important for building correct QName.
     *
     * @param name
     * @return
     */
    RpcDefinition getRpcDefinition(@CheckForNull String name);

    DOMMountPoint parseUriRequestToMountPoint(String requestUriIdentifier);

    InstanceIdentifierContext<?> parseUriRequest(String requestUriIdentifier);

    Module getRestconfModule();

    ListSchemaNode getModuleListSchemaNode();

    ContainerSchemaNode getModuleContainerSchemaNode();

    ListSchemaNode getStreamListSchemaNode();

    ContainerSchemaNode getStreamContainerSchemaNode();

}