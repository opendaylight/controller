package org.opendaylight.controller.datastore.infinispan.utils;

import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NodeIdentifierWithValueGenerator{
        private final String id;
        private final Pattern pattern = Pattern.compile("(.*)\\Q[\\E(.*)\\Q]\\E");
        private final Matcher matcher;
        private final boolean doesMatch;

        public NodeIdentifierWithValueGenerator(String id){
            this.id = id;
            matcher = pattern.matcher(this.id);
            doesMatch = matcher.matches();
        }

        public boolean matches(){
            return doesMatch;
        }

        public InstanceIdentifier.PathArgument getPathArgument(){
            final String name = matcher.group(1);
            final String value = matcher.group(2);

            return new InstanceIdentifier.NodeWithValue(QName.create(name), value);
        }
    }
