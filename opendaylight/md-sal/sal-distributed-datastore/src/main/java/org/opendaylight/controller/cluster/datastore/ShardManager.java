/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import java.io.Serializable;
import java.util.Set;

/**
 * @deprecated This is a deprecated placeholder to keep its inner class present. It serves no other purpose.
 */
@Deprecated
public final class ShardManager {
    /**
     * We no longer persist SchemaContextModules but keep this class around for now for backwards
     * compatibility so we don't get de-serialization failures on upgrade from Helium.
     */
    @Deprecated
    public static class SchemaContextModules implements Serializable {
        private static final long serialVersionUID = -8884620101025936590L;

        private final Set<String> modules;

        public SchemaContextModules(Set<String> modules){
            this.modules = modules;
        }

        public Set<String> getModules() {
            return modules;
        }
    }

    private ShardManager() {
        throw new UnsupportedOperationException("deprecated outer class");
    }
}
