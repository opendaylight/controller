<%--
 - Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved. 
 - 
 - This program and the accompanying materials are made available under the 
 - terms of the Eclipse Public License v1.0 which accompanies this distribution, 
 - and is available at http://www.eclipse.org/legal/epl-v10.html
--%>
<%@ page import="java.net.URL" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!DOCTYPE html>
<html>

<head>
	<meta name="viewport" content="width=device-width, initial-scale=1.0">

	<!-- Bootstrap CSS - 1 -->
	<link href="/css/bootstrap.min.css" rel="stylesheet" media="screen">

	<!-- Core CSS - 2 -->
	<link rel="stylesheet/less" type="text/css" href="/css/one.less">

<c:set var="version" value="Version 0.1" scope="application"/>
<c:set var="name" value="OpenDaylight" scope="application"/>

<%	
    String filePath = "/WEB-INF/jsp/custommain.jsp"; 
    URL fileURL = pageContext.getServletContext().getResource(filePath);
    if(fileURL!=null) {
%>
	  <jsp:include page="<%=filePath%>" flush="true"/>
<% } %>

    <title>${name}</title>
	<!-- jQuery - 1 -->
	<script src="/js/jquery-1.9.1.min.js"></script>
	
	<!-- Bootstrap JS - 2 -->
	<script src="/js/bootstrap.min.js"></script>

	<!-- LESS - 3 -->
	<script type="text/javascript">
		less = {
			env: "production"
		};
	</script>
	<script src="/js/less-1.3.3.min.js"></script>
	
	<!-- Topology - 4 -->
	<script src="/js/jit.js"></script>
</head>
<body>

<!-- #menu -->
<div id="menu" class="navbar navbar-fixed-top">
	<div class="navbar-inner row-fluid">
		<div class="span10">
			<a class="brand" href="/" title="${version}">${name}</a> 
			<ul class="nav nav-tabs">
			</ul>
		</div>
		<div class="span2">
			<div id="toolbar" class="btn-group">
				<a class="btn dropdown-toggle" data-toggle="dropdown" href="#">
					<div class="icon-user"></div> ${username}
					<span class="caret"></span>
				</a>
				<ul class="dropdown-menu">
					<li><a href="#admin" id="admin" data-role="${role}"><div class="icon-users"></div> Users</a></li>
					<li><a href="#save" id="save"><div class="icon-save"></div> Save</a></li>
					<li><a href="#logout" id="logout"><div class="icon-logout"></div> Logout</a></li>
				</ul>
			</div>
		</div>
	</div>
</div><!-- END #menu -->

<!-- #footer -->
<div id="footer" class="navbar navbar-fixed-bottom">
	<div class="navbar-inner row-fluid">
		<div class="alert hide" id="alert">
			<button type="button" class="close">&times;</button>
			<p></p>
		</div>
	</div>
</div><!-- END #footer -->

<!-- #main -->
<div id="main">
	
<!-- #left -->
<div id="left">

	<!-- #left-top -->
	<div id="left-top">

		<div class="dash">
			<ul class="nav nav-tabs">
			</ul>
			<div class="dashlet row-fluid">
			</div>
		</div>

	</div><!-- END #left-top -->

	<!-- #left-bottom -->
	<div id="left-bottom">

		<div class="dash">
			<ul class="nav nav-tabs">
			</ul>
			<div class="dashlet row-fluid">
			</div>
		</div>

	</div><!-- END #left-bottom -->

</div><!-- END #left -->

<!-- #right -->
<div id="right">

	<!-- #right-top -->
	<div id="right-top">

		<div class="dash">
			<div id="topology"></div>
		</div>

	</div><!-- END #right-top -->

	<!-- #right-bottom -->
	<div id="right-bottom">

		<div class="dash">
			<ul class="nav nav-tabs">
			</ul>
			<div class="dashlet row-fluid">
			</div>
		</div>

	</div><!-- END #right-bottom -->

</div><!-- END #right -->

</div><!-- END #main -->

<!-- modal -->
<div id="modal" class="modal hide fade">
	<div class="modal-header">
		<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
		<h3></h3>
	</div>
	<div class="modal-body"></div>
	<div class="modal-footer"></div>
</div>
<!-- END modal -->

<!-- Core JS - 6 -->
<script src="/js/one.js"></script>

<!-- Topology JS - 7 -->
<script src="/js/one-topology.js"></script>

</body>

</html>