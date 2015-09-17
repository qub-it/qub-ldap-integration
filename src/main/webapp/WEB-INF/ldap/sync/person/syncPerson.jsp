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
		class="" href='javascript:sendToLdap();'><spring:message
			code="label.event.sentToLdap" /></a> | &nbsp; &nbsp; <span
		class="glyphicon glyphicon-cog" aria-hidden="true"></span>&nbsp;<a
		class="" href='javascript:receiveFromLdap();'><spring:message
			code="label.event.receiveFromLdap" /></a> | &nbsp; &nbsp; <span
		class="glyphicon glyphicon-cog" aria-hidden="true"></span>&nbsp;<a
		class="" href='javascript:removeFromLdap();'><spring:message
			code="label.event.removeFromLdap" /></a>
			| &nbsp; &nbsp; <span
		class="glyphicon glyphicon-cog" aria-hidden="true"></span>&nbsp;<a
		class="" href='javascript:resetUsername();'><spring:message
			code="label.event.resetUsername" /></a>
	<c:if test="${not empty studentSyncInformation}">
		| &nbsp; &nbsp; <span class="glyphicon glyphicon-cog"
			aria-hidden="true"></span>&nbsp;<a class=""
			href='javascript:sendStudenToLdap();'><spring:message
				code="label.event.sendStudenToLdap" /></a>
	</c:if>
</div>

<script type="text/javascript">
	function sendToLdap() {
		$('#sendToLdap').modal('toggle')
	}

	function removeFromLdap() {
		$('#removeFromLdap').modal('toggle')
	}

	function receiveFromLdap() {
		$('#receiveFromLdap').modal('toggle')
	}

	function resetUsername() {
		$('#resetUsername').modal('toggle')
	}

	<c:if test="${not empty studentSyncInformation}">
	function sendStudenToLdap() {
		$('#sendStudenToLdap').modal('toggle')
	}
	</c:if>
</script>

<c:if test="${not empty studentSyncInformation}">
	<div class="modal fade" id="sendStudenToLdap">
		<div class="modal-dialog">
			<div class="modal-content">
				<div class="modal-header">
					<button type="button" class="close" data-dismiss="modal"
						aria-label="Close">
						<span aria-hidden="true">&times;</span>
					</button>
					<h4 class="modal-title">
						<spring:message code="label.confirmation" />
					</h4>
				</div>
				<div class="modal-body">
					<p>
						<spring:message code="label.confirmation.sendToLdap" />
					</p>
				</div>
				<div class="modal-footer">
					<button type="button" class="btn btn-default" data-dismiss="modal">
						<spring:message code="label.close" />
					</button>
					<a id="deleteLink" class="btn btn-danger"
						href='javascript:$("#sendStudentToLdapForm").submit()'> <spring:message
							code="label.send" /></a>
				</div>
			</div>
			<!-- /.modal-content -->
		</div>
		<!-- /.modal-dialog -->
	</div>
	<!-- /.modal -->
</c:if>


<div class="modal fade" id="sendToLdap">
	<div class="modal-dialog">
		<div class="modal-content">
			<div class="modal-header">
				<button type="button" class="close" data-dismiss="modal"
					aria-label="Close">
					<span aria-hidden="true">&times;</span>
				</button>
				<h4 class="modal-title">
					<spring:message code="label.confirmation" />
				</h4>
			</div>
			<div class="modal-body">
				<p>
					<spring:message code="label.confirmation.sendToLdap" />
				</p>
			</div>
			<div class="modal-footer">
				<button type="button" class="btn btn-default" data-dismiss="modal">
					<spring:message code="label.close" />
				</button>
				<a id="deleteLink" class="btn btn-danger"
					href='javascript:$("#sendToLdapForm").submit()'> <spring:message
						code="label.send" /></a>
			</div>
		</div>
		<!-- /.modal-content -->
	</div>
	<!-- /.modal-dialog -->
</div>
<!-- /.modal -->

