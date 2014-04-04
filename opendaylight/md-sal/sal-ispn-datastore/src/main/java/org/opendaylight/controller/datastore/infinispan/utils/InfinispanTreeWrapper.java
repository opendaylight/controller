package org.opendaylight.controller.datastore.infinispan.utils;

import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.infinispan.tree.Fqn;
import org.infinispan.tree.Node;
import org.infinispan.tree.TreeCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class InfinispanTreeWrapper {

    private static final String DATA = "___data___";
    private static final AtomicLong putCount = new AtomicLong(0);
    private static final AtomicLong getCount = new AtomicLong(0);
    private static final AtomicLong getChildrenCount = new AtomicLong(0);

    private static final Logger logger = LoggerFactory.getLogger("TRANSACTION");

    public void writeValue(TreeCache treeCache, Fqn path, Object value){
        putCount.incrementAndGet();
        treeCache.put(path, DATA, value);
    }

    public Object readValue(Node node){
        getCount.incrementAndGet();
        return node.get(DATA);
    }

    public Set<Node> getChildren(Node node){
        getChildrenCount.incrementAndGet();
        return node.getChildren();
    }

    public static void logCounts(){
        logger.info("Put counts = " + putCount.get());
        logger.info("Get counts = " + getCount.get());
        logger.info("GetChildren counts = " + getChildrenCount.get());
    }

    public static final class CommandHandler implements CommandProvider {

        public void _logInfinispanCounts(CommandInterpreter ci) {
            InfinispanTreeWrapper.logCounts();
        }


        @Override
        public String getHelp() {
            return "logInfinispanCounts";
        }
    }
}
