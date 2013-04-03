
/* 
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved. 
 * 
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

//PAGE flows
one.f = {};

// specify dashlets and layouts
one.f.dashlet = {
    flows : {
        id : 'flows',
        name : 'Flow Entries'
    },
    nodes : {
        id : 'nodes',
        name : 'Nodes'
    },
    detail : {
        id : 'detail',
        name : 'Flow Detail'
    }
};

one.f.menu = {
    left : {
        top : [
            one.f.dashlet.flows
        ],
        bottom : [
            one.f.dashlet.nodes
        ]
    },
    right : {
        top : [],
        bottom : [
            one.f.dashlet.detail
        ]
    }
};

one.f.address = {
    root : "/controller/web/flows",
    flows : {
        main : "/main",
		flows : "/node-flows",
        nodes : "/node-ports",
        flow : "/flow"
    }
}

/** NODES **/
one.f.nodes = {
    id : {},
    registry : {},
    dashlet : function($dashlet) {
        var $h4 = one.lib.dashlet.header("Nodes");
        $dashlet.append($h4);
        
        // load body
        one.f.nodes.ajax.dashlet(function($table) {
			// total nodes count
			var nodeCount = $table.find('tbody').find('tr').size();
			// prompt output
			var nodeText = "node";
			var verb = "is";
			if (nodeCount != 1) {
				nodeText += "s";
				verb = "are";
			}
			var out = "There "+verb+" "+nodeCount+" "+nodeText;
			$p = $(document.createElement('p'));
			$p.append(out);
			$dashlet.append($p);
            // add to dashlet
            $dashlet.append($table);
        });
    },
    ajax : {
        dashlet : function(callback) {
            $.getJSON(one.f.address.root+one.f.address.flows.flows, function(data) {
                var body = one.f.nodes.data.dashlet(data);
                var $body = one.f.nodes.body.dashlet(body, callback);
                callback($body);
            });
        }
    },
    data : {
        dashlet : function(data) {
            var body = [];
            $.each(data, function(key, value) {
                var tr = {};
                var entry = [];
                entry.push(key);
                // parse ports
                entry.push(value);
                // add entry to tr
                tr['entry'] = entry;
                body.push(tr);
            });
            return body;
        }
    },
    body : {
        dashlet : function(body, callback) {
            var attributes = ['table-striped', 'table-bordered', 'table-hover', 'table-condensed'];
            var $table = one.lib.dashlet.table.table(attributes);
            
            var headers = ['Node', 'Flows'];
            var $thead = one.lib.dashlet.table.header(headers);
            $table.append($thead);
            
            var $tbody = one.lib.dashlet.table.body(body);
            $table.append($tbody);
            
            return $table;
        }
    }
}

/** FLOW DETAIL **/
one.f.detail = {
    id : {},
    registry : {},
    dashlet : function($dashlet, details) {
        var $h4 = one.lib.dashlet.header("Flow Details");
        $dashlet.append($h4);
        
        // details
        if (details == undefined) {
        	var $none = $(document.createElement('div'));
        	$none.addClass('none');
            var $p = $(document.createElement('p'));
            $p.text('Please select a flow');
            $p.addClass('text-center').addClass('text-info');
            
            $dashlet.append($none)
            	.append($p);
        }
    },
    data : {
		dashlet : function(data) {
			var body = [];
			var tr = {};
			var entry = [];

			entry.push(data['name']);
			entry.push(data['node']);
			entry.push(data['flow']['priority']);
			entry.push(data['flow']['hardTimeout']);
			entry.push(data['flow']['idleTimeout']);

			tr.entry = entry;
			body.push(tr);
			return body;
		},
        description : function(data) {
			var body = [];
			var tr = {};
			var entry = [];

			entry.push(data['flow']['etherType']);
			entry.push(data['flow']['vlanId']);
			entry.push(data['flow']['vlanPriority']);
			entry.push(data['flow']['srcMac']);
			entry.push(data['flow']['dstMac']);
			entry.push(data['flow']['srcIp']);
			entry.push(data['flow']['dstIp']);
			entry.push(data['flow']['tosBits']);
			entry.push(data['flow']['srcPort']);
			entry.push(data['flow']['dstPort']);
			entry.push(data['flow']['protocol']);
			entry.push(data['flow']['cookie']);

			tr.entry = entry;
			body.push(tr);
			return body;
        },
		actions : function(data) {
			var body = [];
			var tr = {};
			var entry = [];

			var actions = '';
			$(data['flow']['actions']).each(function(index, value) {
				actions += value + ', ';
			});
			actions = actions.slice(0,-2);
			entry.push(actions);

			tr.entry = entry;
			body.push(tr);
			return body;
		}
    },
    body : {
        dashlet : function(body) {
			// create table
			var header = ['Flow Name', 'Node', 'Priority', 'Hard Timeout', 'Idle Timeout'];
			var $thead = one.lib.dashlet.table.header(header);
			var attributes = ['table-striped', 'table-bordered', 'table-condensed'];
			var $table = one.lib.dashlet.table.table(attributes);
			$table.append($thead);

			var $tbody = one.lib.dashlet.table.body(body);
			$table.append($tbody);

            return $table;
        },
		description : function(body) {
			var header = ['Ethernet Type', 'VLAN ID', 'VLAN Priority', 'Source MAC', 'Dest MAC', 'Source IP', 'Dest IP', 'TOS', 'Source Port', 'Dest Port', 'Protocol', 'Cookie'];
			var $thead = one.lib.dashlet.table.header(header);
			var attributes = ['table-striped', 'table-bordered', 'table-condensed'];
			var $table = one.lib.dashlet.table.table(attributes);
			$table.append($thead);

			var $tbody = one.lib.dashlet.table.body(body);
			$table.append($tbody);

            return $table;
		},
		actions : function(body) {
			var header = ['Actions'];
			var $thead = one.lib.dashlet.table.header(header);
			var attributes = ['table-striped', 'table-bordered', 'table-condensed'];
			var $table = one.lib.dashlet.table.table(attributes);
			$table.append($thead);

			var $tbody = one.lib.dashlet.table.body(body);
			$table.append($tbody);

            return $table;
		}
    }
}

