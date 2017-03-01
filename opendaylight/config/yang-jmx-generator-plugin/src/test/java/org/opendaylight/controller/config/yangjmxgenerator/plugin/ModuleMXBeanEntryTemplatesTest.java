/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.plugin;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import com.google.common.collect.Maps;
import java.util.Collections;
import java.util.Map;
import org.junit.Test;
import org.opendaylight.controller.config.yangjmxgenerator.ModuleMXBeanEntry;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.AttributeIfc;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.JavaAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.AbstractFactoryTemplate;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.TemplateFactory;
import org.opendaylight.mdsal.binding.model.api.Type;

public class ModuleMXBeanEntryTemplatesTest {

    @Test
    public void test() {
        final ModuleMXBeanEntry mbe = mockMbe("package");
        final AbstractFactoryTemplate template = TemplateFactory
                .abstractFactoryTemplateFromMbe(mbe);
        assertNotNull(template);
    }

    public static ModuleMXBeanEntry mockMbe(final String packageName) {
        final ModuleMXBeanEntry mbe = mock(ModuleMXBeanEntry.class);
        final Map<String, AttributeIfc> a = Maps.newHashMap();
        final JavaAttribute attr = mockJavaAttr();

        a.put("attr1", attr);
        doReturn(a).when(mbe).getAttributes();
        doReturn(packageName).when(mbe).getPackageName();
        doReturn(Collections.emptyMap()).when(mbe).getProvidedServices();
        doReturn("yang-module").when(mbe).getYangModuleName();
        doReturn("local").when(mbe).getYangModuleLocalname();
        doReturn("AbstractType").when(mbe).getAbstractFactoryName();
        doReturn("Module").when(mbe).getStubModuleName();
        doReturn("fullA").when(mbe).getFullyQualifiedName(anyString());
        doReturn("uniq").when(mbe).getGloballyUniqueName();
        return mbe;
    }

    public static JavaAttribute mockJavaAttr() {
        final JavaAttribute attr = mock(JavaAttribute.class);
        final Type typeA = mock(Type.class);
        doReturn("package").when(typeA).getName();
        doReturn("type").when(typeA).getPackageName();
        doReturn("package.type").when(typeA).getFullyQualifiedName();
        doReturn(typeA).when(attr).getType();
        doReturn("Type").when(attr).getUpperCaseCammelCase();
        doReturn("new Default()").when(attr).getNullableDefault();
        return attr;
    }

}
