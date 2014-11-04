/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.northbound.commons.query;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="person")

public class PersonBean {

    @XmlElement
    public String firstName;
    @XmlElement
    public String lastName;
    @XmlElement
    public String city;
    @XmlElement
    public int id;

    @XmlElementWrapper(name="emails") // ElementWrapper iteratable type
    @XmlElement
    public List<String> email;

    public PersonBean(){}
    public PersonBean(int n, String f, String l, String c) {
        firstName = f;
        lastName = l;
        city = c;
        id = n;
    }

    public void setEmail(List<String> emails){
        email = emails;
    }
    @Override
    public String toString() {
        return "PersonBean [firstName=" + firstName + ", lastName=" + lastName
                + ", city=" + city + ", id=" + id + "]";
    }

}
