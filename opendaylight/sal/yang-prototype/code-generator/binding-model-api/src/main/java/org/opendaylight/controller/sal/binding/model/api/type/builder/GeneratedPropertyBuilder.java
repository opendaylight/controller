/*
  * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
  *
  * This program and the accompanying materials are made available under the
  * terms of the Eclipse Public License v1.0 which accompanies this distribution,
  * and is available at http://www.eclipse.org/legal/epl-v10.html
  */
package org.opendaylight.controller.sal.binding.model.api.type.builder;

import org.opendaylight.controller.sal.binding.model.api.AccessModifier;
import org.opendaylight.controller.sal.binding.model.api.GeneratedProperty;
import org.opendaylight.controller.sal.binding.model.api.Type;

/**

 *
 */
public interface GeneratedPropertyBuilder {
    
    public String getName();
    
    public boolean addReturnType(final Type returnType);
    
    public void accessorModifier(final AccessModifier modifier);
    
    public void addComment(final String comment);
    
    public void setFinal(final boolean isFinal);
    
    public void setReadOnly(final boolean isReadOnly);
    
    public GeneratedProperty toInstance(final Type definingType);
}