/** FLOW ENTRIES **/
one.f.flows = {
    id : {
        dashlet : {
            add : "one_f_flows_id_dashlet_add",
            remove : "one_f_flows_id_dashlet_remove",
            toggle : "one_f_flows_id_dashlet_toggle"
        },
        modal : {
			install : "one_f_flows_id_modal_install",
            add : "one_f_flows_id_modal_add",
            close : "one_f_flows_id_modal_close",
            modal : "one_f_flows_id_modal_modal",
            dialog : {
            	modal : "one_f_flows_id_modal_dialog_modal",
                remove : "one_f_flows_id_modal_dialog_remove",
                close : "one_f_flows_id_modal_dialog_close"
            },
            action : {
                button : "one_f_flows_id_modal_action_button",
                modal : "one_f_flows_id_modal_action_modal",
                add : "one_f_flows_id_modal_action_add",
                close : "one_f_flows_id_modal_action_close",
                table : "one_f_flows_id_modal_action_table",
                addOutputPorts : "one_f_flows_id_modal_action_addOutputPorts",
                setVlanId : "one_f_flows_id_modal_action_setVlanId",
                setVlanPriority : "one_f_flows_id_modal_action_setVlanPriority",
                modifyDatalayerSourceAddress : "one_f_flows_id_modal_action_modifyDatalayerSourceAddress",
                modifyDatalayerDestinationAddress : "one_f_flows_id_modal_action_modifyDatalayerDestinationAddress",
                modifyNetworkSourceAddress : "one_f_flows_modal_action_modifyNetworkSourceAddress",
                modifyNetworkDestinationAddress : "one_f_flows_modal_action_modifyNetworkDestinationAddress",
                modifyTosBits : "one_f_flows_modal_action_modifyTosBits",
                modifyTransportSourcePort : "one_f_flows_modal_action_modifyTransportSourcePort",
                modifyTransportDestinationPort : "one_f_flows_modal_action_modifyTransportDestinationPort",
				modal : {
					modal : "one_f_flows_modal_action_modal_modal",
					remove : "one_f_flows_modal_action_modal_remove",
					cancel : "one_f_flows_modal_action_modal_cancel"
				}
            },
            form : {
                name : "one_f_flows_id_modal_form_name",
                nodes : "one_f_flows_id_modal_form_nodes",
                port : "one_f_flows_id_modal_form_port",
                priority : "one_f_flows_id_modal_form_priority",
                hardTimeout : "one_f_flows_id_modal_form_hardTimeout",
				idleTimeout : "one_f_flows_id_modal_form_idleTimeout",
				cookie : "one_f_flows_id_modal_form_cookie",
                etherType : "one_f_flows_id_modal_form_etherType",
                vlanId : "one_f_flows_id_modal_form_vlanId",
                vlanPriority : "one_f_flows_id_modal_form_vlanPriority",
                srcMac : "one_f_flows_id_modal_form_srcMac",
                dstMac : "one_f_flows_id_modal_form_dstMac",
                srcIp : "one_f_flows_id_modal_form_srcIp",
                dstIp : "one_f_flows_id_modal_form_dstIp",
                tosBits : "one_f_flows_id_modal_form_tosBits",
                srcPort : "one_f_flows_id_modal_form_srcPort",
                dstPort : "one_f_flows_id_modal_form_dstPort",
                protocol : "one_f_flows_id_modal_form_protocol"
            }
        }
    },
    registry : {},
    dashlet : function($dashlet, callback) {
        var $h4 = one.lib.dashlet.header("Flow Entries");
        
        if (one.role < 2) {
	        var button = one.lib.dashlet.button.single("Add Flow Entry", one.f.flows.id.dashlet.add, "btn-primary", "btn-mini");
	        var $button = one.lib.dashlet.button.button(button);
	        
	        $button.click(function() {
	            var $modal = one.f.flows.modal.initialize();
	            $modal.modal();
	        });
        }
        
        $dashlet.append($h4);
        if (one.role < 2) $dashlet.append($button);
        
        // load body
        one.f.flows.ajax.dashlet(function($table) {
			// total flows
			var flowCount = $table.find('tbody').find('tr').size();
			// prompt output
			var flowText = "flow";
			var verb = "is";
			if (flowCount != 1) {
				flowText += "s";
				verb = "are";
			}
			var out = "There "+verb+" "+flowCount+" "+flowText;
			$p = $(document.createElement('p'));
			$p.append(out);
			$dashlet.append($p);
            // table bindings
            $table.find('tbody').find('tr').click(function() {
                var id = $($(this).find('td')[0]).text();
				var node = $(this).data('id');
                one.f.flows.detail(id, node);
            });
            // add to dashlet
            $dashlet.append($table);
            // details callback
            if(callback != undefined) callback();
        });
    },
    detail : function(id, node) {
        // clear flow details
        var $detailDashlet = one.main.dashlet.right.bottom;
        $detailDashlet.empty();
        var $h4 = one.lib.dashlet.header("Flow Overview");
        $detailDashlet.append($h4);
        
        // details
        var flows = one.f.flows.registry.flows;
        var flow;
        $(flows).each(function(index, value) {
            if (value['name'] == id) {
                flow = value;
            }
        });
        if (one.role < 2) {
	        // remove button
	        var button = one.lib.dashlet.button.single("Remove Flow", one.f.flows.id.dashlet.remove, "btn-danger", "btn-mini");
	        var $button = one.lib.dashlet.button.button(button);
	        $button.click(function() {
	            var $modal = one.f.flows.modal.dialog.initialize(id, node);
	            $modal.modal();
	        });
	        // toggle button
	        var toggle;
	        if (flow['flow']['installInHw'] == 'true') {
	            toggle = one.lib.dashlet.button.single("Uninstall Flow", one.f.flows.id.dashlet.toggle, "btn-warning", "btn-mini");
	        } else {
	            toggle = one.lib.dashlet.button.single("Install Flow", one.f.flows.id.dashlet.toggle, "btn-success", "btn-mini");
	        }
	        var $toggle = one.lib.dashlet.button.button(toggle);
	        $toggle.click(function() {
	            one.f.flows.modal.ajax.toggleflow(id, node, function(data) {
	                if(data == "Success") {
	                    one.main.dashlet.right.bottom.empty();
	                    one.f.detail.dashlet(one.main.dashlet.right.bottom);
	                    one.main.dashlet.left.top.empty();
	                	one.f.flows.dashlet(one.main.dashlet.left.top, function() {
	                	   // checks are backwards due to stale registry
	                	   if(flow['flow']['installInHw'] == 'true') {
	                	       one.lib.alert('Uninstalled Flow');
	                	   } else {
	                	       one.lib.alert('Installed Flow');
	                	   }
	                	   one.f.flows.detail(id, node)
	                	});
	                } else {
	                    one.lib.alert('Cannot toggle flow: '+data);
	                }
	            });
	        });
        }
        // append details
        var body = one.f.detail.data.dashlet(flow);
        var $body = one.f.detail.body.dashlet(body);
        if (one.role < 2) $detailDashlet.append($button).append($toggle);
        $detailDashlet.append($body);
		var body = one.f.detail.data.description(flow);
		var $body = one.f.detail.body.description(body);
		$detailDashlet.append($body);
		var body = one.f.detail.data.actions(flow);
		var $body = one.f.detail.body.actions(body);
		$detailDashlet.append($body);
    },
    modal : {
        dialog : {
            initialize : function(id, node) {
                var h3 = "Remove Flow?";
                var $p = one.f.flows.modal.dialog.body(id);
                var footer = one.f.flows.modal.dialog.footer();
                var $modal = one.lib.modal.spawn(one.f.flows.id.modal.dialog.modal, h3, $p, footer);
                $('#'+one.f.flows.id.modal.dialog.close, $modal).click(function() {
                    $modal.modal('hide');
                });
                $('#'+one.f.flows.id.modal.dialog.remove, $modal).click(function() {
                    one.f.flows.modal.ajax.removeflow(id, node, function(data) {
                        if (data == "Success") {
                            $modal.modal('hide');
                            one.main.dashlet.right.bottom.empty();
                            one.f.detail.dashlet(one.main.dashlet.right.bottom);
                            one.main.dashlet.left.top.empty();
                            one.f.flows.dashlet(one.main.dashlet.left.top);
                            one.lib.alert('Flow removed');
                        } else {
                            one.lib.alert('Cannot remove flow: '+data);
                        }
                    });
                });
                return $modal;
            },
            footer : function() {
                var footer = [];
                
                var removeButton = one.lib.dashlet.button.single("Remove Flow", one.f.flows.id.modal.dialog.remove, "btn-danger", "");
                var $removeButton = one.lib.dashlet.button.button(removeButton);
                footer.push($removeButton);
                
                var closeButton = one.lib.dashlet.button.single("Cancel", one.f.flows.id.modal.dialog.close, "", "");
                var $closeButton = one.lib.dashlet.button.button(closeButton);
                footer.push($closeButton);
                
                return footer;
            },
            body : function(id) {
                var $p = $(document.createElement('p'));
                $p.append('Remove flow '+id+'?');
                return $p;
            }
        },
        initialize : function() {
            var h3 = "Add Flow Entry";
            var footer = one.f.flows.modal.footer();
            var $modal = one.lib.modal.spawn(one.f.flows.id.modal.modal, h3, "", footer);
            
            // bind close button
            $('#'+one.f.flows.id.modal.close, $modal).click(function() {
                $modal.modal('hide');
            });
            
            // bind add flow button
            $('#'+one.f.flows.id.modal.add, $modal).click(function() {
                one.f.flows.modal.add($modal, 'false');
            });

            // bind install flow button
            $('#'+one.f.flows.id.modal.install, $modal).click(function() {
                one.f.flows.modal.add($modal, 'true');
            });
            
            // inject body (nodePorts)
            one.f.flows.modal.ajax.nodes(function(nodes, nodeports) {
                var $body = one.f.flows.modal.body(nodes, nodeports);
                one.lib.modal.inject.body($modal, $body);
            });
            
            return $modal;
        },
        add : function($modal, install) {
            var result = {};
            
            result['name'] = $('#'+one.f.flows.id.modal.form.name, $modal).val();
            result['ingressPort'] = $('#'+one.f.flows.id.modal.form.port, $modal).val();
            result['priority'] = $('#'+one.f.flows.id.modal.form.priority, $modal).val();
            result['hardTimeout'] = $('#'+one.f.flows.id.modal.form.hardTimeout, $modal).val();
			result['idleTimeout'] = $('#'+one.f.flows.id.modal.form.idleTimeout, $modal).val();
			result['cookie'] = $('#'+one.f.flows.id.modal.form.cookie, $modal).val();
            result['etherType'] = $('#'+one.f.flows.id.modal.form.etherType, $modal).val();
            result['vlanId'] = $('#'+one.f.flows.id.modal.form.vlanId, $modal).val();
            result['vlanPriority'] = $('#'+one.f.flows.id.modal.form.vlanPriority, $modal).val();
            result['dlSrc'] = $('#'+one.f.flows.id.modal.form.srcMac, $modal).val();
            result['dlDst'] = $('#'+one.f.flows.id.modal.form.dstMac, $modal).val();
            result['nwSrc'] = $('#'+one.f.flows.id.modal.form.srcIp, $modal).val();
            result['nwDst'] = $('#'+one.f.flows.id.modal.form.dstIp, $modal).val();
            result['tosBits'] = $('#'+one.f.flows.id.modal.form.tosBits, $modal).val();
            result['tpSrc'] = $('#'+one.f.flows.id.modal.form.srcPort, $modal).val();
            result['tpDst'] = $('#'+one.f.flows.id.modal.form.dstPort, $modal).val();
            result['protocol'] = $('#'+one.f.flows.id.modal.form.protocol, $modal).val();

			result['installInHw'] = install;

            var nodeId = $('#'+one.f.flows.id.modal.form.nodes, $modal).val();
            
            $.each(result, function(key, value) {
                if (value == "") delete result[key];
            });
            
            var action = [];
            var $table = $('#'+one.f.flows.id.modal.action.table, $modal);
            $($table.find('tbody').find('tr')).each(function(index, value) {
				if (!$(this).find('td').hasClass('empty')) {
	                action.push($(value).data('action'));
				}
            });
            result['actions'] = action;
            
            // frontend validation
			if (result['name'] == undefined) {
				alert('Need flow name');
				return;
			}
			if (nodeId == '') {
				alert('Select node');
				return;
			}
			if (result['ingressPort'] == undefined) {
				alert('Select port');
				return;
			}
			if (action.length == 0) {
				alert('Please specify an action');
				return;
			}
            
			// package for ajax call
            var resource = {};
            resource['body'] = JSON.stringify(result);
            resource['action'] = 'add';
			resource['nodeId'] = nodeId;
            
            one.f.flows.modal.ajax.saveflow(resource, function(data) {
                if (data == "Success") {
                    $modal.modal('hide');
                    one.lib.alert('Flow added');
                    one.main.dashlet.left.top.empty();
                    one.f.flows.dashlet(one.main.dashlet.left.top);
                } else {
					alert('Could not add flow: '+data);
                }
            });
        },
        ajax : {
            nodes : function(successCallback) {
                $.getJSON(one.f.address.root+one.f.address.flows.nodes, function(data) {
                    var nodes = one.f.flows.modal.data.nodes(data);
                    var nodeports = data;
                    one.f.flows.registry['nodeports'] = nodeports;
                    
                    successCallback(nodes, nodeports);
                });
            },
            saveflow : function(resource, callback) {
                $.post(one.f.address.root+one.f.address.flows.flow, resource, function(data) {
                    callback(data);
                });
            },
            removeflow : function(id, node, callback) {
            	resource = {};
            	resource['action'] = 'remove';
                $.post(one.f.address.root+one.f.address.flows.flow+'/'+node+'/'+id, resource, function(data) {
                    callback(data);
                });
            },
            toggleflow : function(id, node, callback) {
            	resource = {};
            	resource['action'] = 'toggle';
            	$.post(one.f.address.root+one.f.address.flows.flow+'/'+node+'/'+id, resource, function(data) {
            		callback(data);
            	});
            }
        },
        data : {
            nodes : function(data) {
                result = {};
                $.each(data, function(key, value) {
                    result[key] = value['name'];
                });
                return result;
            }
        },
        body : function(nodes, nodeports) {
            var $form = $(document.createElement('form'));
			var $fieldset = $(document.createElement('fieldset'));
			// flow description
			var $legend = one.lib.form.legend("Flow Description");
			$fieldset.append($legend);
			// name
			var $label = one.lib.form.label("Name");
			var $input = one.lib.form.input("Match Name");
			$input.attr('id', one.f.flows.id.modal.form.name);
			$fieldset.append($label).append($input);
			// node
			var $label = one.lib.form.label("Node");
			var $select = one.lib.form.select.create(nodes);
			one.lib.form.select.prepend($select, { '' : 'Please Select a Node' });
			$select.val($select.find("option:first").val());
			$select.attr('id', one.f.flows.id.modal.form.nodes);
			
			// bind onchange
			$select.change(function() {
			    // retrieve port value
			    var node = $(this).find('option:selected').attr('value');
			    var $ports = $('#'+one.f.flows.id.modal.form.port);
				if (node == '') {
					one.lib.form.select.inject($ports, {});
					return;
				}
			    one.f.flows.registry['currentNode'] = node;
			    var ports = nodeports[node]['ports'];
			    one.lib.form.select.inject($ports, ports);
			    one.lib.form.select.prepend($ports, { '' : 'Please Select a Port' });
			    $ports.val($ports.find("option:first").val());
			});

            $fieldset.append($label).append($select);
			// input port
			var $label = one.lib.form.label("Input Port");
			var $select = one.lib.form.select.create();
			$select.attr('id', one.f.flows.id.modal.form.port);
			$fieldset.append($label).append($select);
			// priority
			var $label = one.lib.form.label("Priority");
			var $input = one.lib.form.input("Priority");
			$input.attr('id', one.f.flows.id.modal.form.priority);
			$input.val('500');
			$fieldset.append($label).append($input);
			// hardTimeout
			var $label = one.lib.form.label("Hard Timeout");
			var $input = one.lib.form.input("Hard Timeout");
			$input.attr('id', one.f.flows.id.modal.form.hardTimeout);
			$fieldset.append($label).append($input);
			// idleTimeout
			var $label = one.lib.form.label("Idle Timeout");
			var $input = one.lib.form.input("Idle Timeout");
			$input.attr('id', one.f.flows.id.modal.form.idleTimeout);
			$fieldset.append($label).append($input);
			// cookie
			var $label = one.lib.form.label("Cookie");
			var $input = one.lib.form.input("Cookie");
			$input.attr('id', one.f.flows.id.modal.form.cookie);
			$fieldset.append($label).append($input);
			// layer 2
			var $legend = one.lib.form.legend("Layer 2");
			$fieldset.append($legend);
			// etherType
			var $label = one.lib.form.label("Ethernet Type");
			var $input = one.lib.form.input("Ethernet Type");
			$input.attr('id', one.f.flows.id.modal.form.etherType);
			$input.val('0x800');
			$fieldset.append($label).append($input);
			// vlanId
			var $label = one.lib.form.label("VLAN Identification Number");
			var $input = one.lib.form.input("VLAN Identification Number");
			$input.attr('id', one.f.flows.id.modal.form.vlanId);
			var $help = one.lib.form.help("Range: 0 - 4095");
			$fieldset.append($label).append($input).append($help);
			// vlanPriority
			var $label = one.lib.form.label("VLAN Priority");
			var $input = one.lib.form.input("VLAN Priority");
			$input.attr('id', one.f.flows.id.modal.form.vlanPriority);
			var $help = one.lib.form.help("Range: 0 - 7");
			$fieldset.append($label).append($input).append($help);
			// srcMac
			var $label = one.lib.form.label("Source MAC Address");
			var $input = one.lib.form.input("Source MAC Address");
			$input.attr('id', one.f.flows.id.modal.form.srcMac);
			var $help = one.lib.form.help("Example: 00:11:22:aa:bb:cc");
			$fieldset.append($label).append($input).append($help);
			// dstMac
			var $label = one.lib.form.label("Destination MAC Address");
			var $input = one.lib.form.input("Destination MAC Address");
			$input.attr('id', one.f.flows.id.modal.form.dstMac);
			var $help = one.lib.form.help("Example: 00:11:22:aa:bb:cc");
			$fieldset.append($label).append($input).append($help);
			// layer 3
			var $legend = one.lib.form.legend("Layer 3");
			$fieldset.append($legend);
			// srcIp
			var $label = one.lib.form.label("Source IP Address");
			var $input = one.lib.form.input("Source IP Address");
			$input.attr('id', one.f.flows.id.modal.form.srcIp);
			var $help = one.lib.form.help("Example: 127.0.0.1");
			$fieldset.append($label).append($input).append($help);
			// dstIp
			var $label = one.lib.form.label("Destination IP Address");
			var $input = one.lib.form.input("Destination IP Address");
			$input.attr('id', one.f.flows.id.modal.form.dstIp);
			var $help = one.lib.form.help("Example: 127.0.0.1");
			$fieldset.append($label).append($input).append($help);
			// tosBits
			var $label = one.lib.form.label("TOS Bits");
			var $input = one.lib.form.input("TOS Bits");
			$input.attr('id', one.f.flows.id.modal.form.tosBits);
			var $help = one.lib.form.help("Range: 0 - 63");
			$fieldset.append($label).append($input).append($help);
			// layer 4
			var $legend = one.lib.form.legend("Layer 4");
			$fieldset.append($legend);
			// srcPort
			var $label = one.lib.form.label("Source Port");
			var $input = one.lib.form.input("Source Port");
			$input.attr('id', one.f.flows.id.modal.form.srcPort);
			var $help = one.lib.form.help("Range: 1 - 65535");
			$fieldset.append($label).append($input).append($help);
			// dstPort
			var $label = one.lib.form.label("Destination Port");
			var $input = one.lib.form.input("Destination Port");
			$input.attr('id', one.f.flows.id.modal.form.dstPort);
			var $help = one.lib.form.help("Range: 1 - 65535");
			$fieldset.append($label).append($input).append($help);
			// protocol
			var $label = one.lib.form.label("Protocol");
			var $input = one.lib.form.input("Protocol");
			$input.attr('id', one.f.flows.id.modal.form.protocol);
			$fieldset.append($label).append($input);
			// actions
			var $legend = one.lib.form.label("Actions");
			$fieldset.append($legend);
			// actions table
			var tableAttributes = ["table-striped", "table-bordered", "table-condensed", "table-hover", "table-cursor"];
			var $table = one.lib.dashlet.table.table(tableAttributes);
			$table.attr('id', one.f.flows.id.modal.action.table);
			var tableHeaders = ["Action", "Data", "Type"];
		    var $thead = one.lib.dashlet.table.header(tableHeaders);
			var $tbody = one.lib.dashlet.table.body("", tableHeaders);
			$table.append($thead).append($tbody);
			// actions
			var actions = {
			    "" : "Please Select an Action",
			    "drop" : "Drop",
			    "loopback" : "Loopback",
			    "flood" : "Flood",
			    "softwarePath" : "Software Path",
			    "hardwarePath" : "Hardware Path",
			    "controller" : "Controller",
			    "addOutputPorts" : "Add Output Ports",
			    "setVlanId" : "Set VLAN ID",
			    "setVlanPriority" : "Set VLAN Priority",
			    "stripVlanHeader" : "Strip VLAN Header",
			    "modifyDatalayerSourceAddress" : "Modify Datalayer Source Address",
			    "modifyDatalayerDestinationAddress" : "Modify Datalayer Destination Address",
			    "modifyNetworkSourceAddress" : "Modify Network Source Address",
			    "modifyNetworkDestinationAddress" :"Modify Network Destination Address",
			    "modifyTosBits" : "Modify TOS Bits",
			    "modifyTransportSourcePort" : "Modify Transport Source Port",
			    "modifyTransportDestinationPort" : "Modify Transport Destination Port"
			};
            var $select = one.lib.form.select.create(actions);
            // when selecting an action
            $select.change(function() {
                var action = $(this).find('option:selected');
                one.f.flows.modal.action.parse(action.attr('value'));
				$select[0].selectedIndex = 0;
            });
            
			$fieldset.append($select).append($table);
            
			// return
			$form.append($fieldset);
			return $form;
        },
        action : {
            parse : function(option) {
                switch (option) {
                    case "addOutputPorts" :
                        var h3 = "Add Output Port";
                        var $modal = one.f.flows.modal.action.initialize(h3, one.f.flows.modal.action.body.addOutputPorts, one.f.flows.modal.action.add.addOutputPorts);
                        $modal.modal();
                        break;
                    case "setVlanId" :
                        var h3 = "Set VLAN ID";
                        var placeholder = "VLAN Identification Number";
                        var id = one.f.flows.id.modal.action.setVlanId;
                        var help = "Range: 0 - 4095";
                        var action = 'setVlan';
                        var name = "VLAN ID";
                        var body = function() {
                            return one.f.flows.modal.action.body.set(h3, placeholder, id, help);
                        };
                        var add = function($modal) {
                            one.f.flows.modal.action.add.set(name, id, action, $modal);
                        };
                        var $modal = one.f.flows.modal.action.initialize(h3, body, add);
                        $modal.modal();
                        break;
                    case "setVlanPriority" :
                        var h3 = "Set VLAN Priority";
                        var placeholder = "VLAN Priority";
                        var id = one.f.flows.id.modal.action.setVlanPriority;
                        var help = "Range: 0 - 7";
                        var action = 'setVlanPcp';
                        var name = "VLAN Priority";
                        var body = function() {
                            return one.f.flows.modal.action.body.set(h3, placeholder, id, help);
                        };
                        var add = function($modal) {
                            one.f.flows.modal.action.add.set(name, id, action, $modal);
                        };
                        var $modal = one.f.flows.modal.action.initialize(h3, body, add);
                        $modal.modal();
                        break;
                    case "stripVlanHeader" :
                        var name = "Strip VLAN Header";
                        var action = 'stripVlan';
                        one.f.flows.modal.action.add.add(name, action);
                        break;
                    case "modifyDatalayerSourceAddress" :
                        var h3 = "Set Source MAC Address";
                        var placeholder = "Source MAC Address";
                        var id = one.f.flows.id.modal.action.modifyDatalayerSourceAddress;
                        var help = "Example: 00:11:22:aa:bb:cc";
                        var action = 'setDlSrc';
                        var name = "Source MAC";
                        var body = function() {
                            return one.f.flows.modal.action.body.set(h3, placeholder, id, help);
                        };
                        var add = function($modal) {
                            one.f.flows.modal.action.add.set(name, id, action, $modal);
                        };
                        var $modal = one.f.flows.modal.action.initialize(h3, body, add);
                        $modal.modal();
                        break;
                    case "modifyDatalayerDestinationAddress" :
                        var h3 = "Set Destination MAC Address";
                        var placeholder = "Destination MAC Address";
                        var id = one.f.flows.id.modal.action.modifyDatalayerDestinationAddress;
                        var help = "Example: 00:11:22:aa:bb:cc";
                        var action = 'setDlDst';
                        var name = "Destination MAC";
                        var body = function() {
                            return one.f.flows.modal.action.body.set(h3, placeholder, id, help);
                        };
                        var add = function($modal) {
                            one.f.flows.modal.action.add.set(name, id, action, $modal);
                        };
                        var $modal = one.f.flows.modal.action.initialize(h3, body, add);
                        $modal.modal();
                        break;
                    case "modifyNetworkSourceAddress" :
                        var h3 = "Set IP Source Address";
                        var placeholder = "Source IP Address";
                        var id = one.f.flows.id.modal.action.modifyNetworkSourceAddress;
                        var help = "Example: 127.0.0.1";
                        var action = 'setNwSrc';
                        var name = "Source IP";
                        var body = function() {
                            return one.f.flows.modal.action.body.set(h3, placeholder, id, help);
                        };
                        var add = function($modal) {
                            one.f.flows.modal.action.add.set(name, id, action, $modal);
                        };
                        var $modal = one.f.flows.modal.action.initialize(h3, body, add);
                        $modal.modal();
                        break;
                    case "modifyNetworkDestinationAddress" :
                        var h3 = "Set IP Destination Address";
                        var placeholder = "Destination IP Address";
                        var id = one.f.flows.id.modal.action.modifyNetworkDestinationAddress;
                        var help = "Example: 127.0.0.1";
                        var action = 'setNwDst';
                        var name = "Destination IP";
                        var body = function() {
                            return one.f.flows.modal.action.body.set(h3, placeholder, id, help);
                        };
                        var add = function($modal) {
                            one.f.flows.modal.action.add.set(name, id, action, $modal);
                        };
                        var $modal = one.f.flows.modal.action.initialize(h3, body, add);
                        $modal.modal();
                        break;
                    case "modifyTosBits" :
                        var h3 = "Set IPv4 ToS";
                        var placeholder = "IPv4 ToS";
                        var id = one.f.flows.id.modal.action.modifyTosBits;
                        var help = "Range: 0 - 63";
                        var action = 'setNwTos';
                        var name = "TOS Bits";
                        var body = function() {
                            return one.f.flows.modal.action.body.set(h3, placeholder, id, help);
                        };
                        var add = function($modal) {
                            one.f.flows.modal.action.add.set(name, id, action, $modal);
                        };
                        var $modal = one.f.flows.modal.action.initialize(h3, body, add);
                        $modal.modal();
                        break;
                    case "modifyTransportSourcePort" :
                        var h3 = "Set Transport Source Port";
                        var placeholder = "Transport Source Port";
                        var id = one.f.flows.id.modal.action.modifyTransportSourcePort;
                        var help = "Range: 1 - 65535";
                        var action = 'setTpSrc';
                        var name = "Source Port";
                        var body = function() {
                            return one.f.flows.modal.action.body.set(h3, placeholder, id, help);
                        };
                        var add = function($modal) {
                            one.f.flows.modal.action.add.set(name, id, action, $modal);
                        };
                        var $modal = one.f.flows.modal.action.initialize(h3, body, add);
                        $modal.modal();
                        break;
                    case "modifyTransportDestinationPort" :
                        var h3 = "Set Transport Destination Port";
                        var placeholder = "Transport Destination Port";
                        var id = one.f.flows.id.modal.action.modifyTransportDestinationPort;
                        var help = "Range: 1 - 65535";
                        var action = 'setTpDst';
                        var name = "Destination Port";
                        var body = function() {
                            return one.f.flows.modal.action.body.set(h3, placeholder, id, help);
                        };
                        var add = function($modal) {
                            one.f.flows.modal.action.add.set(name, id, action, $modal);
                        };
                        var $modal = one.f.flows.modal.action.initialize(h3, body, add);
                        $modal.modal();
                        break;
                    case "drop" :
                        var name = "Drop";
                        var action = 'drop';
                        one.f.flows.modal.action.add.add(name, action);
                        break;
                    case "loopback" :
                        var name = "Loopback";
                        var action = 'loopback';
                        one.f.flows.modal.action.add.add(name, action);
                        break;
                    case "flood" :
                        var name = "Flood";
                        var action = 'flood';
                        one.f.flows.modal.action.add.add(name, action);
                        break;
                    case "softwarePath" :
                        var name = "Software Path";
                        var action = 'software path';
                        one.f.flows.modal.action.add.add(name, action);
                        break;
                    case "hardwarePath" :
                        var name = "Hardware Path";
                        var action = 'hardware path';
                        one.f.flows.modal.action.add.add(name, action);
                        break;
                    case "controller" :
                        var name = "Controller";
                        var action = 'controller';
                        one.f.flows.modal.action.add.add(name, action);
                        break;
                }
            },
            initialize : function(h3, bodyCallback, addCallback) {
                var footer = one.f.flows.modal.action.footer();
                var $body = bodyCallback();
                var $modal = one.lib.modal.spawn(one.f.flows.id.modal.action.modal, h3, $body, footer);
                // bind close button
                $('#'+one.f.flows.id.modal.action.close, $modal).click(function() {
                    $modal.modal('hide');
                });
                // bind add flow button
                $('#'+one.f.flows.id.modal.action.add, $modal).click(function() {
                    addCallback($modal);
                });
                return $modal;
            },
            add : {
                addOutputPorts : function($modal) {
                    var $options = $('#'+one.f.flows.id.modal.action.addOutputPorts).find('option:selected');
                    var ports = '';
                    var pid = '';
                    $options.each(function(index, value) {
                        ports = ports+$(value).text()+", ";
                        pid = pid+$(value).attr('value')+",";
                    });
                    ports = ports.slice(0,-2);
                    pid = pid.slice(0,-1);
                    var $tr = one.f.flows.modal.action.table.add("Add Output Ports", ports);
                    $tr.attr('id', 'addOutputPorts');
                    $tr.data('action', 'OUTPUT='+pid);
					$tr.click(function() {
						one.f.flows.modal.action.add.modal.initialize(this);
					});
                    one.f.flows.modal.action.table.append($tr);
                    $modal.modal('hide');
                },
                add : function(name, action) {
                    var $tr = one.f.flows.modal.action.table.add(name);
                    $tr.attr('id', action);
                    $tr.data('action', action);
					$tr.click(function() {
						one.f.flows.modal.action.add.modal.initialize(this);
					});
                    one.f.flows.modal.action.table.append($tr);
                },
                set : function(name, id, action, $modal) {
                    var $input = $('#'+id);
                    var value = $input.val();
                    var $tr = one.f.flows.modal.action.table.add(name, value);
                    $tr.attr('id', action);
                    $tr.data('action', action+'='+value);
					$tr.click(function() {
						one.f.flows.modal.action.add.modal.initialize(this);
					});
                    one.f.flows.modal.action.table.append($tr);
                    $modal.modal('hide');
                },
				remove : function(that) {
					$(that).remove();
					var $table = $('#'+one.f.flows.id.modal.action.table);
					if ($table.find('tbody').find('tr').size() == 0) {
						var $tr = $(document.createElement('tr'));
						var $td = $(document.createElement('td'));
						$td.attr('colspan', '3');
						$tr.addClass('empty');
						$td.text('No data available');
						$tr.append($td);
						$table.find('tbody').append($tr);
					}
				},
				modal : {
					initialize : function(that) {
						var h3 = "Remove Action";
						var footer = one.f.flows.modal.action.add.modal.footer();
						var $body = one.f.flows.modal.action.add.modal.body();
						var $modal = one.lib.modal.spawn(one.f.flows.id.modal.action.modal.modal, h3, $body, footer);

						// bind cancel button
						$('#'+one.f.flows.id.modal.action.modal.cancel, $modal).click(function() {
							$modal.modal('hide');
						});

						// bind remove button
						$('#'+one.f.flows.id.modal.action.modal.remove, $modal).click(function() {
							one.f.flows.modal.action.add.remove(that);
							$modal.modal('hide');
						});

						$modal.modal();
					},
					body : function() {
						var $p = $(document.createElement('p'));
						$p.append("Remove this action?");
						return $p;
					},
					footer : function() {
						var footer = [];

						var removeButton = one.lib.dashlet.button.single("Remove Action", one.f.flows.id.modal.action.modal.remove, "btn-danger", "");
						var $removeButton = one.lib.dashlet.button.button(removeButton);
						footer.push($removeButton);

						var cancelButton = one.lib.dashlet.button.single("Cancel", one.f.flows.id.modal.action.modal.cancel, "", "");
						var $cancelButton = one.lib.dashlet.button.button(cancelButton);
						footer.push($cancelButton);

						return footer;
					}
				}
            },
            table : {
                add : function(action, data, type) {
                    var $tr = $(document.createElement('tr'));
                    var $td = $(document.createElement('td'));
                    $td.append(action);
                    $tr.append($td);
                    var $td = $(document.createElement('td'));
                    if (data != undefined) $td.append(data);
                    $tr.append($td);
                    var $td = $(document.createElement('td'));
                    if (type != undefined) $td.append(type);
                    $tr.append($td);
                    return $tr;
                },
                append : function($tr) {
                    var $table = $('#'+one.f.flows.id.modal.action.table);
                    var $empty = $table.find('.empty').parent();
                    if ($empty.size() > 0) $empty.remove();
                    $table.append($tr);
                }
            },
            body : {
                common : function() {
                    var $form = $(document.createElement('form'));
                    var $fieldset = $(document.createElement('fieldset'));
                    return [$form, $fieldset];
                },
                addOutputPorts : function() {
                    var common = one.f.flows.modal.action.body.common();
                    var $form = common[0];
                    var $fieldset = common[1];
                    // output port
                    $label = one.lib.form.label("Select Output Ports");
                    var ports = one.f.flows.registry.nodeports[one.f.flows.registry.currentNode]['ports'];
                    $select = one.lib.form.select.create(ports, true);
                    $select.attr('id', one.f.flows.id.modal.action.addOutputPorts);
                    one.lib.form.select.prepend($select, {'':'Select a Port'});
                    $fieldset.append($label).append($select);
                    $form.append($fieldset);
                    return $form;
                },
                set : function(label, placeholder, id, help) {
                    var common = one.f.flows.modal.action.body.common();
                    var $form = common[0];
                    var $fieldset = common[1];
                    // input
                    $label = one.lib.form.label(label);
                    $input = one.lib.form.input(placeholder);
                    $input.attr('id', id);
                    $help = one.lib.form.help(help);
                    // append
                    $fieldset.append($label).append($input).append($help);
                    $form.append($fieldset);
                    return $form;
                }
            },
            footer : function() {
                var footer = [];
                var addButton = one.lib.dashlet.button.single("Add Action", one.f.flows.id.modal.action.add, "btn-primary", "");
                var $addButton = one.lib.dashlet.button.button(addButton);
                footer.push($addButton);
                
                var closeButton = one.lib.dashlet.button.single("Close", one.f.flows.id.modal.action.close, "", "");
                var $closeButton = one.lib.dashlet.button.button(closeButton);
                footer.push($closeButton);
                
                return footer;
            }
        },
        footer : function() {
            var footer = [];

			var installButton = one.lib.dashlet.button.single("Install Flow", one.f.flows.id.modal.install, "btn-success", "");
			var $installButton = one.lib.dashlet.button.button(installButton);
			footer.push($installButton);
            
            var addButton = one.lib.dashlet.button.single("Save Flow", one.f.flows.id.modal.add, "btn-primary", "");
            var $addButton = one.lib.dashlet.button.button(addButton);
            footer.push($addButton);
            
            var closeButton = one.lib.dashlet.button.single("Close", one.f.flows.id.modal.close, "", "");
            var $closeButton = one.lib.dashlet.button.button(closeButton);
            footer.push($closeButton);
            
            return footer;
        }
    },
    ajax : {
        dashlet : function(callback) {
            $.getJSON(one.f.address.root+one.f.address.flows.main, function(data) {
                one.f.flows.registry['flows'] = data;
                var body = one.f.flows.data.dashlet(data);
                var $body = one.f.flows.body.dashlet(body, callback);
                callback($body);
            });
        }
    },
    data : {
        dashlet : function(data) {
            var body = [];
            $(data).each(function(index, value) {
                var tr = {};
                var entry = [];
                entry.push(value['name']);
                entry.push(value['node']);
                if (value['flow']['installInHw'] == 'true')
                	tr['type'] = ['success'];
                else if (value['flow']['installInHw'] == 'false')
                	tr['type'] = ['warning'];
                tr['entry'] = entry;
                tr['id'] = value['nodeId'];
                
                body.push(tr);
            });
            return body;
        }
    },
    body : {
        dashlet : function(body, callback) {
            var attributes = ['table-striped', 'table-bordered', 'table-hover', 'table-condensed', 'table-cursor'];
            var $table = one.lib.dashlet.table.table(attributes);
            
            var headers = ['Flow Name', 'Node'];
            var $thead = one.lib.dashlet.table.header(headers);
            $table.append($thead);
            
            var $tbody = one.lib.dashlet.table.body(body);
            $table.append($tbody);
            
            return $table;
        }
    }
}

