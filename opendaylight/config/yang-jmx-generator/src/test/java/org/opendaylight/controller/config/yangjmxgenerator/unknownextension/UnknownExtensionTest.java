/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.unknownextension;


import org.opendaylight.controller.config.yangjmxgenerator.ConfigConstants;
import org.opendaylight.controller.config.yangjmxgenerator.ServiceInterfaceEntryTest;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.util.YangModelSearchUtils;
import com.google.common.collect.Lists;
import org.junit.Test;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.parser.impl.YangParserImpl;

import java.io.InputStream;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class UnknownExtensionTest extends ServiceInterfaceEntryTest {

	@Test
	public void testStopOnUnknownLanguageExtension() throws Exception {
		List<InputStream> yangISs = Lists.newArrayList(getClass().getResourceAsStream("test-ifcWithUnknownExtension.yang"));
		yangISs.addAll(getConfigApiYangInputStreams());
		try {
			YangParserImpl parser = new YangParserImpl();
			Set<Module> modulesToBuild = parser.parseYangModelsFromStreams(yangISs);
			context = parser.resolveSchemaContext(modulesToBuild);
			namesToModules = YangModelSearchUtils.mapModulesByNames(context.getModules());
			configModule = namesToModules.get(ConfigConstants.CONFIG_MODULE);
			threadsModule = namesToModules.get(ConfigConstants.CONFIG_THREADS_MODULE);
			try {
				super.testCreateFromIdentities();
				fail();
			} catch (IllegalStateException e) {
				assertTrue(e.getMessage(), e.getMessage().startsWith("Unexpected unknown schema node."));
			}
		} finally {
			for (InputStream is : yangISs) {
				is.close();
			}
		}
	}

}
