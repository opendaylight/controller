/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.model.api;

import java.util.List;

public interface Enumeration extends Type {

    public Type getDefiningType();

    public List<Pair> getValues();

    public String toFormattedString();

    interface Pair {

        public String getName();

        public Integer getValue();
    }
}
