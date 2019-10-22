/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.util.List;
import org.junit.Test;

public class ConfigurationImplHybridTest extends ConfigurationImplBaseTest {

    @Override
    public ConfigurationImpl createConfiguration() {
        Config moduleShardsConf = generateModuleShards(List.of(
                generateShard("default", "default", List.of("member-1", "member-2", "member-3")),
                generateShard("people", "people-1", List.of("member-1")),
                generateShard("cars", "cars-1", List.of("member-1")),
                generateShard("test", "test-1", List.of("member-1"))
        ));
        return new ConfigurationImpl(new HybridModuleShardConfigProvider(moduleShardsConf, "modules.conf"));
    }

    @Test(expected = NullPointerException.class)
    public void testNullModuleShardsConf() {
        new HybridModuleShardConfigProvider(null, "modules.conf");
    }

    private static Config generateModuleShards(final List<String> shards) {
        String moduleShardsContent = String.format("module-shards = [%n%s]", String.join(",\n", shards));
        return ConfigFactory.parseString(moduleShardsContent);
    }

    private static String generateShard(final String name, final String shardsName, final List<String> replicas) {
        return "    {"
                + "        name = \"" + name + "\"\n"
                + "        shards = [\n"
                + "            {\n"
                + "                name=\"" + shardsName + "\"\n"
                + "                replicas = " + replicas
                + "                \n"
                + "            }\n"
                + "        ]\n"
                + "    }";
    }
}
