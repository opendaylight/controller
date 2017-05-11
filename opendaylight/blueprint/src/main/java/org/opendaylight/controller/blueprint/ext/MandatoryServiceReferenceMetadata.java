/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.blueprint.ext;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.osgi.service.blueprint.reflect.ReferenceListener;
import org.osgi.service.blueprint.reflect.ServiceReferenceMetadata;

/**
 * A ServiceReferenceMetadata implementation for a mandatory OSGi service.
 *
 * @author Thomas Pantelis
 */
class MandatoryServiceReferenceMetadata implements ServiceReferenceMetadata {
    private final String interfaceClass;
    private final String id;

    MandatoryServiceReferenceMetadata(final String id, final String interfaceClass) {
        this.id = Preconditions.checkNotNull(id);
        this.interfaceClass = interfaceClass;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public int getActivation() {
        return ACTIVATION_EAGER;
    }

    @Override
    public List<String> getDependsOn() {
        return Collections.emptyList();
    }

    @Override
    public int getAvailability() {
        return AVAILABILITY_MANDATORY;
    }

    @Override
    public String getInterface() {
        return interfaceClass;
    }

    @Override
    public String getComponentName() {
        return null;
    }

    @Override
    public String getFilter() {
        return ComponentProcessor.DEFAULT_TYPE_FILTER;
    }

    @Override
    public Collection<ReferenceListener> getReferenceListeners() {
        return Collections.emptyList();
    }
}
