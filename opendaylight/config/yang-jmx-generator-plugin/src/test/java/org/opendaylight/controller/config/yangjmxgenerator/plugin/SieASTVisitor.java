/*
 * Copyright (c) 2016 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.plugin;

import static org.junit.Assert.assertEquals;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import java.util.HashMap;
import java.util.Map;
import org.opendaylight.controller.config.api.annotations.Description;
import org.opendaylight.controller.config.api.annotations.ServiceInterfaceAnnotation;

class SieASTVisitor extends AbstractVerifier {
    private final Map<String, String> methodDescriptions = new HashMap<>();
    protected String descriptionAnotValue;
    protected String extnds;
    protected String javadoc;
    protected String sieAnnotValue;
    protected String sieAnnotOsgiRegistrationType;

    SieASTVisitor(final String expectedPackageName, final String fileName) {
        super(expectedPackageName, fileName);
    }

    @Override
    public void visit(final ClassOrInterfaceDeclaration n, final Void arg) {
        extnds = n.getExtends().toString();

        final Comment c = n.getComment();
        if (c instanceof JavadocComment) {
            javadoc = c.toString();
        }

        super.visit(n, arg);
    }

    @Override
    public void visit(final NormalAnnotationExpr expr, final Void arg) {
        final String fqcn = expr.getName().toString();
        if (fqcn.equals(Description.class.getCanonicalName())) {
            final Node parent = expr.getParentNode();
            final String value = expr.getPairs().get(0).toString();
            if (parent instanceof ClassOrInterfaceDeclaration) {
                descriptionAnotValue = value;
            } else if (parent instanceof MethodDeclaration) {
                methodDescriptions.put(((MethodDeclaration) parent).getName(), value);
            }
        } else if (fqcn.equals(ServiceInterfaceAnnotation.class.getCanonicalName())) {
            String text1 = expr.getPairs().get(0).toString();
            String text2 = expr.getPairs().get(1).toString();
            if (text1.contains("value")) {
                sieAnnotValue = text1;
                sieAnnotOsgiRegistrationType = text2;
            } else {
                sieAnnotValue = text2;
                sieAnnotOsgiRegistrationType = text1;
            }
        }

        super.visit(expr, arg);
    }

    final void assertMethodDescriptions(final int expected) {
        assertEquals("Incorrenct number of generated method descriptions", expected, methodDescriptions.size());
    }
}
