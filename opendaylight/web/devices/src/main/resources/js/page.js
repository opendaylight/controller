/* 
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved. 
 * 
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

//PAGE Devices
one.f = {};

// specify dashlets and layouts

one.f.dashlet = {
	nodesLearnt : {
		id : 'nodesLearnt',
	    name : 'Nodes Learnt'
	},
    staticRouteConfig : {
        id : 'staticRouteConfig',
        name : 'Static route Configuration'
    },
    subnetGatewayConfig : {
        id : 'subnetGatewayConfig',
        name : 'Subnet Gateway Configuration'
    },
    spanPortConfig : {
        id : 'spanPortConfig',
        name : 'SPAN Port Configuration'
    }
};

one.f.menu = {
    left : {
        top : [
            one.f.dashlet.nodesLearnt
        ],
        bottom : [
            one.f.dashlet.staticRouteConfig
        ]
    },
    right : {
        top : [],
        bottom : [
            one.f.dashlet.subnetGatewayConfig,
            one.f.dashlet.spanPortConfig
        ]
    }
};

/**Devices Modules */
one.f.switchmanager = {
	rootUrl: "controller/web/devices",
	createTable: function(columnNames, body) {
		var tableAttributes = ["table-striped", "table-bordered", "table-condensed"];
		var $table = one.lib.dashlet.table.table(tableAttributes);
		var tableHeaders = columnNames;
		var $thead = one.lib.dashlet.table.header(tableHeaders);
		var $tbody = one.lib.dashlet.table.body(body, tableHeaders);
		$table.append($thead)
		.append($tbody);
		return $table;
	},
	validateName: function(name) {
		return name.match(/^[a-zA-Z0-9][a-zA-Z0-9_\-\.]{1,31}$/g) == null ? false : true;
	}
};

