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
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

/**
 * An abstract base class for invocation requests. Specialized via {@link ExecuteAction} and {@link ExecuteRpc}.
 */
public abstract class AbstractExecute<T extends NormalizedNode<?, ?>> implements Serializable {
    private static final long serialVersionUID = 1L;

    private final transient @NonNull SchemaPath type;
    private final transient T input;

    AbstractExecute(final @NonNull SchemaPath type, final T input) {
        this.type = requireNonNull(type);
        this.input = input;
    }

    public final @NonNull SchemaPath getType() {
        return type;
    }

    public final T getInput() {
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
