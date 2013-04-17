/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.api;

import java.net.URI;
import java.util.Date;
import java.util.Set;



/**
 * 
 */
public interface SchemaContext {

    Set<DataSchemaNode> getDataDefinitions();

    Set<Module> getModules();

    Set<NotificationDefinition> getNotifications();

    Set<RpcDefinition> getOperations();
    
    Set<ExtensionDefinition> getExtensions();
    
    Module findModuleByName(final String name, final Date revision);
    
    Module findModuleByNamespace(final URI namespace);
}