one.f.switchmanager.nodesLearnt = {
	id: {
		dashlet: {
			popout: "one_f_switchmanager_nodesLearnt_id_dashlet_popout"
		},
		modal: {
			modal: "one_f_switchmanager_nodesLearnt_id_modal_modal",
			save: "one_f_switchmanager_nodesLearnt_id_modal_save",
			form: {
				nodeId: "one_f_switchmanager_nodesLearnt_id_modal_form_nodeid",
				nodeName : "one_f_switchmanager_nodesLearnt_id_modal_form_nodename",
				tier: "one_f_switchmanager_nodesLearnt_id_modal_form_tier",
				operationMode: "one_f_switchmanager_nodesLearnt_id_modal_form_opmode"
			}
		}
	},
	dashlet: function($dashlet) {
		var url = one.f.switchmanager.rootUrl + "/nodesLearnt";
		one.lib.dashlet.empty($dashlet);
		$dashlet.append(one.lib.dashlet.header(one.f.dashlet.nodesLearnt.name));

		one.f.switchmanager.nodesLearnt.ajax.main(url, function(content) {
			var body = one.f.switchmanager.nodesLearnt.data.abridged(content);
			var $table = one.f.switchmanager.createTable(["Node Name", "Node ID", "Ports"], body);
			$dashlet.append($table);
		});
	},

	ajax : {
		main : function(url, callback) {
			$.getJSON(url, function(data) {
				callback(data);
			});
		}
	},
	modal : {
		initialize: {
			updateNode: function(evt) {
				var nodeId = decodeURIComponent(evt.target.id);
				var h3 = "Update node information";
	            var footer = one.f.switchmanager.nodesLearnt.modal.footer.updateNode();
	            var $modal = one.lib.modal.spawn(one.f.switchmanager.nodesLearnt.id.modal.modal, h3, "", footer);
	            
	            // bind save button
	            $('#' + one.f.switchmanager.nodesLearnt.id.modal.save, $modal).click(function() {
	            	one.f.switchmanager.nodesLearnt.modal.save($modal);
	            });
	            // inject body (nodePorts)
	            one.f.switchmanager.nodesLearnt.ajax.main(one.f.switchmanager.rootUrl + "/tiers", function(tiers) {
	                var $body = one.f.switchmanager.nodesLearnt.modal.body.updateNode(nodeId, evt.target.switchDetails, tiers);
	                one.lib.modal.inject.body($modal, $body);
	                $modal.modal();
	            });
			},
			popout: function() {
				var h3 = "Nodes Learnt";
	            var footer = one.f.switchmanager.nodesLearnt.modal.footer.popout();
	            var $modal = one.lib.modal.spawn(one.f.switchmanager.nodesLearnt.id.modal.modal, h3, "", footer);
	            var $body = one.f.switchmanager.nodesLearnt.modal.body.popout($modal);
	            return $modal;
			}
		},
		body: {
			updateNode: function(nodeId, switchDetails, tiers) {
				var $form = $(document.createElement('form'));
				var $fieldset = $(document.createElement('fieldset'));
				// node ID. not editable.
				var $label = one.lib.form.label("Node ID");
				var $input = one.lib.form.input("node id");
				$input.attr('id', one.f.switchmanager.nodesLearnt.id.modal.form.nodeId);
				$input.attr("disabled", true);
				$input.attr("value", nodeId);
				$fieldset.append($label).append($input);
				// node name
				var $label = one.lib.form.label("Node Name");
				var $input = one.lib.form.input("Node Name");
				$input.attr('id', one.f.switchmanager.nodesLearnt.id.modal.form.nodeName);
				if(switchDetails["nodeName"] != null) {
					$input.attr('value', switchDetails["nodeName"]);
				}
				$fieldset.append($label).append($input);
				// node tier
				var $label = one.lib.form.label("Tier");
				var $select = one.lib.form.select.create(tiers);
				$select.attr('id', one.f.switchmanager.nodesLearnt.id.modal.form.tier);
				$select.val(switchDetails["tier"]);
				$fieldset.append($label).append($select);
				// operation mode
				var $label = one.lib.form.label("Operation Mode");
				var $select = one.lib.form.select.create(
						["Allow reactive forwarding", "Proactive forwarding only"]);
				$select.attr('id', one.f.switchmanager.nodesLearnt.id.modal.form.operationMode);
				$select.val(switchDetails["mode"]);
				$fieldset.append($label).append($select);
				
				$form.append($fieldset);
				return $form;
			},
			popout: function($modal) {
				var url = one.f.switchmanager.rootUrl + "/nodesLearnt";
				one.f.switchmanager.nodesLearnt.ajax.main(url, function(content) {
					var tableContent = one.f.switchmanager.nodesLearnt.data.popout(content);
					var $table = one.f.switchmanager.createTable(content.columnNames, tableContent);
					one.lib.modal.inject.body($modal, $table);
				});
			}
		},
		save: function($modal) {
			var result = {};
            result['nodeName'] = $('#' + one.f.switchmanager.nodesLearnt.id.modal.form.nodeName, $modal).val();
            if(!one.f.switchmanager.validateName(result['nodeName'])) {
            	alert("Node name can contain alphabets numbers and characters _ - . upto 32 characters in length");
            	return;
            }
            result['nodeId'] = $('#' + one.f.switchmanager.nodesLearnt.id.modal.form.nodeId, $modal).val();
            result['tier'] = $('#' + one.f.switchmanager.nodesLearnt.id.modal.form.tier, $modal).val();
            result['operationMode'] = $('#' + one.f.switchmanager.nodesLearnt.id.modal.form.operationMode, $modal).val();
            one.f.switchmanager.nodesLearnt.modal.ajax(result, 
			  	function(response) {
            		if(response.status == true) {
            			$modal.modal('hide');
            			one.topology.update(); // refresh visual topology with new name
            			// TODO: Identify dashlet by inserting a nodesLearnt div 
           			 	// in the dashlet() instead
            			one.f.switchmanager.nodesLearnt.dashlet($("#left-top .dashlet"));
            		} else {
            			alert(response.message);
            		}
	                
	            });
		},
		ajax: function(requestData, callback) {
			$.getJSON(one.f.switchmanager.rootUrl + "/nodesLearnt/update", requestData, function(response) {
				callback(response);
			});
		},
		footer: {
			updateNode: function() {
				var footer = [];
				if(one.role < 2) {
					var saveButton = one.lib.dashlet.button.single("Save", one.f.switchmanager.nodesLearnt.id.modal.save, "btn-success", "");
		            var $saveButton = one.lib.dashlet.button.button(saveButton);
		            footer.push($saveButton);
				}
	            return footer;
			},
			popout: function() {
				// TODO: Maybe put a close button in the footer?
				return [];
			}
		}
	},
	// data functions
	data : {
		abridged : function(data) {
			var result = [];
			$.each(data.nodeData, function(key, value) {
				var tr = {};
				var entry = [];
				var nodenameentry = value["nodeName"] ? value["nodeName"] : "Click to update";
				// TODO: Move anchor tag creation to one.lib.form.
				var aTag = document.createElement("a");
				aTag.setAttribute("id", encodeURIComponent(value["nodeId"]));
				aTag.switchDetails = value;
				aTag.addEventListener("click", one.f.switchmanager.nodesLearnt.modal.initialize.updateNode);
				aTag.addEventListener("mouseover", function(evt) {
					evt.target.style.cursor = "pointer";
				}, false);
				aTag.innerHTML = nodenameentry;
				entry.push(aTag);
				entry.push(value["nodeId"]);
				entry.push(value["ports"]);
				tr.entry = entry;
				result.push(tr);
			});
			return result;
		},
		popout : function(data) {
			var result = [];
			$.each(data.nodeData, function(key, value) {
				var tr = {};
				// fill up all the td's
				var entry = [];
				var nodenameentry = value["nodeName"] ? value["nodeName"] : "No name provided";
				entry.push(nodenameentry);
				entry.push(value["nodeId"]);
				entry.push(value["tierName"]);
				entry.push(value["mac"]);
				entry.push(value["ports"]);
				tr.entry = entry;
				result.push(tr);
			});
			return result;
		}
	}
};

