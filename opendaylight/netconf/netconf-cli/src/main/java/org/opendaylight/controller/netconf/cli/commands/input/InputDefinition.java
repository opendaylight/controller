/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli.commands.input;

import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;

/**
 * The definition of input arguments represented by schema nodes parsed from
 * yang rpc definition
 */
public class InputDefinition {

    private final ContainerSchemaNode inputContainer;

    public InputDefinition(final ContainerSchemaNode inputContainer) {
        this.inputContainer = inputContainer;
    }

    public static InputDefinition fromInput(final ContainerSchemaNode input) {
        return new InputDefinition(input);
    }

    public ContainerSchemaNode getInput() {
        return inputContainer;
    }

    // FIXME add empty as in output
    public boolean isEmpty() {
        return inputContainer == null;
    }

}