/** INIT **/
// populate nav tabs
$(one.f.menu.left.top).each(function(index, value) {
    var $nav = $(".nav", "#left-top");
    one.main.page.dashlet($nav, value);
});

$(one.f.menu.left.bottom).each(function(index, value) {
    var $nav = $(".nav", "#left-bottom");
    one.main.page.dashlet($nav, value);
});

$(one.f.menu.right.bottom).each(function(index, value) {
    var $nav = $(".nav", "#right-bottom");
    one.main.page.dashlet($nav, value);
});

one.f.populate = function($dashlet, header) {
    var $h4 = one.lib.dashlet.header(header);
    $dashlet.append($h4);
};

// bind dashlet nav
$('.dash .nav a', '#main').click(function() {
    // de/activation
    var $li = $(this).parent();
    var $ul = $li.parent();
    one.lib.nav.unfocus($ul);
    $li.addClass('active');
    // clear respective dashlet
    var $dashlet = $ul.parent().find('.dashlet');
    one.lib.dashlet.empty($dashlet);
    // callback based on menu
    var id = $(this).attr('id');
    var menu = one.f.dashlet;
    switch (id) {
        case menu.flows.id:
            one.f.flows.dashlet($dashlet);
            break;
        case menu.nodes.id:
            one.f.nodes.dashlet($dashlet);
            break;
        case menu.detail.id:
            one.f.detail.dashlet($dashlet);
            break;
    };
});

// activate first tab on each dashlet
$('.dash .nav').each(function(index, value) {
    $($(value).find('li')[0]).find('a').click();
});
