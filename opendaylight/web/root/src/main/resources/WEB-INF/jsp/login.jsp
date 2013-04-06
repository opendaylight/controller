<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.net.URL" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!DOCTYPE html>
<html>
  <head>
    <meta charset="utf-8">

    <!-- Bootstrap CSS - 1 -->
	<link href="/css/bootstrap.min.css" rel="stylesheet" media="screen">

	<!-- Login CSS - 2 -->
	<link rel="stylesheet/less" type="text/css" href="/css/login.less">

<c:set var="title" value="OpenDaylight-login" scope="application"/>
<%
    String filePath = "/WEB-INF/jsp/customlogin.jsp"; 
	URL fileURL = pageContext.getServletContext().getResource(filePath);
	if(fileURL!=null) {
%>
	  <jsp:include page="<%=filePath%>" flush="true"/>
<% } %>
	<title>${title}</title>
	<!-- Bootstrap JS - 1 -->
	<script src="/js/bootstrap.min.js"></script>
	
	<!-- LESS - 2 -->
	<script type="text/javascript">
		less = {
			env: "production"
		};
	</script>
	<script src="/js/less-1.3.3.min.js"></script>
</head>
<body>
  <form action="<c:url value='j_security_check' />" id="form" method="post">

  <div class="container">
    <div class="content">
       <div class="login-form">
         <div id="logo"></div>
           <fieldset>
             <div class="control-group">
               <input type="text" name="j_username" placeholder="Username">
             </div>
             <div class="control-group">
               <input type="password" name="j_password" placeholder="Password">
             </div>
             <button class="btn btn-primary" type="submit" value="Log In" ><div class="icon-login"></div> Log In</button>
           </fieldset>
       </div>
    </div>
  </div> 
  </form>
</body>
</html>
