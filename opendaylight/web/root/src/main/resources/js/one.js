// global
var one = {
	// global variables
	global : {
		remoteAddress : "/"
	},
	role : null
}

// one ui library
one.lib = {};

// registry
one.lib.registry = {};

/** DASHLET */
one.lib.dashlet = {
	empty : function($dashlet) {
		$dashlet.empty();
	},
	append : function($dashlet, $content) {
		$dashlet.append($content);
	},
	header : function(header) {
		var $h4 = $(document.createElement('h4'));
		$h4.text(header);
		return $h4;
	},
	list : function(list) {
		var $ul = $(document.createElement('ul'));
		$(list).each(function(index, value) {
			var $li = $(document.createElement('li'));
			$li.append(value);
			$ul.append($li);
		});
		return $ul;
	},
	button : {
		config : function(name, id, type, size) {
			var button = {};
			button['name'] = name;
			button['id'] = id;
			button['type'] = type;
			button['size'] = size;
			return button;
		},
		single : function(name, id, type, size) {
			var buttonList = [];
			var button = one.lib.dashlet.button.config(name, id, type, size);
			buttonList.push(button);
			return buttonList;
		},
		button : function(buttonList) {
			var $buttonGroup = $(document.createElement('div'));
			$buttonGroup.addClass("btn-group");
			$(buttonList).each(function(index, value) {
				var $button = $(document.createElement('button'));
				$button.text(value.name);
				$button.addClass('btn');
				$button.addClass(value['type']);
				$button.addClass(value['size']);
				if(!(typeof value.id === 'undefined')) {
					$button.attr('id', value.id);
				}
				$buttonGroup.append($button);
			});
			return $buttonGroup;
		}
	},
	table : {
		table : function(classes, id) {
			var $table = $(document.createElement('table'));
			$table.addClass("table");
			$(classes).each(function(index, value) {
				$table.addClass(value);
			});
			if (!(typeof id === 'undefined'))
				$table.attr("id", id);
			return $table;
		},
		header : function(headers) {
			var $thead = $(document.createElement('thead'));
			var $tr = $(document.createElement('tr'));
			$(headers).each(function(index, value) {
				$th = $(document.createElement('th'));
				$th.append(value);
				$tr.append($th);
			});
			$thead.append($tr);
			return $thead;
		},
		body : function(body, thead) {
			var $tbody = $(document.createElement('tbody'));
			// if empty
			if (body.length == 0 && !(typeof thead === 'undefined')) {
				var $tr = $(document.createElement('tr'));
				var $td = $(document.createElement('td'));
				$td.attr("colspan", thead.length);
				$td.text("No data available");
				$td.addClass("empty");
				$tr.append($td);
				$tbody.append($tr);
				return $tbody;
			}
			// else, populate as usual
			$(body).each(function(index, value) {
				var $tr = $(document.createElement('tr'));
				// data-id
				if(value['id'] != undefined) {
					$tr.attr('data-id', value['id']);
				}
				// add classes
				$(value["type"]).each(function(index, value) {
					$tr.addClass(value);
				});
				// add entries
				$(value["entry"]).each(function(index, value) {
					var $td = $(document.createElement('td'));
					$td.append(value);
					$tr.append($td);
				});
				$tbody.append($tr);
			});
			return $tbody;
		}
	},
	description : function(description, horizontal) {
		var $dl = $(document.createElement('dl'));
		if(horizontal == true) {
			$dl.addClass("dl-horizontal");
		}
		$(description).each(function(index, value) {
			var $dt = $(document.createElement('dt'));
			$dt.text(value.dt);
			var $dd = $(document.createElement('dd'));
			$dd.text(value.dd);
			$dl.append($dt).append($dd);
		});
		return $dl;
	}
}

