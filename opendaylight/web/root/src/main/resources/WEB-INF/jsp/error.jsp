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

<c:set var="title" value="OpenDaylight login error" scope="application"/>
<%
    String filePath = "/WEB-INF/jsp/customerror.jsp"; 
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
        <form action="<c:url value='/' />" id="form" method="post">

  <div class="container">
    <div class="content">
       <div class="login-form">
         <div id="logo"></div>
           <fieldset>
            <p>Authentication failed or session might have timed out.<p>
            <p>Please go back to the login page and try again.</p>
            <br/>
             <button class="btn btn-primary" type="submit" value="Log In" >Go To Login Page</button>
           </fieldset>
       </div>
    </div>
  </div> 
  </form>
</body>
</html>
