/** COMMON **/
var labelType, useGradients, nativeTextSupport, animate;

(function() {
	var ua = navigator.userAgent,
	iStuff = ua.match(/iPhone/i) || ua.match(/iPad/i),
	typeOfCanvas = typeof HTMLCanvasElement,
	nativeCanvasSupport = (typeOfCanvas == 'object' || typeOfCanvas == 'function'),
	textSupport = nativeCanvasSupport 
	&& (typeof document.createElement('canvas').getContext('2d').fillText == 'function');
	//I'm setting this based on the fact that ExCanvas provides text support for IE
	//and that as of today iPhone/iPad current text support is lame
	labelType = (!nativeCanvasSupport || (textSupport && !iStuff))? 'Native' : 'HTML';
	nativeTextSupport = labelType == 'Native';
	useGradients = nativeCanvasSupport;
	animate = !(iStuff || !nativeCanvasSupport);
})();

/** TOPOLOGY **/
one.topology = {};

one.topology.option = {
	navigation : function(enable, panning, zooming) {
		var option = {};
		option["enable"] = enable;
		option["panning"] = panning;
		option["zooming"] = zooming;
		return option;
	},
	node : function(overridable, color, height, dim) {
		var option = {};
		option["overridable"] = overridable;
		option["color"] = color;
		option["height"] = height;
		option["dim"] = dim;
		return option;
	},
	edge : function(overridable, color, lineWidth, epsilon) {
		var option = {};
		option["overridable"] = overridable;
		option["color"] = color;
		option["lineWidth"] = lineWidth;
		if (epsilon != undefined)
			option["epsilon"] = epsilon;
		return option;
	},
	label : function(style, node) {
		var marginTop, minWidth;
		if (node.data["$type"] == "swtch") {
			marginTop = "42px";
			minWidth = "65px";
		} else if (node.data["$type"] == "host") {
			marginTop = "48px";
			minWidth = "";
		} else if (node.data["$type"].indexOf("monitor") == 0) {
			marginTop = "52px";
			minWidth = "";
		}
		style.marginTop = marginTop;
		style.minWidth = minWidth;
		style.background = "rgba(68,68,68,0.7)";
		style.borderRadius = "4px";
		style.color = "#fff";
		style.cursor = "default";
	}
};

one.topology.init = function(json) {
	if (json.length == 0) {
		$div = $(document.createElement('div'));
		$img = $(document.createElement('div'));
		$img.css('height', '128px');
		$img.css('width', '128px');
		$img.css('background-image', 'url(/img/topology_view_1033_128.png)');
		$img.css('clear', 'both');
		$img.css('margin', '0 auto');
		$p = $(document.createElement('p'));
		$p.addClass('text-center');
		$p.addClass('text-info');
		$p.css('width', '100%');
		$p.css('padding', '10px 0');
		$p.css('cursor', 'default');
		$p.append('No Network Elements Connected');
		$div.css('position', 'absolute');
		$div.css('top', '25%');
		$div.css('margin', '0 auto');
		$div.css('width', '100%');
		$div.css('text-align', 'center');
		$div.append($img).append($p);
		$("#topology").append($div);
		return false;
	}
	one.topology.graph = new $jit.MultiTopology({
		injectInto: 'topology',
		Navigation: one.topology.option.navigation(true, 'avoid nodes', 10),
		Node: one.topology.option.node(true, '#444', 25, 27),
		Edge: one.topology.option.edge(true, '23A4FF', 1.5),
		Tips: {
			enable: true,
			type: 'Native',
			onShow: function(tip, node) {
				if (node.name != undefined)
					tip.innerHTML = "";
				//tipsOnShow(tip, node);
			}
		},
		Events: {
			enable: true,
			type: 'Native',
			onMouseEnter: function(node, eventInfo, e) {
				// if node
				if (node.id != undefined) {
					one.topology.graph.canvas.getElement().style.cursor = 'move';
				} else if (eventInfo.edge != undefined && 
						eventInfo.edge.nodeTo.data["$type"] == "swtch" && 
						eventInfo.edge.nodeFrom.data["$type"] == "swtch") {
					one.topology.graph.canvas.getElement().style.cursor = 'pointer';
				}
			},
			onMouseLeave: function(node, eventInfo, e) {
				one.topology.graph.canvas.getElement().style.cursor = '';
			},
			//Update node positions when dragged
			onDragMove: function(node, eventInfo, e) {
				var pos = eventInfo.getPos();
				node.pos.setc(pos.x, pos.y);
				one.topology.graph.plot();
				one.topology.graph.canvas.getElement().style.cursor = 'crosshair';
			},
			//Implement the same handler for touchscreens
			onTouchMove: function(node, eventInfo, e) {
				$jit.util.event.stop(e); //stop default touchmove event
				this.onDragMove(node, eventInfo, e);
			},
			onDragEnd: function(node, eventInfo, e) {
				var ps = eventInfo.getPos();
				var did = node.id;
				var data = {};
				data['x'] = ps.x;
				data['y'] = ps.y;
				$.post('/controller/web/topology/node/' + did, data);
			},
			onClick: function(node, eventInfo, e) {
				if(one.f.topology === undefined) {
					return false;
				} else {
					one.f.topology.Events.onClick(node, eventInfo);
				}
			}
		},
		iterations: 200,
		levelDistance: 130,
		onCreateLabel: function(domElement, node){
			var nameContainer = document.createElement('span'),
			closeButton = document.createElement('span'),
			style = nameContainer.style;
			nameContainer.className = 'name';
			var nodeDesc = node.data["$desc"];
			if (nodeDesc == "None" || nodeDesc == "" || nodeDesc == "undefined" || nodeDesc == undefined) {
				nameContainer.innerHTML = "<small>"+node.name+"</small>";
			} else {
				nameContainer.innerHTML = nodeDesc;
			}
			domElement.appendChild(nameContainer);
			style.fontSize = "1.0em";
			style.color = "#000";
			style.fontWeight = "bold";
			style.width = "100%";
			style.padding = "1.5px 4px";
			style.display = "block";

			one.topology.option.label(style, node);
		},
		onPlaceLabel: function(domElement, node){
			var style = domElement.style;
			var left = parseInt(style.left);
			var top = parseInt(style.top);
			var w = domElement.offsetWidth;
			style.left = (left - w / 2) + 'px';
			style.top = (top - 15) + 'px';
			style.display = '';
			style.minWidth = "28px";
			style.width = "auto";
			style.height = "28px";
			style.textAlign = "center";
		}
	});

	one.topology.graph.loadJSON(json);
	// compute positions incrementally and animate.
	one.topology.graph.computeIncremental({
		iter: 40,
		property: 'end',
		onStep: function(perc){
			console.log(perc + '% loaded');
		},
		onComplete: function(){
			for (var idx in one.topology.graph.graph.nodes) {
				var node = one.topology.graph.graph.nodes[idx];
				if(node.getData("x") && node.getData("y")) {
					var x = node.getData("x");
					var y = node.getData("y");
					node.setPos(new $jit.Complex(x, y), "end");
				}
			}
                        console.log('done');
			one.topology.graph.animate({
				modes: ['linear'],
				transition: $jit.Trans.Elastic.easeOut,
				duration: 0
			});
		}
	});
	one.topology.graph.canvas.setZoom(0.8,0.8);
}

one.topology.update = function() {
	$('#topology').empty();
	$.getJSON(one.global.remoteAddress+"controller/web/topology/visual.json", function(data) {
		one.topology.init(data);
	});
}

/** INIT */
$.getJSON(one.global.remoteAddress+"controller/web/topology/visual.json", function(data) {
	one.topology.init(data);
});
