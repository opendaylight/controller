/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.parser.util;

/**
 * Marker object representing special 'min' or 'max' values in YANG.
 */
public final class UnknownBoundaryNumber extends Number {
    private static final long serialVersionUID = 1464861684686434869L;

    private final String value;

    UnknownBoundaryNumber(String value) {
        this.value = value;
    }

    @Override
    public int intValue() {
        return 0;
    }

    @Override
    public long longValue() {
        return 0;
    }

    @Override
    public float floatValue() {
        return 0;
    }

    @Override
    public double doubleValue() {
        return 0;
    }

    @Override
    public String toString() {
        return UnknownBoundaryNumber.class.getSimpleName() + "[" + value + "]";
    }

}
