/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.operations.editconfig;

import java.util.EnumSet;
import java.util.Set;

//FIXME: make thread safe
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
        case none:
            return new NoneEditConfigStrategy();
        default:
            throw new UnsupportedOperationException("Unimplemented edit config strategy" + this);
        }
    }
}
