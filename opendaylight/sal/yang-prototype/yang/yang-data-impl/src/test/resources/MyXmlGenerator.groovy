/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
import groovy.xml.MarkupBuilder
import org.opendaylight.controller.yang.data.impl.MyNodeBuilder

/**
 * wrapper class - applies hardcoded builder on given data closure
 */
class MyXmlGenerator {

    def myBuilder
    
    MyXmlGenerator() {
        myBuilder = MyNodeBuilder.newInstance();
    }

    MyNodeBuilder getBuilder() { 
      return myBuilder;
    }
        
    void buildTree(data) {
        data.setDelegate(myBuilder)
        data()
    }
    
    /**
     * tests builder execution
     */
    static void main(args) {
        println 'hello'
        def data = {
          network(xmlns: 'urn:opendaylight:controller:network') {
            topologies {
              topology {
                'topology-id'('topId_01')
                
                nodes {
                  node {
                    'node-id'('nodeId_02')
                    'supporting-ne'('networkId_03')
                    'termination-points' {
                      'termination-point' {
                        'tp-id'('tpId_04')
                      }
                    }
                  }
                  node {
                    'node-id'('nodeId_05')
                    'supporting-ne'('networkId_06')
                    'termination-points' {
                      'termination-point' {
                        'tp-id'('tpId_07')
                      }
                    }
                  }
                  node {
                    'node-id'('nodeId_08')
                    'supporting-ne'('networkId_09')
                    'termination-points' {
                      'termination-point' {
                        'tp-id'('tpId_10')
                      }
                      'termination-point' {
                        'tp-id'('tpId_11')
                      }
                    }
                  }
                }
                links {
                  link {
                    'link-id'('linkId_12')
                    source {
                      'source-node'('nodeId_13')
                      'source-tp'('tpId_13')
                    }
                    destination {
                      'dest-node'('nodeId_14')
                      'dest-tp'('tpId_14')
                    }
                  }
                  link {
                    'link-id'('linkId_15')
                    source {
                      'source-node'('nodeId_16')
                      'source-tp'('tpId_16')
                    }
                    destination {
                      'dest-node'('nodeId_17')
                      'dest-tp'('tpId_17')
                    }
                  }
                }
              }
            }
            'network-elements' {
              'network-element' {
                'element-id'('ntElementId_18')
              }
              'network-element' {
                'element-id'('ntElementId_19')
              }
            }
          }

        }

        def xmlGen = new MyXmlGenerator()
        xmlGen.buildTree(data)
        println xmlGen.getBuilder().getRootNode()
    }

}



