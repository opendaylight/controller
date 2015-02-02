/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.api;

import com.google.common.base.Preconditions;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

/**
 * Identifier of a RPC context. This is an extension of the YANG RPC, which
 * always has global context. It allows an RPC to have a instance identifier
 * attached, so that there can be multiple implementations bound to different
 * contexts concurrently.
 */
public abstract class DOMRpcIdentifier {
    private static final class Global extends DOMRpcIdentifier {
        private Global(final @Nonnull SchemaPath type) {
            super(type);
        }

        @Override
        public YangInstanceIdentifier getContextReference() {
            return null;
        }
    }

    private static final class Local extends DOMRpcIdentifier {
        private final YangInstanceIdentifier contextReference;

        private Local(final @Nonnull SchemaPath type, final @Nonnull YangInstanceIdentifier contextReference) {
            super(type);
            this.contextReference = Preconditions.checkNotNull(contextReference);
        }

        @Override
        public YangInstanceIdentifier getContextReference() {
            return contextReference;
        }
    }

    private final SchemaPath type;

    private DOMRpcIdentifier(final SchemaPath type) {
        this.type = Preconditions.checkNotNull(type);
    }

    // FIXME: javadoc
    public static DOMRpcIdentifier create(final @Nonnull SchemaPath type) {
        return new Global(type);
    }

    // FIXME: javadoc
    public static DOMRpcIdentifier create(final @Nonnull SchemaPath type, final @Nullable YangInstanceIdentifier contextReference) {
        if (contextReference == null) {
            return new Global(type);
        } else {
            return new Local(type, contextReference);
        }
    }

    // FIXME: javadoc
    public final @Nonnull SchemaPath getType() {
        return type;
    }

    // FIXME: javadoc
    public abstract @Nullable YangInstanceIdentifier getContextReference();

    @Override
    public final int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + type.hashCode();
        result = prime * result + (getContextReference() == null ? 0 : getContextReference().hashCode());
        return result;
    }

    @Override
    public final boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof DOMRpcIdentifier)) {
            return false;
        }
        DOMRpcIdentifier other = (DOMRpcIdentifier) obj;
        if (!type.equals(other.type)) {
            return false;
        }
        return Objects.equals(getContextReference(), other.getContextReference());
    }

    @Override
    public final String toString() {
        return com.google.common.base.Objects.toStringHelper(this).omitNullValues().add("type", type).add("contextReference", getContextReference()).toString();
    }
}
