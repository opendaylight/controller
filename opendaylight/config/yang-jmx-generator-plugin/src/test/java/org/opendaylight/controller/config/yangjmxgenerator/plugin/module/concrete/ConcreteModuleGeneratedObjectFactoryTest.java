/*
 * Copyright (c) 2013, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yangjmxgenerator.plugin.module.concrete;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.common.base.Optional;
import java.io.File;
import java.util.Map.Entry;
import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.lang.java.ast.ASTClassOrInterfaceDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTCompilationUnit;
import org.junit.Test;
import org.opendaylight.controller.config.yangjmxgenerator.ModuleMXBeanEntry;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.gofactory.ConcreteModuleGeneratedObjectFactory;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.java.FullyQualifiedName;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.java.GeneratedObject;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.module.AbstractGeneratedObjectTest;

public class ConcreteModuleGeneratedObjectFactoryTest extends AbstractGeneratedObjectTest {

    @Test
    public void test() throws Exception {
        FullyQualifiedName fqn = new FullyQualifiedName("foo.bar", "Baz");
        FullyQualifiedName abstractFQN = new FullyQualifiedName("foo.bar", "AbstractBaz");
        String nullableDescription = null;

        ModuleMXBeanEntry moduleMXBeanEntry = mockModuleMXBeanEntry(fqn, abstractFQN, nullableDescription);
        Optional<String> copyright = Optional.absent();
        Optional<String> header = Optional.absent();
        GeneratedObject go = new ConcreteModuleGeneratedObjectFactory().toGeneratedObject(moduleMXBeanEntry, copyright, header);
        Entry<FullyQualifiedName, File> entry = go.persist(generatorOutputPath).get();

        File dstFile = entry.getValue();
        Node c = parse(dstFile);
        assertEquals(fqn.getPackageName(), ((ASTCompilationUnit) c).getPackageDeclaration().getPackageNameImage());
        assertEquals(fqn.getTypeName(), c.getFirstDescendantOfType(ASTClassOrInterfaceDeclaration.class).getImage());
        assertHasMethodNamed(c, "customValidation");
        assertHasMethodNamed(c, "createInstance");
    }

    static ModuleMXBeanEntry mockModuleMXBeanEntry(FullyQualifiedName fqn, FullyQualifiedName abstractFQN, String nullableDescription) {
        ModuleMXBeanEntry mock = mock(ModuleMXBeanEntry.class);
        assertEquals(fqn.getPackageName(), abstractFQN.getPackageName());
        doReturn(fqn.getPackageName()).when(mock).getPackageName();
        doReturn(fqn.getTypeName()).when(mock).getStubModuleName();
        doReturn(nullableDescription).when(mock).getNullableDescription();
        doReturn(abstractFQN.getTypeName()).when(mock).getAbstractModuleName();
        return mock;
    }
}
