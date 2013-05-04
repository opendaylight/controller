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
            if (value != undefined) {
                var $li = $(document.createElement('li'));
                var $a = $(document.createElement('a'));
                $a.text(value['name']);
                $a.attr('href', '#' + value['id']);
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
        $.getScript(one.main.constants.address.prefix + "/" + page
                + "/js/page.js");

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
                    password : "one_main_admin_id_modal_add_form_password",
					verify : "one_main_admin_id_modal_add_form_verify"
                }
            },
            remove : {
                user : "one_main_admin_id_modal_remove_user",
                close : "one_main_admin_id_modal_remove_close",
				password : 'one_main_admin_id_modal_remove_password'
            },
			password : {
				modal : 'one_main_admin_id_modal_password_modal',
				submit : 'one_main_admin_id_modal_password_submit',
				cancel : 'one_main_admin_id_modal_password_cancel',
				form : {
					old : 'one_main_admin_id_modal_password_form_old',
					set : 'one_main_admin_id_modal_password_form_new',
					verify : 'one_main_admin_id_modal_password_form_verify'
				}
			}
        },
        add : {
            user : "one_main_admin_id_add_user"
        }
    },
    address : {
        root : "/admin",
        users : "/users",
		password : '/admin/users/password/'
    },
    modal : {
        initialize : function(callback) {
            var h3 = "Welcome " + $('#admin').text();
            var footer = one.main.admin.modal.footer();
            var $modal = one.lib.modal.spawn(one.main.admin.id.modal.main, h3,
                    '', footer);

            // close binding
            $('#' + one.main.admin.id.modal.close, $modal).click(function() {
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

            var closeButton = one.lib.dashlet.button.single("Close",
                    one.main.admin.id.modal.close, "", "");
            var $closeButton = one.lib.dashlet.button.button(closeButton);
            footer.push($closeButton);

            return footer;
        }
    },
    ajax : {
        users : function(callback) {
            $.getJSON(one.main.admin.address.root
                    + one.main.admin.address.users, function(data) {
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
            var attributes = [ "table-striped", "table-bordered",
                    "table-hover", "table-cursor" ];
            var $table = one.lib.dashlet.table.table(attributes);
            var headers = [ "User", "Role" ];
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
                var addUserButton = one.lib.dashlet.button.single("Add User",
                        one.main.admin.id.add.user, "btn-primary", "btn-mini");
                var $addUserButton = one.lib.dashlet.button
                        .button(addUserButton);
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
                var h3 = "Edit User";
                var footer = one.main.admin.remove.footer();
                var $body = one.main.admin.remove.body();
                var $modal = one.lib.modal.spawn(one.main.admin.id.modal.user,
                        h3, $body, footer);

                // close binding
                $('#' + one.main.admin.id.modal.remove.close, $modal).click(
                        function() {
                            $modal.modal('hide');
                        });

                // remove binding
                $('#' + one.main.admin.id.modal.remove.user, $modal)
                        .click(
                                function() {
                                    one.main.admin.remove.modal
                                            .ajax(
                                                    id,
                                                    function(result) {
                                                        if (result == 'Success') {
                                                            $modal
                                                                    .modal('hide');
                                                            // body inject
                                                            var $admin = $('#'
                                                                    + one.main.admin.id.modal.main);
                                                            one.main.admin.ajax
                                                                    .users(function($body) {
                                                                        one.lib.modal.inject
                                                                                .body(
                                                                                        $admin,
                                                                                        $body);
                                                                    });
                                                        } else
                                                            alert("Failed to remove user: "
                                                                    + result);
                                                    });
                                });

				// change password binding
				$('#' + one.main.admin.id.modal.remove.password, $modal).click(function() {
					one.main.admin.password.initialize(id, function() {
						$modal.modal('hide');
					});
				});

                $modal.modal();
            },
            ajax : function(id, callback) {
                $.post(one.main.admin.address.root
                        + one.main.admin.address.users + '/' + id,
                        function(data) {
                            callback(data);
                        });
            },
        },

        footer : function() {
            var footer = [];

            var removeButton = one.lib.dashlet.button.single("Remove User",
                    one.main.admin.id.modal.remove.user, "btn-danger", "");
            var $removeButton = one.lib.dashlet.button.button(removeButton);
            footer.push($removeButton);

			var change = one.lib.dashlet.button.single('Change Password',
					one.main.admin.id.modal.remove.password, 'btn-success', '');
			var $change = one.lib.dashlet.button.button(change);
			footer.push($change);

            var closeButton = one.lib.dashlet.button.single("Close",
                    one.main.admin.id.modal.remove.close, "", "");
            var $closeButton = one.lib.dashlet.button.button(closeButton);
            footer.push($closeButton);

            return footer;
        },
        body : function() {
            var $p = $(document.createElement('p'));
            $p.append('Select an action');
            return $p;
        },
    },
    add : {
        modal : {
            initialize : function() {
                var h3 = "Add User";
                var footer = one.main.admin.add.footer();
                var $body = one.main.admin.add.body();
                var $modal = one.lib.modal.spawn(one.main.admin.id.modal.user,
                        h3, $body, footer);

                // close binding
                $('#' + one.main.admin.id.modal.add.close, $modal).click(
                        function() {
                            $modal.modal('hide');
                        });

                // add binding
                $('#' + one.main.admin.id.modal.add.user, $modal)
                        .click(
                                function() {
                                    one.main.admin.add.modal
                                            .add(
                                                    $modal,
                                                    function(result) {
                                                        if (result == 'Success') {
                                                            $modal
                                                                    .modal('hide');
                                                            // body inject
                                                            var $admin = $('#'
                                                                    + one.main.admin.id.modal.main);
                                                            one.main.admin.ajax
                                                                    .users(function($body) {
                                                                        one.lib.modal.inject
                                                                                .body(
                                                                                        $admin,
                                                                                        $body);
                                                                    });
                                                        } else
                                                            alert("Failed to add user: "
                                                                    + result);
                                                    });
                                });

                $modal.modal();
            },
            add : function($modal, callback) {
                var user = {};
                user['user'] = $modal.find(
                        '#' + one.main.admin.id.modal.add.form.name).val();
                user['password'] = $modal.find(
                        '#' + one.main.admin.id.modal.add.form.password).val();
                user['role'] = $modal.find(
                        '#' + one.main.admin.id.modal.add.form.role).find(
                        'option:selected').attr('value');

				// password check
				var verify = $('#'+one.main.admin.id.modal.add.form.verify).val();
				if (user.password != verify) {
					alert('Passwords do not match');
					return false;
				}

                var resource = {};
                resource['json'] = JSON.stringify(user);
                resource['action'] = 'add'

                one.main.admin.add.modal.ajax(resource, callback);
            },
            ajax : function(data, callback) {
                $.post(one.main.admin.address.root
                        + one.main.admin.address.users, data, function(data) {
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
			// password verify
			var $label = one.lib.form.label('Verify Password');
			var $input = one.lib.form.input('Verify Password');
			$input.attr('id', one.main.admin.id.modal.add.form.verify);
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

            var addButton = one.lib.dashlet.button.single("Add User",
                    one.main.admin.id.modal.add.user, "btn-primary", "");
            var $addButton = one.lib.dashlet.button.button(addButton);
            footer.push($addButton);

            var closeButton = one.lib.dashlet.button.single("Close",
                    one.main.admin.id.modal.add.close, "", "");
            var $closeButton = one.lib.dashlet.button.button(closeButton);
            footer.push($closeButton);

            return footer;
        }
    },
	password : {
		initialize : function(id, successCallback) {
			var h3 = 'Change Password';
			var footer = one.main.admin.password.footer();
			var $body = one.main.admin.password.body(id);;
			var $modal = one.lib.modal.spawn(one.main.admin.id.modal.password.modal,
				h3, $body, footer);

			// cancel binding
			$('#'+one.main.admin.id.modal.password.cancel, $modal).click(function() {
				$modal.modal('hide');
			});

			// change password binding
			$('#'+one.main.admin.id.modal.password.submit, $modal).click(function() {
				one.main.admin.password.submit(id, $modal, function(result) {
					if (result.code == 'SUCCESS') {
						$modal.modal('hide');
						successCallback();
					} else {
						alert(result.code+': '+result.description);
					}
				});
			});

			$modal.modal();
		},
		submit : function(id, $modal, callback) {
			var resource = {};
			resource.newPassword = $('#'+one.main.admin.id.modal.password.form.set, $modal).val();

			// verify password
			var verify = $('#'+one.main.admin.id.modal.password.form.verify, $modal).val();
			if (verify != resource.newPassword) {
				alert('Passwords do not match');
				return false;
			}

			resource.currentPassword = $('#'+one.main.admin.id.modal.password.form.old, $modal).val();

			$.post(one.main.admin.address.password+id, resource, function(data) {
				callback(data);
			});
		},
		body : function(id) {
			var $form = $(document.createElement('form'));
			var $fieldset = $(document.createElement('fieldset'));
			// user
			var $label = one.lib.form.label('Username');
			var $input = one.lib.form.input('');
			$input.attr('disabled', 'disabled');
			$input.val(id);
			$fieldset.append($label)
				.append($input);
			// old password
            var $label = one.lib.form.label('Old Password');
            var $input = one.lib.form.input('Old Password');
            $input.attr('id', one.main.admin.id.modal.password.form.old);
            $input.attr('type', 'password');
            $fieldset.append($label).append($input);
			// new password
            var $label = one.lib.form.label('New Password');
            var $input = one.lib.form.input('New Password');
            $input.attr('id', one.main.admin.id.modal.password.form.set);
            $input.attr('type', 'password');
            $fieldset.append($label).append($input);
			// verify new password
			var $label = one.lib.form.label('Verify Password');
			var $input = one.lib.form.input('Verify Password');
			$input.attr('id', one.main.admin.id.modal.password.form.verify);
			$input.attr('type', 'password');
			$fieldset.append($label).append($input);
			// return
			$form.append($fieldset);
			return $form;
		},
		footer : function() {
			var footer = [];
			var submit = one.lib.dashlet.button.single('Submit',
				one.main.admin.id.modal.password.submit, 'btn-primary', '');
			var $submit = one.lib.dashlet.button.button(submit);
			footer.push($submit);
			var cancel = one.lib.dashlet.button.single('Cancel',
				one.main.admin.id.modal.password.cancel, '', '');
			var $cancel = one.lib.dashlet.button.button(cancel);
			footer.push($cancel);
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
            one.lib.alert("Unable to save configuration: " + data);
        }
    });
});

// logout
$("#logout").click(function() {
    location.href = "/logout";
});

$.ajaxSetup({
    complete : function(xhr, textStatus) {
        var mime = xhr.getResponseHeader('Content-Type');
        if (mime.substring(0, 9) == 'text/html') {
            location.href = '/';
        }
    }
});

/** MAIN PAGE LOAD */
one.main.menu.load();
