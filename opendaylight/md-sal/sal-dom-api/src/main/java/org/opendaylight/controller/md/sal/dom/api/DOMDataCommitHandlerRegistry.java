/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.api;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;

/**
 *
 * Commit Cohort registry for {@link DOMDataReadWriteTransaction} and {@link DOMDataWriteTransaction}.
 *
 * See {@link DOMDataCommitCohort} and {@link DOMDataTreeValidator} for more details.
 *
 * @author Tony Tkacik <ttkacik@cisco.com>
 *
 */
public interface DOMDataCommitHandlerRegistry extends DOMDataBrokerExtension {

    /**
     * Register commit cohort which will participate in three-phase commit protocols
     * of {@link DOMDataReadWriteTransaction} and {@link DOMDataWriteTransaction}
     * in data broker associated with this instance of extension.
     *
     * @param store Logical Data Store type
     * @param cohort Commit cohort
     * @return
     */
    <T extends DOMDataCommitCohort> DOMDataCommitCohortRegistration<T> registerCommitCohort(LogicalDatastoreType store,DOMDataCommitCohort cohort);
}
