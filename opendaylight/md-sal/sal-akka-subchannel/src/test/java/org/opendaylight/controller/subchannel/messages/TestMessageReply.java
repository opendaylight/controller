/*
 * Copyright (c) 2017 ZTE.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.subchannel.messages;

import java.io.Serializable;

/**
 * Created by HanJie on 2017/2/14.
 *
 * @author Han Jie
 */
public class TestMessageReply implements Serializable {

        private short result;
        TestMessageReply(short result){
            this.result = result;
        }

        public short getResult() {
            return result;
        }

}
