/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package org.opendaylight.controller.md.sal.binding.api;

import com.google.common.annotations.Beta;
import org.opendaylight.controller.md.sal.common.api.data.DataValidationFailedException;


/**
 *
 * Data tree validator, which validates data tree modifications for validity,
 * with option to reject supplied modification.
 *
 * <h2>Implementation requirements</h2>
 *
 * <h3>Correctness assumptions</h3> Implementation SHOULD use only
 * {@link ModifiedDataRoot} for validation purposes.
 *
 * Use of any other external mutable state is discouraged, implementation MUST
 * NOT use any transaction related APIs during invocation of callbacks, except
 * ones provided as argument.
 *
 * Note that this may be enforced by some implementations of
 * {@link DataValidatorRegistry} and such calls may fail.
 *
 * <h3>Correct model usage</h3> If implementation is performing YANG-model
 * driven validation implementation SHOULD use provided schema context.
 *
 * Any other instance of {@link SchemaContext} obtained by other means, may not
 * be valid for associated DataTreeCandidate and it may lead to incorrect
 * validation of provided data.
 *
 * <h3>Modified Data Root assumptions</h3>
 *
 * Implementation SHOULD NOT make any assumptions on {@link ModifiedDataRoot}
 * being successfully committed, even if validate method finished correctly,
 * because other validation mechanism or other {@link DataValidator} may reject
 * proposed {@link ModifiedDataRoot}.
 *
 * FIXME: Provide a way to understand which effects were committed will allow
 * implementations to keep a pre-computed cache
 *
 * @author Tony Tkacik <ttkacik@cisco.com>
 *
 */
@Beta
public interface DataValidator {

    /**
     * Initial validation of pre-existing data tree.
     *
     * @param initialTx
     *            {@link ReadWriteTransaction}, which may be used to make
     *            provided existing data tree correct according to validation
     *            logic.
     *
     *            FIXME: Consider using different API as ReadWriteTransaction,
     *            which will require validator to specify message / reason, why
     *            such change was done.
     *
     * @param tree
     *            Preexisting Data tree displayed as {@link DataTreeCandidate},
     *            where each and every one is marked as added.
     *
     */
    void initialValidation(ReadWriteTransaction tx, ModifiedDataRoot tree);

    /**
     * Validates supplied data tree.
     *
     * If {@link DataValidationFailedException} is thrown by implementation,
     * commit of supplied data will be prevented, with the DataBroker transaction
     * providing the thrown exception as the cause of failure.
     *
     * Note this call is synchronous and it is executed during transaction
     * commit, so it affects performance characteristics of data broker.
     *
     * Implementation SHOULD do it processing fast, SHOULD NOT block and should
     * execute fast.
     *
     * Implementation SHOULD NOT access any transaction related APIs during
     * invocation of callback. Note that this may be enforced by some
     * implementations of {@link DataValidatorRegistry} and such calls may fail.
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
    // TODO: Introduce better exception
    void validate(ModifiedDataRoot candidate) throws DataValidationFailedException;

}
