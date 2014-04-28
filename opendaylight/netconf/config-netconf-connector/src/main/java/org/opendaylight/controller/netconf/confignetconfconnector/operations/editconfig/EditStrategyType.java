/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.operations.editconfig;

import com.google.common.collect.Multimap;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.confignetconfconnector.exception.OperationNotPermittedException;

import java.util.EnumSet;
import java.util.Set;

public enum EditStrategyType {
    // can be default
    merge, replace, none,
    // additional per element
    delete, remove;

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
                        + " as "
                        + EditConfigXmlParser.DEFAULT_OPERATION_KEY
                        + " operations on module elements are not permitted since the default option is restrictive"),
                        NetconfDocumentedException.ErrorType.application,
                        NetconfDocumentedException.ErrorTag.operation_failed,
                        NetconfDocumentedException.ErrorSeverity.error);
            }
        }

    }
    public EditConfigStrategy getFittingStrategy(Multimap<String, String> providedServices) {
        switch (this) {
        case merge:
            return new MergeEditConfigStrategy(providedServices);
        case replace:
            return new ReplaceEditConfigStrategy(providedServices);
        case delete:
            return new DeleteEditConfigStrategy();
        case remove:
            return new RemoveEditConfigStrategy();
        case none:
            return new NoneEditConfigStrategy();
        default:
            throw new UnsupportedOperationException("Unimplemented edit config strategy" + this);
        }
    }
}
