/*
 * Copyright (c) 2016 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.plugin;

import static org.junit.Assert.assertEquals;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.google.common.base.Preconditions;

abstract class AbstractVerifier extends VoidVisitorAdapter<Void> {
    private final String expectedPackageName;
    private final String expectedType;
    private String packageName;
    private String type;

    AbstractVerifier(final String expectedPackageName, final String fileName) {
        this.expectedPackageName = Preconditions.checkNotNull(expectedPackageName);
        this.expectedType = fileName.substring(0, fileName.length() - 5);
    }

    @Override
    public void visit(final ClassOrInterfaceDeclaration n, final Void arg) {
        type = n.getName();
        super.visit(n, arg);
    }

    @Override
    public final void visit(final PackageDeclaration n, final Void arg) {
        packageName = n.getName().toString();
        super.visit(n, arg);
    }

    void verify() {
        assertEquals(expectedPackageName, packageName);
        assertEquals(expectedType, type);
    }
}
