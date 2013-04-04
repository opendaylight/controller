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
import java.util.List;
import java.util.Set;

public interface Module extends DataNodeContainer {

    URI getNamespace();

    String getName();

    Date getRevision();

    String getPrefix();

    String getYangVersion();

    String getDescription();

    String getReference();

    String getOrganization();

    String getContact();

    Set<ModuleImport> getImports();

    Set<FeatureDefinition> getFeatures();

    Set<NotificationDefinition> getNotifications();

    Set<AugmentationSchema> getAugmentations();

    Set<RpcDefinition> getRpcs();

    Set<Deviation> getDeviations();

    Set<IdentitySchemaNode> getIdentities();

    List<ExtensionDefinition> getExtensionSchemaNodes();

}
