/*
 * Copyright (c) 2019 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Message sent to a data tree change listener actor to indicate there is no initial data.
 *
 * @author Thomas Pantelis
 */
@NonNullByDefault
public final class OnInitialData {
    public static final OnInitialData INSTANCE = new OnInitialData();

    private OnInitialData() {
        // Hidden on purpose
    }
}
