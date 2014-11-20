/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yangjmxgenerator.plugin.module.abs;

import com.google.common.base.Optional;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import org.junit.Test;
import org.opendaylight.controller.config.yangjmxgenerator.ModuleMXBeanEntry;
import org.opendaylight.controller.config.yangjmxgenerator.ServiceInterfaceEntry;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.gofactory.AbsModuleGeneratedObjectFactory;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.java.FullyQualifiedName;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.java.GeneratedObject;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.module.AbstractGeneratedObjectTest;
import org.opendaylight.yangtools.yang.common.QName;

public class AbsModuleGeneratedObjectFactoryTest extends AbstractGeneratedObjectTest {

    @Test
    public void test() throws IOException {
        Map<QName,ServiceInterfaceEntry> serviceInterfaceEntryMap = loadThreadsServiceInterfaceEntries("packages.sis");
        Map<String, ModuleMXBeanEntry> namesToMBEs = loadThreadsJava(serviceInterfaceEntryMap, "packages.pack2");
        ModuleMXBeanEntry dynamicThreadPool = namesToMBEs.get(THREADPOOL_DYNAMIC_MXB_NAME);
        parseGeneratedFile(dynamicThreadPool);

    }

    private void parseGeneratedFile(ModuleMXBeanEntry moduleMXBeanEntry) throws IOException {
        Optional<String> copyright = Optional.absent();
        GeneratedObject generatedObject = new AbsModuleGeneratedObjectFactory().toGeneratedObject(moduleMXBeanEntry, copyright);
        Entry<FullyQualifiedName,File> entry = generatedObject.persist(generatorOutputPath).get();

        File dstFile = entry.getValue();
        parse(dstFile);
    }
}
