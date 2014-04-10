package org.opendaylight.controller.datastore.infinispan.utils;

import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AugmentationIdentifierGenerator {
    private final String id;
    private final Pattern pattern = Pattern.compile("AugmentationIdentifier\\Q{\\EchildNames=\\Q[\\E(.*)\\Q]}\\E");
    private final Matcher matcher;
    private final boolean doesMatch;

    public AugmentationIdentifierGenerator(String id){
        this.id = id;
        matcher = pattern.matcher(this.id);
        doesMatch = matcher.matches();
    }

    public boolean matches(){
        return doesMatch;
    }

    public InstanceIdentifier.AugmentationIdentifier getPathArgument(){
        Set<QName> childNames = new HashSet<QName>();
        final String childQNames = matcher.group(1);

        final String[] splitChildQNames = childQNames.split(",");

        for(String name : splitChildQNames){
            childNames.add(QName.create(name.trim()));
        }

        return new InstanceIdentifier.AugmentationIdentifier(null, childNames);
    }

}
