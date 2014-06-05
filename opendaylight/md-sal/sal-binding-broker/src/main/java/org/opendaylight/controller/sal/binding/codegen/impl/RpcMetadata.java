/**
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.codegen.impl;

import javassist.CtClass;
import javassist.CtMethod;

import org.opendaylight.yangtools.yang.binding.BaseIdentity;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

final class RpcMetadata {
    private final Class<? extends BaseIdentity> context;
    private final CtMethod inputRouteGetter;
    private final Boolean routeEncapsulated;
    private final CtClass inputType;
    private final String methodName;

    public Class<? extends BaseIdentity> getContext() {
        return context;
    }

    public CtMethod getInputRouteGetter() {
        return inputRouteGetter;
    }

    public CtClass getInputType() {
        return inputType;
    }

    public boolean isRouteEncapsulated() {
        return routeEncapsulated;
    }

    public RpcMetadata(final String methodName, final Class<? extends BaseIdentity> context, final CtMethod inputRouteGetter, final boolean routeEncapsulated, final CtClass inputType) {
        this.inputRouteGetter = Preconditions.checkNotNull(inputRouteGetter);
        this.methodName = Preconditions.checkNotNull(methodName);
        this.inputType = Preconditions.checkNotNull(inputType);
        this.context = Preconditions.checkNotNull(context);
        this.routeEncapsulated = routeEncapsulated;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + methodName.hashCode();
        result = prime * result + context.hashCode();
        result = prime * result + inputRouteGetter.hashCode();
        result = prime * result + routeEncapsulated.hashCode();
        result = prime * result +  inputType.hashCode();
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof RpcMetadata)) {
            return false;
        }
        final RpcMetadata other = (RpcMetadata) obj;
        if (!methodName.equals(other.methodName)) {
            return false;
        }
        if (!context.equals(other.context)) {
            return false;
        }
        if (!inputRouteGetter.equals(other.inputRouteGetter)) {
            return false;
        }
        if (!routeEncapsulated.equals(other.routeEncapsulated)) {
            return false;
        }
        return inputType.equals(other.inputType);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("context", context)
                .add("inputRouteGetter", inputRouteGetter)
                .add("inputType", inputType)
                .add("methodName", methodName)
                .add("routeEncapsulated", routeEncapsulated)
                .toString();
    }
}
