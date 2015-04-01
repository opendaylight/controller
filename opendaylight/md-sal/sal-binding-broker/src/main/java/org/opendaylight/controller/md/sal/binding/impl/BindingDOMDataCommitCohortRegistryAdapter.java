/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.md.sal.binding.api.DataCommitCohort;
import org.opendaylight.controller.md.sal.binding.api.DataCommitCohortRegistration;
import org.opendaylight.controller.md.sal.binding.api.DataCommitCoordinator;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataCommitCohort;
import org.opendaylight.controller.md.sal.dom.api.DOMDataCommitCohortRegistration;
import org.opendaylight.controller.md.sal.dom.api.DOMDataCommitHandlerRegistry;

public class BindingDOMDataCommitCohortRegistryAdapter implements DataCommitCoordinator{

    private final DOMDataCommitHandlerRegistry commitHandlerRegistry;
    private final BindingToNormalizedNodeCodec codec;

    private BindingDOMDataCommitCohortRegistryAdapter(final DOMDataCommitHandlerRegistry commitHandlerRegistry,
            final BindingToNormalizedNodeCodec codec) {
        this.commitHandlerRegistry = Preconditions.checkNotNull(commitHandlerRegistry);
        this.codec = Preconditions.checkNotNull(codec);
    }

    @Override
    public <T extends DataCommitCohort> DataCommitCohortRegistration<T> registerCommitCohort(
            final LogicalDatastoreType store, final T cohort) {
        final DOMDataCommitCohort domCohort = BindingDOMDataCommitCohortAdapter.create(store, cohort, codec);
        final DOMDataCommitCohortRegistration<DOMDataCommitCohort> domReg = commitHandlerRegistry.registerCommitCohort(store, domCohort);
        return new BindingDOMDataCommitCohortRegistrationAdapter<>(cohort,domReg);
    }

}
