/**

 *
 * March 2013
 *
 * Copyright (c) 2013 by Cisco Systems, Inc.
 * All rights reserved.
 */
package org.opendaylight.controller.sal.binding.model.api.type.builder;

import org.opendaylight.controller.sal.binding.model.api.MethodSignature;
import org.opendaylight.controller.sal.binding.model.api.Type;

public interface MethodSignatureBuilder {

    public void addReturnType(final Type returnType);

    public void addParameter(final Type type, final String name);

    public void addComment(final String comment);

    public MethodSignature toInstance(final Type definingType);
}
