/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.messaging;

import org.opendaylight.yangtools.util.AbstractStringIdentifier;

/**
 * Identifier that stores a string.
 *
 * @author Thomas Pantelis
 */
public class StringIdentifier extends AbstractStringIdentifier<StringIdentifier> {
    private static final long serialVersionUID = 1L;

    public StringIdentifier(String string) {
        super(string);
    }
}
