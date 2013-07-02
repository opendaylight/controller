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

    /**
     * Returns feature statements defined in module.
     *
     * @return feature statements in lexicographical order
     */
    Set<FeatureDefinition> getFeatures();

    /**
     * Returns notification statements defined in module.
     *
     * @return notification statements in lexicographical order
     */
    Set<NotificationDefinition> getNotifications();

    /**
     * Returns augment statements defined in module.
     *
     * @return augment statements
     */
    Set<AugmentationSchema> getAugmentations();

    /**
     * Returns rpc statements defined in module.
     *
     * @return rpc statements in lexicographical order
     */
    Set<RpcDefinition> getRpcs();

    /**
     * Returns deviation statements defined in module.
     *
     * @return deviation statements
     */
    Set<Deviation> getDeviations();

    /**
     * Returns identity statements defined in module.
     *
     * @return identity statements in lexicographical order
     */
    Set<IdentitySchemaNode> getIdentities();

    /**
     * Returns extension statements defined in module.
     *
     * @return extension statements in lexicographical order
     */
    List<ExtensionDefinition> getExtensionSchemaNodes();

    /**
     * Returns unknown nodes defined in module.
     *
     * @return unknown nodes in lexicographical order
     */
    List<UnknownSchemaNode> getUnknownSchemaNodes();

}
