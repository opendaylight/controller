/*
 * Copyright (c) 2016 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.plugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.opendaylight.controller.config.api.annotations.RequireInterface;

class MbeASTVisitor extends SieASTVisitor {
    private final Set<String> constructors = new HashSet<>();
    private final Set<String> fieldDeclarations = new HashSet<>();
    final Set<String> methods = new HashSet<>();
    private final Map<String, String> methodJavadoc = new HashMap<>();
    final Map<String, String> requireIfc = new HashMap<>();

    String implmts;

    MbeASTVisitor(final String expectedPackageName, final String fileName) {
        super(expectedPackageName, fileName);
    }

    @Override
    public void visit(final NormalAnnotationExpr expr, final Void arg) {
        super.visit(expr, arg);

        final String fqcn = expr.getName().toString();
        if (fqcn.equals(RequireInterface.class.getCanonicalName()) && expr.getParentNode() instanceof MethodDeclaration) {
            final Node parent = expr.getParentNode();
            if (parent instanceof MethodDeclaration) {
                // remember only top level description annotation
                String reqVal = expr.getPairs().get(0).toString();
                requireIfc.put(((MethodDeclaration) parent).getName().toString(), reqVal);
            }
        }
    }

    @Override
    public void visit(final ConstructorDeclaration n, final Void arg) {
        constructors.add(n.toString());
        super.visit(n, arg);
    }

    @Override
    public void visit(final MethodDeclaration n, final Void arg) {
        final String signature = n.getDeclarationAsString(false, false);

        methods.add(signature);

        final Comment c = n.getComment();
        if (c instanceof JavadocComment) {
            methodJavadoc.put(signature, c.toString());
        }
        super.visit(n, arg);
    }

    @Override
    public void visit(final FieldDeclaration n, final Void arg) {
        fieldDeclarations.add(n.toStringWithoutComments());
        super.visit(n, arg);
    }

    @Override
    public void visit(final ClassOrInterfaceDeclaration n, final Void arg) {
        super.visit(n, arg);

        List<?> superIfcs = n.getImplements();
        implmts = superIfcs != null && !superIfcs.isEmpty() ? superIfcs.toString() : null;

        if (!n.isInterface()) {
            final List<ClassOrInterfaceType> e = n.getExtends();
            if (!e.isEmpty()) {
                extnds = e.get(0).toString();
            }
        }
    }

    void assertConstructors(final int expected) {
        assertEquals("Incorrenct number of generated constructors", expected, constructors.size());
    }

    void assertField(final String declaration) {
        assertTrue("Missing field " + declaration + ", got: " + fieldDeclarations,
            fieldDeclarations.contains(declaration + ";"));
    }

    void assertFields(final int expected) {
        assertEquals("Incorrect number of generated fields", expected, fieldDeclarations.size());
    }

    void assertMethodJavadoc(final String method, final String signature) {
        assertNotNull("Missing javadoc for " + method + " method " + methodJavadoc, methodJavadoc.get(signature));
    }

    void assertMethodJavadocs(final int expected) {
        assertEquals("Incorrenct number of generated method javadoc", expected, methodJavadoc.size());
    }
}
