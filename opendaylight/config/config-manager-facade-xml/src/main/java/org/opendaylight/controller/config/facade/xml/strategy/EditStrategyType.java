/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.facade.xml.strategy;

import java.util.EnumSet;
import java.util.Set;
import org.opendaylight.controller.config.facade.xml.exception.OperationNotPermittedException;
import org.opendaylight.controller.config.util.xml.DocumentedException;

public enum EditStrategyType {
    // can be default
    merge, replace, none,
    // additional per element
    delete, remove, recreate;

    private static final Set<EditStrategyType> defaultStrats = EnumSet.of(merge, replace, none);

    public static EditStrategyType getDefaultStrategy() {
        return merge;
    }

    public boolean isEnforcing() {
        switch (this) {
        case merge:
        case none:
        case remove:
        case delete:
        case recreate:
            return false;
        case replace:
            return true;

        default:
            throw new IllegalStateException("Default edit strategy can be only of value " + defaultStrats + " but was "
                    + this);
        }
    }
    public static void compareParsedStrategyToDefaultEnforcing(EditStrategyType parsedStrategy,
                                                                  EditStrategyType defaultStrategy) throws OperationNotPermittedException {
        if (defaultStrategy.isEnforcing()) {
            if (parsedStrategy != defaultStrategy){
                throw new OperationNotPermittedException(String.format("With "
                        + defaultStrategy
                        + " as default-operation operations on module elements are not permitted since the default option is restrictive"),
                        DocumentedException.ErrorType.APPLICATION,
                        DocumentedException.ErrorTag.OPERATION_FAILED,
                        DocumentedException.ErrorSeverity.ERROR);
            }
        }

    }
    public EditConfigStrategy getFittingStrategy() {
        switch (this) {
        case merge:
            return new MergeEditConfigStrategy();
        case replace:
            return new ReplaceEditConfigStrategy();
        case delete:
            return new DeleteEditConfigStrategy();
        case remove:
            return new RemoveEditConfigStrategy();
        case recreate:
            return new ReCreateEditConfigStrategy();
        case none:
            return new NoneEditConfigStrategy();
        default:
            throw new UnsupportedOperationException("Unimplemented edit config strategy" + this);
        }
    }
}
