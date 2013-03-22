/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.model.api.type.builder;

import org.opendaylight.controller.sal.binding.model.api.GeneratedType;
import org.opendaylight.controller.sal.binding.model.api.Type;

/**

 *
 */
public interface GeneratedTypeBuilder extends Type {

    public Type getParentType();

    public void addComment(final String comment);

    public ConstantBuilder addConstant(final Type type, final String name,
            final Object value);

    public EnumBuilder addEnumeration(final String name);

    public MethodSignatureBuilder addMethod(final String name);

    public GeneratedType toInstance();
}
