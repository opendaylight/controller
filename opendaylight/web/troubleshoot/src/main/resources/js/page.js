
/* 
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved. 
 * 
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

//PAGE troubleshoot
one.f = {};

// specify dashlets and layouts
one.f.dashlet = {
    existingNodes : {
        id : 'existingNodes',
        name : 'Existing Nodes'
    },
    uptime: {
    	id: 'uptime',
    	name: 'Uptime'
    },
    flowsOrPorts: {
    	id: "flowsOrPorts",
    	name: "Statistics"
    }
};

one.f.menu = {
    left : {
        top : [
            one.f.dashlet.existingNodes
        ],
        bottom : [
            one.f.dashlet.uptime
        ]
    },
    right : {
        top : [],
        bottom : [
            one.f.dashlet.flowsOrPorts
        ]
    }
};

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

/**Troubleshoot modules*/
one.f.troubleshooting = {
	rootUrl: "/controller/web/troubleshoot",
	rightBottomDashlet: { 
		get: function() {
			var $rightBottomDashlet = $("#right-bottom").find(".dashlet");
			return $rightBottomDashlet;
		},
		setDashletHeader: function(label) {
			$("#right-bottom li a")[0].innerHTML = label; 
		}
	},
	createTable: function(columnNames, body) {
		var tableAttributes = ["table-striped", "table-bordered", "table-condensed"];
		var $table = one.lib.dashlet.table.table(tableAttributes);
		var tableHeaders = columnNames;
		var $thead = one.lib.dashlet.table.header(tableHeaders);
		var $tbody = one.lib.dashlet.table.body(body, tableHeaders);
		$table.append($thead)
			.append($tbody);
		return $table;
	}
};

one.f.troubleshooting.existingNodes = {
		id: {
			popout: "one_f_troubleshooting_existingNodes_id_popout",
			modal: "one_f_troubleshooting_existingNodes_id_modal"
		},
		load: {
			main: function($dashlet) {
				one.lib.dashlet.empty($dashlet);
				$dashlet.append(one.lib.dashlet.header(one.f.dashlet.existingNodes.name));
				
				// TODO(l): Add a generic auto expand function to one.lib and replace custom height setting.
				//$('#left-top').height('100%');
				one.f.troubleshooting.existingNodes.ajax(one.f.troubleshooting.rootUrl + "/existingNodes" , function(content) {
					var body = one.f.troubleshooting.existingNodes.data.existingNodes(content);
					var $table = one.f.troubleshooting.createTable(content.columnNames, body);
					$dashlet.append($table);
				});
			},
			flows: function(nodeId) {
				try {
					clearTimeout(one.f.troubleshooting.existingNodes.registry.refreshTimer);
					$.getJSON(one.main.constants.address.prefix + "/troubleshoot/flowStats?nodeId=" + nodeId, function(content) {
						var body = one.f.troubleshooting.existingNodes.data.flows(content);
						var $table = one.f.troubleshooting.createTable(content.columnNames, body);
						$rightBottomDashlet = one.f.troubleshooting.rightBottomDashlet.get();
						one.f.troubleshooting.rightBottomDashlet.setDashletHeader("Flows");
						one.lib.dashlet.empty($rightBottomDashlet);
						$rightBottomDashlet.append(one.lib.dashlet.header("Flow Details"));
						$rightBottomDashlet.append($table);
						one.f.troubleshooting.existingNodes.registry.refreshTimer = setTimeout(
								one.f.troubleshooting.existingNodes.load.flows, 5000, nodeId);
					});
				} catch(e) {}
			},
			ports: function(nodeId) {
				try {
					clearTimeout(one.f.troubleshooting.existingNodes.registry.refreshTimer);
					$.getJSON(one.main.constants.address.prefix + "/troubleshoot/portStats?nodeId=" + nodeId, function(content) {
						var body = one.f.troubleshooting.existingNodes.data.ports(content);
						var $table = one.f.troubleshooting.createTable(content.columnNames, body);
						$rightBottomDashlet = one.f.troubleshooting.rightBottomDashlet.get();
						one.f.troubleshooting.rightBottomDashlet.setDashletHeader("Ports");
						one.lib.dashlet.empty($rightBottomDashlet);
						$rightBottomDashlet.append(one.lib.dashlet.header("Port Details"));
						$rightBottomDashlet.append($table);
						one.f.troubleshooting.existingNodes.registry.refreshTimer = setTimeout(
								one.f.troubleshooting.existingNodes.load.ports, 5000, nodeId);
					});
				} catch(e) {}
			} 
		},
		ajax : function(url, callback) {
			$.getJSON(url, function(data) {
				callback(data);
			});
		},
		registry: {},
		modal : {
		},
		data : {
			existingNodes : function(data) {
				var result = [];
				$.each(data.nodeData, function(key, value) {
					var tr = {};
					var entry = [];
					entry.push(value["nodeName"]);
					entry.push(value["nodeId"]);
					var nodeIdvalue = value["nodeId"];
					entry.push("<a href=\"javascript:one.f.troubleshooting.existingNodes.load.flows('" + value["nodeId"] + "');\">Flows</a>" + 
							" <a href=\"javascript:one.f.troubleshooting.existingNodes.load.ports('" + value["nodeId"] + "');\">Ports</a>");
					tr.entry = entry;
					result.push(tr);
				});
				return result;
			},
			ports: function(data) {
				var result = [];
				$.each(data.nodeData, function(key, value) {
					var tr = {};
					var entry = [];
					entry.push(value["nodeConnector"]);
					entry.push(value["rxPkts"]);
					entry.push(value["txPkts"]);
					entry.push(value["rxBytes"]);
					entry.push(value["txBytes"]);
					entry.push(value["rxDrops"]);
					entry.push(value["txDrops"]);
					entry.push(value["rxErrors"]);
					entry.push(value["txErrors"]);
					entry.push(value["rxFrameErrors"]);
					entry.push(value["rxOverRunErrors"]);
					entry.push(value["rxCRCErrors"]);
					entry.push(value["collisions"]);
					tr.entry = entry;
					result.push(tr);
				});
				return result;
			},
			flows: function(data) {
				var result = [];
				$.each(data.nodeData, function(key, value) {
					var tr = {};
					var entry = [];
					entry.push(value["nodeName"]);
					entry.push(value["inPort"]);
					entry.push(value["dlSrc"]);
					entry.push(value["dlDst"]);
					entry.push(value["dlType"]);
					entry.push(value["dlVlan"]);
					entry.push(value["nwSrc"]);
					entry.push(value["nwDst"]);
					entry.push(value["nwProto"]);
					entry.push(value["tpSrc"]);
					entry.push(value["tpDst"]);
					entry.push(value["actions"]);
					entry.push(value["byteCount"]);
					entry.push(value["packetCount"]);
					entry.push(value["durationSeconds"]);
					entry.push(value["idleTimeout"]);
					entry.push(value["outPorts"]);
					entry.push(value["outVlanId"]);
					entry.push(value["priority"]);
					tr.entry = entry;
					result.push(tr);
				});
				return result;
			}
		}
};

