<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jstl/fmt"%>
<spring:url var="datatablesUrl"
	value="/javaScript/dataTables/media/js/jquery.dataTables.latest.min.js" />
<spring:url var="datatablesBootstrapJsUrl"
	value="/javaScript/dataTables/media/js/jquery.dataTables.bootstrap.min.js"></spring:url>
<script type="text/javascript" src="${datatablesUrl}"></script>
<script type="text/javascript" src="${datatablesBootstrapJsUrl}"></script>
<spring:url var="datatablesCssUrl"
	value="/CSS/dataTables/dataTables.bootstrap.min.css" />
<link rel="stylesheet" href="${datatablesCssUrl}" />
<spring:url var="datatablesI18NUrl"
	value="/javaScript/dataTables/media/i18n/${portal.locale.language}.json" />

<link rel="stylesheet" type="text/css"
	href="${pageContext.request.contextPath}/CSS/dataTables/dataTables.bootstrap.min.css" />

<link
	href="//cdn.datatables.net/responsive/1.0.4/css/dataTables.responsive.css"
	rel="stylesheet" />
<script
	src="//cdn.datatables.net/responsive/1.0.4/js/dataTables.responsive.js"></script>
<link
	href="//cdn.datatables.net/tabletools/2.2.3/css/dataTables.tableTools.css"
	rel="stylesheet" />
<script
	src="//cdn.datatables.net/tabletools/2.2.3/js/dataTables.tableTools.min.js"></script>
<link
	href="//cdnjs.cloudflare.com/ajax/libs/select2/4.0.0-rc.1/css/select2.min.css"
	rel="stylesheet" />
<script
	src="//cdnjs.cloudflare.com/ajax/libs/select2/4.0.0-rc.1/js/select2.min.js"></script>

<!-- Choose ONLY ONE:  bennuToolkit OR bennuAngularToolkit -->
<%--${portal.angularToolkit()} --%>
${portal.toolkit()}

<%-- TITLE --%>
<div class="page-header">
	<h1>
		<spring:message code="label.ldap.sync.syncPerson" />
		<small></small>
	</h1>
</div>

<%-- NAVIGATION --%>
<div class="well well-sm" style="display: inline-block">
	<span class="glyphicon glyphicon-arrow-left" aria-hidden="true"></span>&nbsp;<a
		class="" href="${pageContext.request.contextPath}/ldap/sync/person/"><spring:message
			code="label.event.back" /></a> | &nbsp; &nbsp; <span
		class="glyphicon glyphicon-cog" aria-hidden="true"></span>&nbsp;<a
		class="" href='javascript:$("#sendToLdap").submit()'><spring:message
			code="label.event.sentToLdap" /></a> | &nbsp; &nbsp; <span
		class="glyphicon glyphicon-cog" aria-hidden="true"></span>&nbsp;<a
		class="" href='javascript:$("#receiveFromLdap").submit()'><spring:message
			code="label.event.receiveFromLdap" /></a>
</div>

<form id="sendToLdap"
	action="${pageContext.request.contextPath}/ldap/sync/person/sendtoldap/${person.externalId}"
	method="POST"></form>

<form id="receiveFromLdap"
	action="${pageContext.request.contextPath}/ldap/sync/person/receivefromldap/${person.externalId}"
	method="POST"></form>

<c:if test="${not empty infoMessages}">
	<div class="alert alert-info" role="alert">

		<c:forEach items="${infoMessages}" var="message">
			<p>${message}</p>
		</c:forEach>

	</div>
</c:if>
<c:if test="${not empty warningMessages}">
	<div class="alert alert-warning" role="alert">

		<c:forEach items="${warningMessages}" var="message">
			<p>${message}</p>
		</c:forEach>

	</div>
</c:if>
<c:if test="${not empty errorMessages}">
	<div class="alert alert-danger" role="alert">

		<c:forEach items="${errorMessages}" var="message">
			<p>${message}</p>
		</c:forEach>

	</div>
</c:if>

<div class="panel panel-primary">
	<div class="panel-heading">
		<h3 class="panel-title">
			<spring:message code="label.details" />
		</h3>
	</div>
	<div class="panel-body">


		<table class="table responsive table-bordered table-hover">
			<tr>
				<th><spring:message code="label.attributes" /></th>
				<th><spring:message code="label.attributes.fenix" /></th>
				<th><spring:message code="label.attributes.ldap" /></th>
			</tr>
			<c:forEach var="attribute" items="${syncInformation}">

				<tr>
					<th>${attribute.key}</th>
					<td>${attribute.value[0]}</td>
					<td>${attribute.value[1]}</td>
				</tr>
			</c:forEach>
		</table>


	</div>
</div>
