/*
 * Copyright (c) 2019 Nordix Foundation.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.messages;

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import java.io.Serializable;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * An abstract base class for invocation requests. Specialized via {@link ExecuteAction} and {@link ExecuteRpc}.
 */
public abstract class AbstractExecute<T, I extends NormalizedNode> implements Serializable {
    private static final long serialVersionUID = 1L;

    private final transient @NonNull T type;
    private final transient I input;

    AbstractExecute(final @NonNull T type, final I input) {
        this.type = requireNonNull(type);
        this.input = input;
    }

    public final @NonNull T getType() {
        return type;
    }

    public final I getInput() {
        return input;
    }

    @Override
    public final String toString() {
        // We want 'type' to be always first
        return addToStringAttributes(MoreObjects.toStringHelper(this).omitNullValues().add("type", type)).toString();
    }

    ToStringHelper addToStringAttributes(final ToStringHelper helper) {
        return helper.add("input", input);
    }

    abstract Object writeReplace();
}