one.f.troubleshooting.uptime = {
	id: {
		popout: "one_f_troubleshooting_existingNodes_id_popout",
		modal: "one_f_troubleshooting_existingNodes_id_modal"
	},

	dashlet: function($dashlet) {
			one.lib.dashlet.empty($dashlet);
			$dashlet.append(one.lib.dashlet.header(one.f.dashlet.uptime.name));
			var url = one.f.troubleshooting.rootUrl + "/uptime";
			one.f.troubleshooting.uptime.ajax.main(url , {} ,function(content) {
				var body = one.f.troubleshooting.uptime.data.uptime(content);
				var $table = one.f.troubleshooting.createTable(content.columnNames, body);
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
	
	data: {
		uptime: function(data) {
			var result = [];
			$.each(data.nodeData, function(key, value) {
				var tr = {};
				var entry = [];
				entry.push(value["nodeName"]);
				entry.push(value["nodeId"]);
				entry.push(value["connectedSince"]);
				tr.entry = entry;
				result.push(tr);
			});
			return result;
		}
	},
};

one.f.troubleshooting.statistics = {
	dashlet : function($dashlet) {
        var $h4 = one.lib.dashlet.header("Statistics");
        $dashlet.append($h4);
		// empty
		var $none = $(document.createElement('div'));
		$none.addClass('none');
		var $p = $(document.createElement('p'));
		$p.text('Please select a Flow or Ports statistics');
		$p.addClass('text-center').addClass('text-info');
		
		$dashlet.append($none)
			.append($p);
	}
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
        case menu.existingNodes.id:
        	one.f.troubleshooting.existingNodes.load.main($dashlet);
            break;
        case menu.uptime.id:
        	one.f.troubleshooting.uptime.dashlet($dashlet);
			break;
		case menu.flowsOrPorts.id:
			one.f.troubleshooting.statistics.dashlet($dashlet);
			break;
    };
});

// activate first tab on each dashlet
$('.dash .nav').each(function(index, value) {
    $($(value).find('li')[0]).find('a').click();
});
