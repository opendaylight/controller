/*
 * Copyright (c) 2019 Ericsson Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.jmx.mbeans;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

import org.opendaylight.controller.cluster.datastore.Shard;
import org.opendaylight.controller.md.sal.common.util.jmx.AbstractMXBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShardDataTreeDumperMXBeanImpl extends AbstractMXBean implements ShardDataTreeDumperMXBean {

    private static final Logger LOG = LoggerFactory.getLogger(ShardDataTreeDumperMXBeanImpl.class);

    public static final String JMX_CATEGORY_SHARD = "ShardDataTreeDumperMBeans";

    private Shard shard;

    private DataOutputStream dos;

    public ShardDataTreeDumperMXBeanImpl(final String shardName, final String mxBeanType) {
        super(shardName + "-data-tree-dumper-mbean", mxBeanType, JMX_CATEGORY_SHARD);
    }

    public void setShard(Shard shard) {
        this.shard = shard;
    }

    @Override
    public void getShardDataTreeDump(String filename) {
        File file = new File(System.getProperty("karaf.data", "."),getMBeanName() + "-output-" + filename);
        String fileName = file.getAbsolutePath();
        FileOutputStream fos;
        FileWriter fw = null;

        String shardDataTree = shard.getDataStore().getDataTree().toString();

        try {
            fw = new FileWriter(fileName);
            fw.write(shardDataTree);
        } catch (IOException e) {
            LOG.error("IOException during dumping of shardDataTree using ShardDataTreeDumperMXBean {}", e);
        } finally {
            //close resources
            try {
                fw.close();
            } catch (IOException e) {
                LOG.error("IOException during closing of ShardDataTreeDump file {}", e);
            }
        }

    }
}