/** MODAL */
one.lib.modal = {
	// clone default modal
	clone : function(id) {
		var $clone = $("#modal").clone(true);
		$clone.attr("id", id);
		return $clone;
	},
	// populate modal
	populate : function($modal, header, $body, footer) {
		var $h3 = $modal.find("h3");
		$h3.text(header);

		var $modalBody = $modal.find(".modal-body");
		$modalBody.append($body);

		$(footer).each(function(index, value) {
			$modal.find(".modal-footer").append(value);
		});
	},
	// clone and populate modal
	spawn : function(id, header, $body, footer) {
		var $modal = one.lib.modal.clone(id);
		one.lib.modal.populate($modal, header, $body, footer);
		return $modal;
	},
	// empty modal
	empty : function($modal) {
		$modal.find("h3").empty();
		$modal.find(".modal-body").empty();
		$modal.find(".modal-footer").empty();
	},
	// injection
	inject : {
		body : function($modal, $body) {
			$modal.find(".modal-body").empty();
			$modal.find(".modal-body").append($body);
		}
	}
}

/** FORM */
one.lib.form = {
	// create select-option form element
	select : {
		create : function(options, multiple) {
			// assert - auto assign
			if(options == undefined) options = {};

			var $select = $(document.createElement('select'));
			if (multiple == true) {
				$select.attr("multiple", "multiple");
			}
			var optionArray = one.lib.form.select.options(options);
			$(optionArray).each(function(index, value) {
				$select.append(value);
			});
			return $select;
		},
		options : function(options) {
			var result = [];
			$.each(options, function(key, value) {
				var $option = $(document.createElement('option'));
				$option.attr("value", key);
				$option.text(value);
				result.push($option);
			});
			return result;
		},
		empty : function($select) {
			$select.empty();
		},
		inject : function($select, options) {
			$select.empty();
			var options = one.lib.form.select.options(options);
			$select.append(options);
		},
		prepend : function($select, options) {
			var options = one.lib.form.select.options(options);
			$select.prepend(options);
		},
		bubble : function($select, bubble) {
			$($select.find("option")).each(function(index, value) {
				if( $(value).attr("value") == bubble ) {
					var option = $(value);
					$(value).remove();
					$select.prepend(option);
					return;
				}
			});
		}
	},
	// create legend form element
	legend : function(name) {
		var $legend = $(document.createElement('legend'));
		$legend.text(name);
		return $legend;
	},
	// create label form element
	label : function(name) {
		var $label = $(document.createElement('label'));
		$label.text(name);
		return $label;
	},
	// create help block form element
	help : function(help) {
		var $help = $(document.createElement('span'));
		$help.text(help);
		$help.addClass("help-block");
		return $help;
	},
	// create generic text input
	input : function(placeholder) {
		var $input = $(document.createElement('input'));
		$input.attr('type', 'text');
		$input.attr('placeholder', placeholder);
		return $input;
	}
}

/** NAV */
one.lib.nav = {
	unfocus : function($nav) {
		$($nav.find("li")).each(function(index, value) {
			$(value).removeClass("active");
		});
	}
}

/** ALERT */
one.lib.alert = function(alert) {
	$("#alert p").text(alert);
	$("#alert").hide();
	$("#alert").slideToggle();
	clearTimeout(one.lib.registry.alert);
	one.lib.registry.alert = setTimeout(function() {
		$("#alert").slideUp();
	}, 8000);
}

/* 
	INITIALIZATION
	executable JS code starts here
*/
//OpenDaylight root page
one.main = {};

one.main.constants = {
    address : {
        menu : "/web.json",
        prefix : "/controller/web",
        save : "/save"
    }
}

