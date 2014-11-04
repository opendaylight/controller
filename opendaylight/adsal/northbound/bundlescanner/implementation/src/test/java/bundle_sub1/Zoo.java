/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package bundle_sub1;

import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;

import bundle_base.Animal;
import bundle_base.Mammal;


@XmlRootElement
public class Zoo {
    private Animal creature;

    @XmlElementRef
    public Animal getCreature() {
        return creature;
    }

    public void setCreature(Animal creature) {
        this.creature = creature;
    }

    public Zoo() {
        creature = new Mammal();
    }
}
