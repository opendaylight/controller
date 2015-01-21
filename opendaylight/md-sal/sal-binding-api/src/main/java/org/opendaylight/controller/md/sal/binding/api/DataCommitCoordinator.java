/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 */
package org.opendaylight.controller.md.sal.binding.api;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
/**
 *
 * Registry for user-supplied {@link DataCommitCohort} implementations.
 *
 * {@link DataCommitCoordinator} is used to register {@link DataCommitCohort} for
 * customising validation of submitted data in system.
 *
 */
public interface DataCommitCoordinator {

    /**
     * Registers data validator for supplied {@link LogicalDatastoreType}.
     *
     * During registration {@link DataCommitCohort#initialValidation(ReadWriteTransaction, ModifiedDataRoot)}
     * method will be called, which allows validator to perform initial validation of preexisting data,
     * and some cleanup to remove data which did not passed validation.
     *
     * Registered data validator will be invoked during commit for every transaction
     * affecting supplied logical data store. See {@link DataCommitCohort} for more details.
     *
     * @param validator
     * @return Registration object for supplied validator. Invoking {@link ObjectRegistration#close()}
     * method will unregister validator.
     */
    <T extends DataCommitCohort> ObjectRegistration<T> registerDataValidator(LogicalDatastoreType store, T cohort);
}
