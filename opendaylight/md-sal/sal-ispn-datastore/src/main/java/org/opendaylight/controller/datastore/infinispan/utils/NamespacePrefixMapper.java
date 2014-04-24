package org.opendaylight.controller.datastore.infinispan.utils;

import com.google.common.primitives.Ints;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NamespacePrefixMapper {
    private final Map<String, Integer> nameSpaceToPrefix = new HashMap<>();
    private final Map<Integer, String> prefixToNameSpace = new HashMap<>();

    private final Pattern pattern = Pattern.compile("\\Q(\\E(.*?)\\Q)\\E");
    private final AtomicInteger currentPrefix = new AtomicInteger(1);

    private static final NamespacePrefixMapper instance = new NamespacePrefixMapper();

    /**
     * If the String is from an InstanceIdentifier then it probably has full name spaces in it
     * @param instanceIdentifier
     * @return
     */
    public String fromInstanceIdentifier(String instanceIdentifier){
        Matcher matcher = pattern.matcher(instanceIdentifier);
        String output = instanceIdentifier;
        while(matcher.find()){
            final String group = matcher.group();
            final String nameSpace = group.substring(1, group.length() - 1);
            if(nameSpace.length() > 6){
                Integer prefix = nameSpaceToPrefix.get(nameSpace);
                if(prefix == null){
                    synchronized (this) {
                        prefix = nameSpaceToPrefix.get(nameSpace);
                        if(prefix == null){
                            prefix = currentPrefix.getAndIncrement();
                            nameSpaceToPrefix.put(nameSpace, prefix);
                            prefixToNameSpace.put(prefix, nameSpace);
                        }
                    }
                }
                output = output.replace(nameSpace, prefix.toString());
            }
        }

        return output;
    }

    /**
     * Replace prefix with namespace
     * @param fqn
     * @return
     */
    public String fromFqn(String fqn){
        Matcher matcher = pattern.matcher(fqn);
        String output = fqn;
        while(matcher.find()){
            final String group = matcher.group();
            final String prefix = group.substring(1, group.length() - 1);
            String nameSpace = null;
            final Integer integerPrefix = Ints.tryParse(prefix);
            if(integerPrefix != null){
                nameSpace = prefixToNameSpace.get(integerPrefix);
            }

            if(nameSpace != null){
                output = output.replace("(" + prefix.toString() + ")", "(" + nameSpace + ")");
            }
        }

        return output;

    }


    public static NamespacePrefixMapper get(){
        return instance;
    }
}
