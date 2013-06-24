def data = {
 network(xmlns: 'urn:opendaylight:controller:network') {
    topologies {
      topology {
        'topology-id'('topId_01')
        
        //types()
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

System.err.println('data inited')

import MyXmlGenerator

xmlGen = new MyXmlGenerator()
xmlGen.buildTree(data)