one.main.menu = {
    load : function() {
        one.main.menu.ajax(function(data) {
            // reparse the ajax data
            var result = one.main.menu.data.menu(data);
            // transform into list to append to menu
            var $div = one.main.menu.menu(result);
            // append to menu
            $("#menu .nav").append($div.children());
            // binding all menu items
            var $menu = $("#menu .nav a");
            $menu.click(function() {
                var href = $(this).attr('href').substring(1);
                one.main.page.load(href);
                var $li = $(this).parent();
                // reset all other active
                $menu.each(function(index, value) {
                	$(value).parent().removeClass('active');
                });
                $li.addClass('active');
            });
            // reset or go to first menu item by default
            var currentLocation = location.hash;
            if (data[currentLocation.substring(1)] == undefined) {
            	$($menu[0]).click();
            } else {
            	$menu.each(function(index, value) {
            		var menuLocation = $(value).attr('href');
            		if (currentLocation == menuLocation) {
            			$($menu[index]).click();
            			return;
            		}
            	});
            }
        });
    },
    menu : function(result) {
        var $div = $(document.createElement('div'));
        $(result).each(function(index, value) {
            if( value != undefined) {
                var $li = $(document.createElement('li'));
                var $a = $(document.createElement('a'));
                $a.text(value['name']);
                $a.attr('href', '#'+value['id']);
                $li.append($a);
                $div.append($li);
            }
        });
        return $div;
    },
    ajax : function(successCallback) {
        $.getJSON(one.main.constants.address.menu, function(data) {
            successCallback(data);
        });
    },
    data : {
        menu : function(data) {
            var result = [];
            $.each(data, function(key, value) {
                var order = value['order'];
                if (order >= 0) {
	                var name = value['name'];
	                var entry = {
	                    'name' : name,
	                    'id' : key
	                };
	                result[order] = entry;
                }
            });
            return result;
        }
    }
}

one.main.page = {
    load : function(page) {
		if (one.f !== undefined && one.f.cleanUp !== undefined) {
		    one.f.cleanUp();
		}   	
        // clear page related
        delete one.f;
        $('.dashlet', '#main').empty();
        $('.nav', '#main').empty();
        // fetch page's js
        $.getScript(one.main.constants.address.prefix+"/"+page+"/js/page.js");
        
        $.ajaxSetup({
        	data : {
        		'x-page-url' : page
        	}
        });
    },
    dashlet : function($nav, dashlet) {
        var $li = $(document.createElement('li'));
        var $a = $(document.createElement('a'));
        $a.text(dashlet.name);
        $a.attr('id', dashlet.id);
        $a.attr('href', '#');
        $li.append($a);
        $nav.append($li);
    }
}

