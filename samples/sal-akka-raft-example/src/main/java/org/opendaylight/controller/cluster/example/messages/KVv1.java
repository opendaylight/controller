/*
 * Copyright (c) 2022 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.example.messages;

import java.io.Serializable;

final class KVv1 implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String key;
    private final String value;

    KVv1(String key, String value) {
        this.key = key;
        this.value = value;
    }

    Object readResolve() {
        return new KeyValue(key, value);
    }
}