one.f.switchmanager.subnetGatewayConfig = {
	id: {
		dashlet: {
			addIPAddress: "one_f_switchmanager_subnetGatewayConfig_id_dashlet_addIP",
			addPorts: "one_f_switchmanager_subnetGatewayConfig_id_dashlet_addPorts",
			removeIPAddress: "one_f_switchmanager_subnetGatewayConfig_id_dashlet_removeIP"
		}, 
		modal: {
			modal: "one_f_switchmanager_subnetGatewayConfig_id_modal_modal",
			save: "one_f_switchmanager_subnetGatewayConfig_id_modal_save",
			form: {
				name : "one_f_switchmanager_subnetGatewayConfig_id_modal_form_gatewayname",
				gatewayIPAddress : "one_f_switchmanager_subnetGatewayConfig_id_modal_form_gatewayipaddress",
				nodeId: "one_f_switchmanager_subnetGatewayConfig_id_modal_form_nodeid",
				ports: "one_f_switchmanager_subnetGatewayConfig_id_modal_form_ports"
			}
		}
	},
	// device ajax calls
	dashlet: function($dashlet) {
		one.lib.dashlet.empty($dashlet);
		$dashlet.append(one.lib.dashlet.header(one.f.dashlet.subnetGatewayConfig.name));
		// Add gateway IP Address button
		if(one.role < 2) {
			var button = one.lib.dashlet.button.single("Add Gateway IP Address", 
					one.f.switchmanager.subnetGatewayConfig.id.dashlet.addIPAddress, "btn-primary", "btn-mini");
	        var $button = one.lib.dashlet.button.button(button);
	        $button.click(function() {
	        	var $modal = one.f.switchmanager.subnetGatewayConfig.modal.initialize.gateway();
	            $modal.modal();
	        });
			$dashlet.append($button);
		
		
			// Delete gateway ip address button
	        var button = one.lib.dashlet.button.single("Delete Gateway IP Address(es)", 
	        		one.f.switchmanager.subnetGatewayConfig.id.dashlet.removeIPAddress, "btn-primary", "btn-mini");
	        var $button = one.lib.dashlet.button.button(button);
	        $button.click(function() {
	        	var requestData = {};
	        	var gatewaysToDelete = [];
	        	var checkedCheckBoxes = $("input:checked", $(this).closest(".dashlet").find("table"));
	        	checkedCheckBoxes.each(function(index, value) {
	        		gatewaysToDelete.push(checkedCheckBoxes[index].id);
	        	}); 
	        	if(gatewaysToDelete.length > 0) {
	        		requestData["gatewaysToDelete"] = gatewaysToDelete.toString();
	            	var url = one.f.switchmanager.rootUrl + "/subnetGateway/delete";
	        		one.f.switchmanager.subnetGatewayConfig.ajax.main(url, requestData, function(response) {
	        			if(response.status == true) {
	        				// refresh dashlet by passing dashlet div as param
	                    	one.f.switchmanager.subnetGatewayConfig.dashlet($("#right-bottom .dashlet"));
	        			} else {
	        				alert(response.message);
	        			}
	        		});
	        	} 
	        });
	        $dashlet.append($button);
	
			// Add Ports button
			var button = one.lib.dashlet.button.single("Add Ports", 
					one.f.switchmanager.subnetGatewayConfig.id.dashlet.addPorts, "btn-primary", "btn-mini");
	        var $button = one.lib.dashlet.button.button(button);
	        $button.click(function() {
	        	var $modal = one.f.switchmanager.subnetGatewayConfig.modal.initialize.ports();
	            $modal.modal();
	        });
			$dashlet.append($button);
		}
		var url = one.f.switchmanager.rootUrl + "/subnets";
		one.f.switchmanager.subnetGatewayConfig.ajax.main(url, {}, function(content) {
			var body = one.f.switchmanager.subnetGatewayConfig.data.devices(content);
			// first column contains checkbox. no need for header
			content.columnNames.splice(0,0," ");
			var $table = one.f.switchmanager.createTable(content.columnNames, body);
			$dashlet.append($table);
		});
	},
	ajax : {
		main : function(url, requestData, callback) {
			$.getJSON(url, requestData, function(data) {
				callback(data);
			});
		}
	},
	registry: {},
	modal : {
		initialize: {
			gateway: function() {
				var h3 = "Add Gateway IP Address";
	            var footer = one.f.switchmanager.subnetGatewayConfig.modal.footer();
	            var $modal = one.lib.modal.spawn(one.f.switchmanager.subnetGatewayConfig.id.modal.modal, h3, "", footer);
	            // bind save button
	            $('#' + one.f.switchmanager.subnetGatewayConfig.id.modal.save, $modal).click(function() {
	            	one.f.switchmanager.subnetGatewayConfig.modal.save.gateway($modal);
	            });
	            var $body = one.f.switchmanager.subnetGatewayConfig.modal.body.gateway();
	            one.lib.modal.inject.body($modal, $body);
	            return $modal;
			},
			ports: function() {
				var h3 = "Add Ports";
	            var footer = one.f.switchmanager.subnetGatewayConfig.modal.footer();
	            var $modal = one.lib.modal.spawn(one.f.switchmanager.subnetGatewayConfig.id.modal.modal, h3, "", footer);
	            // bind save button
	            $('#' + one.f.switchmanager.subnetGatewayConfig.id.modal.save, $modal).click(function() {
	            	one.f.switchmanager.subnetGatewayConfig.modal.save.ports($modal);
	            });
	            
	            // TODO: Change to subnetGateway instead.
	            one.f.switchmanager.spanPortConfig.modal.ajax.nodes(function(nodes, nodeports) {
	                var $body = one.f.switchmanager.subnetGatewayConfig.modal.body.ports(nodes, nodeports);
	                one.lib.modal.inject.body($modal, $body);
	            });
	            return $modal;
			}
		},
		save: {
			gateway: function($modal) {
				var result = {};
	            result['gatewayName'] = $('#' + one.f.switchmanager.subnetGatewayConfig.id.modal.form.name, $modal).val();
	            if(!one.f.switchmanager.validateName(result['gatewayName'])) {
	            	alert("Gateway name can contain alphabets numbers and characters _ - . upto 32 characters in length");
	            	return;
	            }
	            result['gatewayIPAddress'] = $('#' + one.f.switchmanager.subnetGatewayConfig.id.modal.form.gatewayIPAddress, $modal).val();
	            one.f.switchmanager.subnetGatewayConfig.modal.ajax.gateway(result, 
				  	function(response) {
	            		if(response.status == true) {
	            			$modal.modal('hide');
	            			one.f.switchmanager.subnetGatewayConfig.dashlet($("#right-bottom .dashlet"));
	            		} else {
	            			alert(response.message);
	            		}
		            });
			},
			ports: function($modal) {
				var result = {};
				var gatewayRegistryIndex = $('#' + one.f.switchmanager.subnetGatewayConfig.id.modal.form.name, $modal).val();
	            result['portsName'] = one.f.switchmanager.subnetGatewayConfig.registry.gateways[gatewayRegistryIndex];
	            result['nodeId'] = $('#' + one.f.switchmanager.subnetGatewayConfig.id.modal.form.nodeId, $modal).val();
	            result['ports'] = $('#' + one.f.switchmanager.subnetGatewayConfig.id.modal.form.ports, $modal).val();
	            if(!result['portsName'] || result['portsName'] == "") {
	            	alert("No gateway chosen. Cannot add port");
	            	return;
	            }
	            if(!result['nodeId'] || result['nodeId'] == "") {
	            	alert("Please select a node.");
	            	return;
	            }
	            if(!result['ports'] || result['ports'] == "") {
	            	alert("Please choose a port.");
	            	return;
	            }
	            one.f.switchmanager.subnetGatewayConfig.modal.ajax.ports(result, 
				  	function(response) {
	            		if(response.status == true) {
	            			$modal.modal('hide');
	            			one.f.switchmanager.subnetGatewayConfig.dashlet($("#right-bottom .dashlet"));
	            		} else {
	            			alert(response.message);
	            		}
		        	});
			}
		},
		body: {
			gateway: function() {
				var $form = $(document.createElement('form'));
				var $fieldset = $(document.createElement('fieldset'));
				// gateway name
				var $label = one.lib.form.label("Name");
				var $input = one.lib.form.input("Name");
				$input.attr('id', one.f.switchmanager.subnetGatewayConfig.id.modal.form.name);
				$fieldset.append($label).append($input);
				// gateway IP Mask 
				var $label = one.lib.form.label("Gateway IP Address/Mask");
				var $input = one.lib.form.input("Gateway IP Address/Mask");
				$input.attr('id', one.f.switchmanager.subnetGatewayConfig.id.modal.form.gatewayIPAddress);
				$fieldset.append($label).append($input);
				
				$form.append($fieldset);
				return $form;
			},
			ports: function(nodes, nodeports) {
				var $form = $(document.createElement('form'));
				var $fieldset = $(document.createElement('fieldset'));
				// gateways drop down
				var $label = one.lib.form.label("Gateway Name");
				var $select = one.lib.form.select.create(one.f.switchmanager.subnetGatewayConfig.registry.gateways);
				$select.attr('id', one.f.switchmanager.subnetGatewayConfig.id.modal.form.name);
				$select.val($select.find("option:first").val());
				$fieldset.append($label).append($select);

				// node ID
				var $label = one.lib.form.label("Node ID");
				var $select = one.lib.form.select.create(nodes);
				$select.attr('id', one.f.switchmanager.subnetGatewayConfig.id.modal.form.nodeId);
				one.lib.form.select.prepend($select, { '' : 'Please Select a Node' });
				$select.val($select.find("option:first").val());
				$fieldset.append($label).append($select);

				// bind onchange
				$select.change(function() {
				    // retrieve port value
				    var node = $(this).find('option:selected').attr('value');
				    one.f.switchmanager.subnetGatewayConfig.registry['currentNode'] = node;
				    var $ports = $('#' + one.f.switchmanager.subnetGatewayConfig.id.modal.form.ports);
				    var ports = nodeports[node];
				    one.lib.form.select.inject($ports, ports);
				    one.lib.form.select.prepend($ports, { '' : 'Please Select a Port' });
				    $ports.val($ports.find("option:first").val());
				});

				// ports
				var $label = one.lib.form.label("Select Port");
				var $select = one.lib.form.select.create();
				$select.attr('id', one.f.switchmanager.subnetGatewayConfig.id.modal.form.ports);
				$fieldset.append($label).append($select);
				
				$form.append($fieldset);
				return $form;
			}
		},
		ajax: {
			gateway: function(requestData, callback) {
				$.getJSON(one.f.switchmanager.rootUrl + "/subnetGateway/add", requestData, function(data) {
					callback(data);
			});
			},
			ports: function(requestData, callback) {
				$.getJSON(one.f.switchmanager.rootUrl + "/subnetGateway/ports/add", requestData, function(data) {
					callback(data);
			});
			}
		},
		footer : function() {
            var footer = [];
            if(one.role < 2) {
            	 var saveButton = one.lib.dashlet.button.single("Save", one.f.switchmanager.subnetGatewayConfig.id.modal.save, "btn-success", "");
                 var $saveButton = one.lib.dashlet.button.button(saveButton);
                 footer.push($saveButton);
            }
            return footer;
        }
	},
	// data functions
	data : {
		devices : function(data) {
			var result = [];
			one.f.switchmanager.subnetGatewayConfig.registry.gateways = [];
			$.each(data.nodeData, function(key, value) {
				var tr = {};
				// fill up all the td's
				var subnetConfigObject = $.parseJSON(value["json"]);
				var nodePorts = subnetConfigObject.nodePorts;
				var $nodePortsContainer = $(document.createElement("div"));
				
				for(var i = 0; i < nodePorts.length; i++) {
					var nodePort = nodePorts[i];
					$nodePortsContainer.append(nodePort + " ");
					// add delete anchor tag to delete ports
					var aTag = document.createElement("a");
					aTag.setAttribute("id", encodeURIComponent(nodePort));
					aTag.gatewayName = value["name"];
					aTag.addEventListener("click", function(evt) {
						var htmlPortAnchor = evt.target;
						var requestData = {};
						requestData["gatewayName"] = evt.target.gatewayName;
						requestData["nodePort"] = decodeURIComponent(evt.target.id);
						// make ajax call to delete port
						var url = one.f.switchmanager.rootUrl + "/subnetGateway/ports/delete";
		        		one.f.switchmanager.subnetGatewayConfig.ajax.main(url, requestData, function(response) {
		        			if(response.status == true) {
		        				// refresh dashlet by passing dashlet div as param
		                    	one.f.switchmanager.subnetGatewayConfig.dashlet($("#right-bottom .dashlet"));
		        			} else {
		        				alert(response.message);
		        			}
		        		});
						
					});
					aTag.addEventListener("mouseover", function(evt) {
						evt.target.style.cursor = "pointer";
					}, false);
					aTag.innerHTML = "Delete";
					$nodePortsContainer.append(aTag);
					$nodePortsContainer.append("<br/>");
				}

				// store gateways in the registry so that they can be used in the add ports popup
				one.f.switchmanager.subnetGatewayConfig.registry.gateways.push(value["name"]);
				var entry = [];
				var checkbox = document.createElement("input");
				checkbox.setAttribute("type", "checkbox");
				checkbox.setAttribute("id", value["name"]);
				entry.push(checkbox);
				entry.push(value["name"]);
				entry.push(value["subnet"]);
				entry.push($nodePortsContainer);
				tr.entry = entry;
				result.push(tr);
			});
			return result;
		}
	}
}