<div class="modal fade" id="removeFromLdap">
	<div class="modal-dialog">
		<div class="modal-content">
			<div class="modal-header">
				<button type="button" class="close" data-dismiss="modal"
					aria-label="Close">
					<span aria-hidden="true">&times;</span>
				</button>
				<h4 class="modal-title">
					<spring:message code="label.confirmation" />
				</h4>
			</div>
			<div class="modal-body">
				<p>
					<spring:message code="label.confirmation.removeFromLdap" />
				</p>
			</div>
			<div class="modal-footer">
				<button type="button" class="btn btn-default" data-dismiss="modal">
					<spring:message code="label.close" />
				</button>
				<a id="deleteLink" class="btn btn-danger"
					href='javascript:$("#removeFromLdapForm").submit()'> <spring:message
						code="label.delete" /></a>
			</div>
		</div>
		<!-- /.modal-content -->
	</div>
	<!-- /.modal-dialog -->
</div>
<!-- /.modal -->

<div class="modal fade" id="receiveFromLdap">
	<div class="modal-dialog">
		<div class="modal-content">
			<div class="modal-header">
				<button type="button" class="close" data-dismiss="modal"
					aria-label="Close">
					<span aria-hidden="true">&times;</span>
				</button>
				<h4 class="modal-title">
					<spring:message code="label.confirmation" />
				</h4>
			</div>
			<div class="modal-body">
				<p>
					<spring:message code="label.confirmation.receiveFromLdap" />
				</p>
			</div>
			<div class="modal-footer">
				<button type="button" class="btn btn-default" data-dismiss="modal">
					<spring:message code="label.close" />
				</button>
				<a id="deleteLink" class="btn btn-danger"
					href='javascript:$("#receiveFromLdapForm").submit()'> <spring:message
						code="label.receive" /></a>
			</div>
		</div>
		<!-- /.modal-content -->
	</div>
	<!-- /.modal-dialog -->
</div>
<!-- /.modal -->

<div class="modal fade" id="resetUsername">
	<div class="modal-dialog">
		<div class="modal-content">
			<div class="modal-header">
				<button type="button" class="close" data-dismiss="modal"
					aria-label="Close">
					<span aria-hidden="true">&times;</span>
				</button>
				<h4 class="modal-title">
					<spring:message code="label.confirmation" />
				</h4>
			</div>
			<div class="modal-body">
				<p>
					<spring:message code="label.confirmation.resetUsername" />
				</p>
			</div>
			<div class="modal-footer">
				<button type="button" class="btn btn-default" data-dismiss="modal">
					<spring:message code="label.close" />
				</button>
				<a id="deleteLink" class="btn btn-danger"
					href='javascript:$("#resetUsernameForm").submit()'> <spring:message
						code="label.reset" /></a>
			</div>
		</div>
		<!-- /.modal-content -->
	</div>
	<!-- /.modal-dialog -->
</div>
<!-- /.modal -->

<form id="sendToLdapForm"
	action="${pageContext.request.contextPath}/ldap/sync/person/sendtoldap/${person.externalId}"
	method="POST"></form>

<form id="receiveFromLdapForm"
	action="${pageContext.request.contextPath}/ldap/sync/person/receivefromldap/${person.externalId}"
	method="POST"></form>

<form id="removeFromLdapForm"
	action="${pageContext.request.contextPath}/ldap/sync/person/removefromldap/${person.externalId}"
	method="POST"></form>

<form id="resetUsernameForm"
	action="${pageContext.request.contextPath}/ldap/sync/person/resetUsername/${person.externalId}"
	method="POST"></form>


<c:if test="${not empty studentSyncInformation}">
	<form id="sendStudentToLdapForm"
		action="${pageContext.request.contextPath}/ldap/sync/person/sendstudenttoldap/${person.externalId}"
		method="POST"></form>
</c:if>


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
			<spring:message code="label.details" /> (${person.username})
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


<c:if test="${not empty studentSyncInformation}">
	<div class="panel panel-primary">
		<div class="panel-heading">
			<h3 class="panel-title">
				<spring:message code="label.student.details" />
			</h3>
		</div>
		<div class="panel-body">
			<table class="table responsive table-bordered table-hover">
				<tr>
					<th><spring:message code="label.attributes" /></th>
					<th><spring:message code="label.attributes.fenix" /></th>
					<th><spring:message code="label.attributes.ldap" /></th>
				</tr>
				<c:forEach var="attribute" items="${studentSyncInformation}">

					<tr>
						<th>${attribute.key}</th>
						<td>${attribute.value[0]}</td>
						<td>${attribute.value[1]}</td>
					</tr>
				</c:forEach>
			</table>
		</div>
	</div>
</c:if>