one.main.admin = {
    id : {
        modal : {
            main : "one_main_admin_id_modal_main",
            close : "one_main_admin_id_modal_close",
            user : "one_main_admin_id_modal_user",
            add : {
                user : "one_main_admin_id_modal_add_user",
                close : "one_main_admin_id_modal_add_close",
                form : {
                    name : "one_main_admin_id_modal_add_form_name",
                    role : "one_main_admin_id_modal_add_form_role",
                    password : "one_main_admin_id_modal_add_form_password"
                }
            },
            remove : {
                user : "one_main_admin_id_modal_remove_user",
                close : "one_main_admin_id_modal_remove_close"
            }
        },
        add : {
            user : "one_main_admin_id_add_user"
        }
    },
    address : {
        root : "/admin",
        users : "/users"
    },
    modal : {
        initialize : function(callback) {
            var h3 = "Welcome "+$('#admin').text();
            var footer = one.main.admin.modal.footer();
            var $modal = one.lib.modal.spawn(one.main.admin.id.modal.main, h3, '', footer);
            
            // close binding
            $('#'+one.main.admin.id.modal.close, $modal).click(function() {
                $modal.modal('hide');
            });
            
            // body inject
            one.main.admin.ajax.users(function($body) {
                one.lib.modal.inject.body($modal, $body);
            });
            
            // modal show callback
            callback($modal);
        },
        footer : function() {
            var footer = [];
            
            var closeButton = one.lib.dashlet.button.single("Close", one.main.admin.id.modal.close, "", "");
            var $closeButton = one.lib.dashlet.button.button(closeButton);
            footer.push($closeButton);
            
            return footer;
        }
    },
    ajax : {
        users : function(callback) {
            $.getJSON(one.main.admin.address.root+one.main.admin.address.users, function(data) {
                var body = one.main.admin.data.users(data);
                var $body = one.main.admin.body.users(body);
                callback($body);
            });
        }
    },
    data : {
        users : function(data) {
            var body = [];
            $(data).each(function(index, value) {
                var tr = {};
                var entry = [];
                entry.push(value['user']);
                entry.push(value['role']);
                tr['entry'] = entry;
                tr['id'] = value['user'];
                body.push(tr);
            });
            return body;
        }
    },
    body : {
        users : function(body) {
            var $div = $(document.createElement('div'));
            var $h5 = $(document.createElement('h5'));
            $h5.append("Manage Users");
            var attributes = ["table-striped", "table-bordered", "table-hover", "table-cursor"];
            var $table = one.lib.dashlet.table.table(attributes);
            var headers = ["User", "Role"];
            var $thead = one.lib.dashlet.table.header(headers);
            var $tbody = one.lib.dashlet.table.body(body);
            $table.append($thead).append($tbody);
            
            // bind table
            if (one.role < 2) {
	            $table.find('tr').click(function() {
	                var id = $(this).data('id');
	                one.main.admin.remove.modal.initialize(id);
	            });
            }
            
            // append to div
            $div.append($h5).append($table);
            
            if (one.role < 2) {
	            var addUserButton = one.lib.dashlet.button.single("Add User", one.main.admin.id.add.user, "btn-primary", "btn-mini");
	            var $addUserButton = one.lib.dashlet.button.button(addUserButton);
	            $div.append($addUserButton);
	            
	            // add user binding
	            $addUserButton.click(function() {
	                one.main.admin.add.modal.initialize();
	            });
            }
            
            return $div;
        }
    },
    remove : {
        modal : {
            initialize : function(id) {
                var h3 = "Remove User";
                var footer = one.main.admin.remove.footer();
                var $body = one.main.admin.remove.body();
                var $modal = one.lib.modal.spawn(one.main.admin.id.modal.user, h3, $body, footer);
                
                // close binding
                $('#'+one.main.admin.id.modal.remove.close, $modal).click(function() {
                    $modal.modal('hide');
                });
                
                // remove binding
                $('#'+one.main.admin.id.modal.remove.user, $modal).click(function() {
                    one.main.admin.remove.modal.ajax(id, function(result) {
                        if (result == 'Success') {
                            $modal.modal('hide');
                            // body inject
                            var $admin = $('#'+one.main.admin.id.modal.main);
                            one.main.admin.ajax.users(function($body) {
                                one.lib.modal.inject.body($admin, $body);
                            });
                        } else alert("Failed to remove user: "+result);
                    });
                });
                
                $modal.modal();
            },
            ajax : function(id, callback) {
                $.post(one.main.admin.address.root+one.main.admin.address.users+'/'+id, function(data) {
                    callback(data);
                });
            },
        },
        
        footer : function() {
            var footer = [];
            
            var removeButton = one.lib.dashlet.button.single("Remove User", one.main.admin.id.modal.remove.user, "btn-danger", "");
            var $removeButton = one.lib.dashlet.button.button(removeButton);
            footer.push($removeButton);
            
            var closeButton = one.lib.dashlet.button.single("Close", one.main.admin.id.modal.remove.close, "", "");
            var $closeButton = one.lib.dashlet.button.button(closeButton);
            footer.push($closeButton);
            
            return footer;
        },
        body : function() {
            var $p = $(document.createElement('p'));
            $p.append("Remove user?");
            return $p;
        },
    },
    add : {
        modal : {
            initialize : function() {
                var h3 = "Add User";
                var footer = one.main.admin.add.footer();
                var $body = one.main.admin.add.body();
                var $modal = one.lib.modal.spawn(one.main.admin.id.modal.user, h3, $body, footer);
                
                // close binding
                $('#'+one.main.admin.id.modal.add.close, $modal).click(function() {
                    $modal.modal('hide');
                });
                
                // add binding
                $('#'+one.main.admin.id.modal.add.user, $modal).click(function() {
                    one.main.admin.add.modal.add($modal, function(result) {
                        if(result == 'Success') {
                            $modal.modal('hide');
                            // body inject
                            var $admin = $('#'+one.main.admin.id.modal.main);
                            one.main.admin.ajax.users(function($body) {
                                one.lib.modal.inject.body($admin, $body);
                            });
                        } else alert("Failed to add user: "+result);
                    });
                });
                
                $modal.modal();
            },
            add : function($modal, callback) {
                var user = {};
                user['user'] = $modal.find('#'+one.main.admin.id.modal.add.form.name).val();
                user['password'] = $modal.find('#'+one.main.admin.id.modal.add.form.password).val();
                user['role'] = $modal.find('#'+one.main.admin.id.modal.add.form.role).find('option:selected').attr('value');
                
                var resource = {};
                resource['json'] = JSON.stringify(user);
                resource['action'] = 'add'
                
                one.main.admin.add.modal.ajax(resource, callback);
            },
            ajax : function(data, callback) {
                $.post(one.main.admin.address.root+one.main.admin.address.users, data, function(data) {
                    callback(data);
                });
            }
        },
        body : function() {
            var $form = $(document.createElement('form'));
            var $fieldset = $(document.createElement('fieldset'));
            // user
            var $label = one.lib.form.label('Username');
            var $input = one.lib.form.input('Username');
            $input.attr('id', one.main.admin.id.modal.add.form.name);
            $fieldset.append($label).append($input);
            // password
            var $label = one.lib.form.label('Password');
            var $input = one.lib.form.input('Password');
            $input.attr('id', one.main.admin.id.modal.add.form.password);
            $input.attr('type', 'password');
            $fieldset.append($label).append($input);
            // roles
            var $label = one.lib.form.label('Roles');
            var options = {
                "Network-Admin" : "Network Administrator",
                "Network-Operator" : "Network Operator"
            };
            var $select = one.lib.form.select.create(options);
            $select.attr('id', one.main.admin.id.modal.add.form.role);
            $fieldset.append($label).append($select);
            $form.append($fieldset);
            return $form;
        },
        footer : function() {
            var footer = [];
            
            var addButton = one.lib.dashlet.button.single("Add User", one.main.admin.id.modal.add.user, "btn-primary", "");
            var $addButton = one.lib.dashlet.button.button(addButton);
            footer.push($addButton);
            
            var closeButton = one.lib.dashlet.button.single("Close", one.main.admin.id.modal.add.close, "", "");
            var $closeButton = one.lib.dashlet.button.button(closeButton);
            footer.push($closeButton);
            
            return footer;
        }
    }
}

one.main.dashlet = {
	left : {
		top : $("#left-top .dashlet"),
		bottom : $("#left-bottom .dashlet")
	},
	right : {
		bottom : $("#right-bottom .dashlet")
	}
}

/** BOOTSTRAP */
$(".modal").on('hidden', function() {
    $(this).remove();
});

$("#alert .close").click(function() {
	$("#alert").hide();
});

/** INIT */

// parse role
one.role = $('#admin').data('role');

// user admin
$("#admin").click(function() {
    one.main.admin.modal.initialize(function($modal) {
        $modal.modal();
    });
});

// save
$("#save").click(function() {
	$.post(one.main.constants.address.save, function(data) {
		if (data == "Success") {
			one.lib.alert("Configuration Saved");
		} else {
			one.lib.alert("Unable to save configuration: "+data);
		}
	});
});

// logout
$("#logout").click(function() {
	location.href = "/logout";
});

$.ajaxSetup({
    complete : function(xhr,textStatus) {
    	var mime = xhr.getResponseHeader('Content-Type');
        if (mime.substring(0, 9) == 'text/html') {
            location.href = '/';
        }
    }
});

/** MAIN PAGE LOAD */
one.main.menu.load();