one.f.switchmanager.staticRouteConfig = {
	id: {
		dashlet: {
			add: "one_f_switchmanager_staticRouteConfig_id_dashlet_add",
			remove: "one_f_switchmanager_staticRouteConfig_id_dashlet_remove"
		}, 
		modal: {
			modal: "one_f_switchmanager_staticRouteConfig_id_modal_modal",
			save: "one_f_switchmanager_staticRouteConfig_id_modal_save",
			form: {
				routeName : "one_f_switchmanager_staticRouteConfig_id_modal_form_routename",
				staticRoute : "one_f_switchmanager_staticRouteConfig_id_modal_form_staticroute",
                nextHop : "one_f_switchmanager_staticRouteConfig_id_modal_form_nexthop",
			}
		}
	},
	dashlet: function($dashlet) {
		one.lib.dashlet.empty($dashlet);
		
		if(one.role < 2) {
			// Add static route button
			var button = one.lib.dashlet.button.single("Add Static Route", 
					one.f.switchmanager.staticRouteConfig.id.dashlet.add, "btn-primary", "btn-mini");
	        var $button = one.lib.dashlet.button.button(button);
	        $button.click(function() {
	        	var $modal = one.f.switchmanager.staticRouteConfig.modal.initialize();
	            $modal.modal();
	        });
	        $dashlet.append(one.lib.dashlet.header(one.f.dashlet.staticRouteConfig.name));
	        $dashlet.append($button);
	        
	        // Delete static route button
	        var button = one.lib.dashlet.button.single("Delete Static Route(s)", 
	        		one.f.switchmanager.staticRouteConfig.id.dashlet.remove, "btn-primary", "btn-mini");
	        var $button = one.lib.dashlet.button.button(button);
	        $button.click(function() {
	        	var requestData = {};
	        	var routesToDelete = [];
	        	var checkedCheckBoxes = $("input:checked", $(this).closest(".dashlet").find("table"));
	        	checkedCheckBoxes.each(function(index, value) {
	        		routesToDelete.push(checkedCheckBoxes[index].id);
	        	}); 
	        	if(routesToDelete.length > 0) {
	        		requestData["routesToDelete"] = routesToDelete.toString();
	            	var url = one.f.switchmanager.rootUrl + "/staticRoute/delete";
	        		one.f.switchmanager.staticRouteConfig.ajax.main(url, requestData, function(response) {
	        			if(response.status == true) {
	        				// refresh dashlet by passing dashlet div as param
	                    	one.f.switchmanager.staticRouteConfig.dashlet($("#left-bottom .dashlet"));
	        			} else {
	        				alert(response.message);
	        			}
	        		});
	        	} 
	        });
	        $dashlet.append($button);
		}
		
		var url = one.f.switchmanager.rootUrl + "/staticRoutes";
		one.f.switchmanager.staticRouteConfig.ajax.main(url, {}, function(content) {
			var body = one.f.switchmanager.staticRouteConfig.data.staticRouteConfig(content);
			// first column contains checkbox. no need for header
			content.columnNames.splice(0,0," ");
			var $table = one.f.switchmanager.createTable(content.columnNames, body);
			$dashlet.append($table);
		});
	},
	// device ajax calls
	ajax : {
		main : function(url, requestData, callback) {
			$.getJSON(url, requestData, function(data) {
				callback(data);
			});
		}
	},
	registry: {},
	modal : {
		initialize: function() {
			var h3 = "Add Static Route";
            var footer = one.f.switchmanager.staticRouteConfig.modal.footer();
            var $modal = one.lib.modal.spawn(one.f.switchmanager.staticRouteConfig.id.modal.modal, h3, "", footer);
            // bind save button
            $('#' + one.f.switchmanager.staticRouteConfig.id.modal.save, $modal).click(function() {
            	one.f.switchmanager.staticRouteConfig.modal.save($modal);
            });
            var $body = one.f.switchmanager.staticRouteConfig.modal.body();
            one.lib.modal.inject.body($modal, $body);
            return $modal;
		},
		save: function($modal) {
			var result = {};
            result['routeName'] = $('#' + one.f.switchmanager.staticRouteConfig.id.modal.form.routeName, $modal).val();
            result['staticRoute'] = $('#' + one.f.switchmanager.staticRouteConfig.id.modal.form.staticRoute, $modal).val();
            result['nextHop'] = $('#' + one.f.switchmanager.staticRouteConfig.id.modal.form.nextHop, $modal).val();
			one.f.switchmanager.staticRouteConfig.modal.ajax.staticRouteConfig(result, function(response) {
	                if(response.status == true) {
	                	$modal.modal('hide');
	                	// refresh dashlet by passing dashlet div as param
	                	one.f.switchmanager.staticRouteConfig.dashlet($("#left-bottom .dashlet"));
	                } else {
	                	// TODO: Show error message in a error message label instead.
	                	alert(response.message);
	                }
	            });
		},
		body: function() {
			var $form = $(document.createElement('form'));
			var $fieldset = $(document.createElement('fieldset'));
			// static route name
			var $label = one.lib.form.label("Name");
			var $input = one.lib.form.input("Name");
			$input.attr('id', one.f.switchmanager.staticRouteConfig.id.modal.form.routeName);
			$fieldset.append($label).append($input);
			// static route IP Mask 
			var $label = one.lib.form.label("Static Route");
			var $input = one.lib.form.input("Static Route");
			$input.attr('id', one.f.switchmanager.staticRouteConfig.id.modal.form.staticRoute);
			$fieldset.append($label).append($input);
			// static route IP Mask 
			var $label = one.lib.form.label("Next Hop");
			var $input = one.lib.form.input("Next Hop");
			$input.attr('id', one.f.switchmanager.staticRouteConfig.id.modal.form.nextHop);
			$fieldset.append($label).append($input);
			// return
			$form.append($fieldset);
			return $form;
		},
		ajax: {
			staticRouteConfig: function(requestData, callback) {
				$.getJSON(one.f.switchmanager.rootUrl + "/staticRoute/add", requestData, function(data) {
					callback(data);
				});
			}
		},
		data : {
            
        },
		footer : function() {
            var footer = [];
            if (one.role < 2) {
            	 var saveButton = one.lib.dashlet.button.single("Save", one.f.switchmanager.staticRouteConfig.id.modal.save, "btn-success", "");
                 var $saveButton = one.lib.dashlet.button.button(saveButton);
                 footer.push($saveButton);
            }
            return footer;
        }
	},
	// data functions
	data : {
		staticRouteConfig : function(data) {
			var result = [];
			$.each(data.nodeData, function(key, value) {
				var tr = {};
				// fill up all the td's
				var entry = [];
				var checkbox = document.createElement("input");
				checkbox.setAttribute("type", "checkbox");
				checkbox.setAttribute("id", value["name"]);
				entry.push(checkbox);
				entry.push(value["name"]);
				entry.push(value["staticRoute"]);
				entry.push(value["nextHop"]);
				tr.entry = entry;
				result.push(tr);
			});
			return result;
		}
	}
}

