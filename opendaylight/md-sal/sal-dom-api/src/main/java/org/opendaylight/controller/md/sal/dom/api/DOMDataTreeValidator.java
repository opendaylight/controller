/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package org.opendaylight.controller.md.sal.dom.api;

import org.opendaylight.controller.md.sal.common.api.data.DataValidationFailedException;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;


/**
 *
 * Data tree validator, which validates data tree modifications
 * for validity, with option to reject supplied modification.
 *
 * <h2>Implementation requirements</h2>
 *
 * <h3>Correctness assumptions</h3>
 * Implementation SHOULD use only {@link DataTreeCandidate} and provided
 * {@link SchemaContext} for validation purposes.
 *
 * Use of any other external mutable state is discouraged, implementation
 * MUST NOT use any transaction related APIs during invocation
 * of callbacks, except ones provided as argument.
 *
 * Note that this may be enforced by some implementations of
 * {@link DOMDataTreeValidatorRegistry} and such calls may fail.
 *
 * <h3>Correct model usage</h3>
 * If implementation is performing YANG-model driven validation
 * implementation SHOULD use provided schema context.
 *
 * Any other instance of
 * {@link SchemaContext} obtained by other means, may not be valid for
 * associated DataTreeCandidate and it may lead to incorrect validation of
 * provided data.
 *
 * <h3>DataTreeCandidate assumptions</h3>
 * Implementation SHOULD NOT make any assumptions on {@link DataTreeCandidate}
 * being successfully committed, even if validate method finished correctly,
 * because other validation mechanism or other {@link DOMDataTreeValidator} may
 * reject proposed {@link DataTreeCandidate}.
 *
 *
 * @author Tony Tkacik &lt;ttkacik@cisco.com&gt;
 *
 */
public abstract class DOMDataTreeValidator implements DOMDataCommitCohort {

    @Override
    public final CommitCohortTransaction canCommit(final Object txId, final DataTreeCandidate candidate, final SchemaContext ctx)
            throws DataValidationFailedException {
        validate(candidate, ctx);
        return COHORT_NOOP_TRANSACTION;
    }

    /**
     * Validates supplied data tree.
     *
     * If {@link DataValidationFailedException} is thrown by implementation,
     * commit of supplied data will be prevented, with the DataBroker transaction
     * providing the thrown exception as the cause of failure.
     *
     * Note this call is synchronous and it is executed during transaction commit,
     * so it affects performance characteristics of data broker.
     *
     * Implementation SHOULD do it processing fast, SHOULD NOT block.
     *
     * Implementation SHOULD NOT access any data transaction related APIs during
     * invocation of callback. Note that this may be enforced by some implementations
     * of {@link DOMDataCommitHandlerRegistry} and such calls may fail.
     *
     * @param tree
     *            Data Tree candidate to be validated
     * @param ctx
     *            Schema Context to which Data Tree candidate should conform.
     *
     * @throws DataValidationFailedException
     *             If and only if provided {@link DataTreeCandidate} did not
     *             pass validation. Users are encouraged to use more specific
     *             subclasses of this exception to provide additional data about
     *             validation failure reason.
     */
    public abstract void validate(DataTreeCandidate tree, SchemaContext ctx) throws DataValidationFailedException;
}
