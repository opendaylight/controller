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
import java.util.Locale;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.clustering.shard.configuration.rev191128.DatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.clustering.shard.configuration.rev191128.shard.persistence.Persistence;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.clustering.shard.configuration.rev191128.shard.persistence.PersistenceBuilder;

public class ConfigurationImplHybridTest extends ConfigurationImplBaseTest {

    @Override
    public ConfigurationImpl createConfiguration() {
        Config moduleShardsConf = generateModuleShards(List.of(
                generateShard("default", new PersistenceBuilder().setDatastore(DatastoreType.Configuration)
                        .setPersistent(true).build(), "default", List.of("member-1", "member-2", "member-3")),
                generateShard("people", null, "people-1", List.of("member-1")),
                generateShard("cars", new PersistenceBuilder().setDatastore(DatastoreType.Configuration)
                        .setPersistent(false).build(), "cars-1", List.of("member-1")),
                generateShard("test", null, "test-1", List.of("member-1"))
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

    private static String generateShard(final String name, final Persistence persistence, final String shardsName,
                                        final List<String> replicas) {
        return "    {"
                + "        name = \"" + name + "\"\n"
                + persistenceToString(persistence)
                + "        shards = [\n"
                + "            {\n"
                + "                name=\"" + shardsName + "\"\n"
                + "                replicas = " + replicas
                + "                \n"
                + "            }\n"
                + "        ]\n"
                + "    }";
    }

    private static String persistenceToString(final Persistence persistence) {
        if (persistence == null) {
            return "";
        }
        return String.format("persistence = {\n"
                        + "            datastore-type = \"%s\"\n"
                        + "            persistent = %s\n"
                        + "        }\n", persistence.getDatastore().getName().toLowerCase(Locale.getDefault()),
                persistence.isPersistent().toString().toLowerCase(Locale.getDefault()));
    }
}