one.f.switchmanager.spanPortConfig = {
	id: {
		dashlet: {
			add: "one_f_switchmanager_spanPortConfig_id_dashlet_add",
			remove: "one_f_switchmanager_spanPortConfig_id_dashlet_remove"
		}, 
		modal: {
			modal: "one_f_switchmanager_spanPortConfig_id_modal_modal",
			save: "one_f_switchmanager_spanPortConfig_id_modal_save",
			form: {
				name : "one_f_switchmanager_spanPortConfig_id_modal_form_name",
				nodes : "one_f_switchmanager_spanPortConfig_id_modal_form_nodes",
                port : "one_f_switchmanager_spanPortConfig_id_modal_form_port",
			}
		}
	},
	dashlet: function($dashlet) {
		one.lib.dashlet.empty($dashlet);
		if(one.role < 2) {

			// Add span port button
			var button = one.lib.dashlet.button.single("Add Span Port", one.f.switchmanager.spanPortConfig.id.dashlet.add, "btn-primary", "btn-mini");
			var $button = one.lib.dashlet.button.button(button);

			$button.click(function() {
				var $modal = one.f.switchmanager.spanPortConfig.modal.initialize();
				$modal.modal();
			});
			$dashlet.append(one.lib.dashlet.header(one.f.dashlet.spanPortConfig.name));
			$dashlet.append($button);

			// Delete span port button
			var button = one.lib.dashlet.button.single("Delete SPAN Port(s)", 
					one.f.switchmanager.spanPortConfig.id.dashlet.remove, "btn-primary", "btn-mini");
			var $button = one.lib.dashlet.button.button(button);
			$button.click(function() {

				var checkedCheckBoxes = $("input:checked", $(this).closest(".dashlet").find("table"));
				if(checkedCheckBoxes.length > 0) {
					var spanPortsToDelete = "";
					checkedCheckBoxes.each(function(index, value) {
						spanPortsToDelete += checkedCheckBoxes[index].spanPort + "###";
					}); 

					var requestData = {};
					requestData["spanPortsToDelete"] = spanPortsToDelete;
					var url = one.f.switchmanager.rootUrl + "/spanPorts/delete";
					one.f.switchmanager.spanPortConfig.ajax.main(url, requestData, function(response) {
						if(response.status == true) {
							// refresh dashlet by passing dashlet div as param
							one.f.switchmanager.spanPortConfig.dashlet($("#right-bottom .dashlet"));
						} else {
							alert(response.message);
						}
					});
				}
			});
			$dashlet.append($button);
		}
        
        //populate table in dashlet
		var url = one.f.switchmanager.rootUrl + "/spanPorts";
		one.f.switchmanager.spanPortConfig.ajax.main(url, {}, function(content) {
			var body = one.f.switchmanager.spanPortConfig.data.devices(content);
			// first column contains the checkbox. no header required.
			content.columnNames.splice(0,0," ");
			var $table = one.f.switchmanager.createTable(content.columnNames, body);
			$dashlet.append($table);
		});
	},
	// device ajax calls
	ajax : {
		main : function(url, requestData, callback) {
			$.getJSON(url, requestData, function(data) {
				callback(data);
			});
		}
	},
	registry: {},
	modal : {
		initialize: function() {
			var h3 = "Add SPAN Port";
            var footer = one.f.switchmanager.spanPortConfig.modal.footer();
            var $modal = one.lib.modal.spawn(one.f.switchmanager.spanPortConfig.id.modal.modal, h3, "", footer);
            // bind save button
            $('#' + one.f.switchmanager.spanPortConfig.id.modal.save, $modal).click(function() {
            	one.f.switchmanager.spanPortConfig.modal.save($modal);
            });

            one.f.switchmanager.spanPortConfig.modal.ajax.nodes(function(nodes, nodeports) {
                var $body = one.f.switchmanager.spanPortConfig.modal.body(nodes, nodeports);
                one.lib.modal.inject.body($modal, $body);
            });
            return $modal;
		},
		save: function($modal) {
			var result = {};
            result['nodeId'] = $('#' + one.f.switchmanager.spanPortConfig.id.modal.form.nodes, $modal).val();
            result['spanPort'] = $('#' + one.f.switchmanager.spanPortConfig.id.modal.form.port, $modal).val();
			one.f.switchmanager.spanPortConfig.modal.ajax.saveSpanPortConfig(result, 
				function(response) {
					if(response.status == true) {
						$modal.modal('hide');
						one.f.switchmanager.spanPortConfig.dashlet($("#right-bottom .dashlet"));
					} else {
						alert(response.message);
					}
	                
	            });
		},
		body: function(nodes, nodeports) {
			var $form = $(document.createElement('form'));
			var $fieldset = $(document.createElement('fieldset'));
			// node
			var $label = one.lib.form.label("Node");
			var $select = one.lib.form.select.create(nodes);
			one.lib.form.select.prepend($select, { '' : 'Please Select a Node' });
			$select.attr('id', one.f.switchmanager.spanPortConfig.id.modal.form.nodes);
			
			// bind onchange
			$select.change(function() {
			    // retrieve port value
			    var node = $(this).find('option:selected').attr('value');
			    one.f.switchmanager.spanPortConfig.registry['currentNode'] = node;
			    var $ports = $('#' + one.f.switchmanager.spanPortConfig.id.modal.form.port);
			    var ports = nodeports[node];
			    one.lib.form.select.inject($ports, ports);
			});

            $fieldset.append($label).append($select);
			// input port
			var $label = one.lib.form.label("Input Port");
			var $select = one.lib.form.select.create();
			$select.attr('id', one.f.switchmanager.spanPortConfig.id.modal.form.port);
			$fieldset.append($label).append($select);
			
			// return
			$form.append($fieldset);
			return $form;
		},
		ajax: {
			nodes: function(callback) {
				$.getJSON(one.f.switchmanager.rootUrl + "/nodeports", function(data) {
                    var nodes = one.f.switchmanager.spanPortConfig.modal.data.nodes(data);
                    var nodeports = data;
                    one.f.switchmanager.spanPortConfig.registry['nodeports'] = nodeports;
                    callback(nodes, nodeports);
                });
			},
			saveSpanPortConfig: function(requestData, callback) {
				var resource = {};
				resource["jsonData"] = JSON.stringify(requestData);
				$.getJSON(one.f.switchmanager.rootUrl + "/spanPorts/add", resource, function(data) {
					callback(data);
				});
			}
		},
		data : {
            nodes : function(data) {
                result = {};
                $.each(data, function(key, value) {
                    result[key] = key;
                });
                return result;
            }
        },
		footer : function() {
            var footer = [];
            if (one.role < 2) {
                var saveButton = one.lib.dashlet.button.single("Save", one.f.switchmanager.spanPortConfig.id.modal.save, "btn-success", "");
                var $saveButton = one.lib.dashlet.button.button(saveButton);
                footer.push($saveButton);
            }
            return footer;
        }
	},
	// data functions
	data : {
		devices : function(data) {
			var result = [];
			$.each(data.nodeData, function(key, value) {
				var tr = {};
				// fill up all the td's
				var entry = [];
				var checkbox = document.createElement("input");
				checkbox.setAttribute("type", "checkbox");
				checkbox.spanPort = value.json;
				entry.push(checkbox);
				entry.push(value["nodeName"]);
				entry.push(value["spanPort"]);
				tr.entry = entry;
				result.push(tr);
			});
			return result;
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

one.f.addPopOut = function() {
	$img1 = $(document.createElement("img"));
	$img1.attr("src", "/img/Expand16T.png");
	$img1.attr("style", "float: right;");
	$img1.hover(function() {
		$img1.css("cursor", "pointer");
	});
	$img1.click(function() {
		var $modal = one.f.switchmanager.nodesLearnt.modal.initialize.popout();
    	$modal.css('width', 'auto');
		$modal.css('margin-left', '-40%');
        $modal.modal();
	});
	$dash1 = $($("#left-top .nav")[0]);
	$dash1.append($img1);
};
one.f.addPopOut();

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
        case menu.nodesLearnt.id:
        	one.f.switchmanager.nodesLearnt.dashlet($dashlet);
            break;
        case menu.staticRouteConfig.id:
        	one.f.switchmanager.staticRouteConfig.dashlet($dashlet);
            break;
        case menu.subnetGatewayConfig.id:
        	one.f.switchmanager.subnetGatewayConfig.dashlet($dashlet);
            break;
        case menu.spanPortConfig.id:
        	one.f.switchmanager.spanPortConfig.dashlet($dashlet);
            break;
    };
});

// activate first tab on each dashlet
$('.dash .nav').each(function(index, value) {
    $($(value).find('li')[0]).find('a').click();
});