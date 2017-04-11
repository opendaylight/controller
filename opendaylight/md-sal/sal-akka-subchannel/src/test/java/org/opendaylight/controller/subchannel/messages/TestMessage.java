/*
 * Copyright (c) 2017 ZTE.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.subchannel.messages;

import scala.Serializable;

/**
 * Created by HanJie on 2017/2/14.
 *
 * @author Han Jie
 */
public class TestMessage implements Serializable{
    private int id = 0;
    private int[] data = new int[2048*1000];
    TestMessage(){

    }
    TestMessage(int id){
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
