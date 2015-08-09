/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.restconf.impl;

import com.google.common.base.Optional;

public class WriterParameters {
    private final Optional<Integer> depth;
    private final boolean prettyPrint;

    private WriterParameters(final WriterParametersBuilder builder) {
        this.prettyPrint = builder.prettyPrint;
        this.depth = builder.depth;
    }

    public Optional<Integer> getDepth() {
        return depth;
    }

    public boolean isPrettyPrint() {
        return prettyPrint;
    }

    public static class WriterParametersBuilder {
        private Optional<Integer> depth = Optional.absent();
        private boolean prettyPrint;

        public WriterParametersBuilder() {
        }

        public Optional<Integer> getDepth() {
            return depth;
        }

        public WriterParametersBuilder setDepth(final int depth) {
            this.depth = Optional.of(depth);
            return this;
        }

        public boolean isPrettyPrint() {
            return prettyPrint;
        }

        public WriterParametersBuilder setPrettyPrint(final boolean prettyPrint) {
            this.prettyPrint = prettyPrint;
            return this;
        }

        public WriterParameters build() {
            return new WriterParameters(this);
        }
    }
}

