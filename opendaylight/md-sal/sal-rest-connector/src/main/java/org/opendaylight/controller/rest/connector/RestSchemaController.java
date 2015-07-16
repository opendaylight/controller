/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.rest.connector;

import java.net.URI;
import java.util.Set;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.rest.common.InstanceIdentifierContext;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;

public interface RestSchemaController extends SchemaContextListener, AutoCloseable {

    void setGlobalSchema(@Nonnull SchemaContext globalSchema);

    void setMountService(@Nonnull DOMMountPointService mountService);

    InstanceIdentifierContext<?> toInstanceIdentifier(String uriPath);

    boolean isNodeMixin(YangInstanceIdentifier path);

    YangInstanceIdentifier toXpathRepresentation(YangInstanceIdentifier path);

    Module findModuleByNamespace(DOMMountPoint mountPoint, URI validNamespace);

    Module findModuleByNamespace(URI validNamespace);

    URI findNamespaceByModuleName(DOMMountPoint mountPoint, String namespace);

    URI findNamespaceByModuleName(String namespace);

    DataSchemaNode getRestconfModuleErrorsSchemaNode();

    SchemaContext getGlobalSchema();

    String toFullRestconfIdentifier(YangInstanceIdentifier path, DOMMountPoint mount);

    Set<Module> getAllModules();

    Set<Module> getAllModules(DOMMountPoint mountPoint);

    Module getRestconfModule();

    InstanceIdentifierContext<?> toMountPointIdentifier(String restconfInstance);

    String urlPathArgDecode(String pathArg);

    RpcDefinition getRpcDefinition(String name);

    DataSchemaNode getRestconfModuleRestConfSchemaNode(Module inRestconfModule, String schemaNodeName);

    Module findModuleByNameAndRevision(DOMMountPoint mountPoint, QName module);

    Module findModuleByNameAndRevision(QName module);
}
