<%--
 * This file was created by Quorum Born IT <http://www.qub-it.com/> and its 
 * copyright terms are bind to the legal agreement regulating the FenixEdu@ULisboa 
 * software development project between Quorum Born IT and Serviços Partilhados da
 * Universidade de Lisboa:
 *  - Copyright © 2015 Quorum Born IT (until any Go-Live phase)
 *  - Copyright © 2015 Universidade de Lisboa (after any Go-Live phase)
 *
 * Contributors: paulo.abrantes@qub-it.com
 *
 * 
 * This file is part of FenixEdu fenixedu-ulisboa-ldapIntegration.
 *
 * FenixEdu fenixedu-ulisboa-ldapIntegration is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu fenixedu-ulisboa-ldapIntegration is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu fenixedu-ulisboa-ldapIntegration.  If not, see <http://www.gnu.org/licenses/>.
 *--%>
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

<%-- TITLE --%>
<div class="page-header">
	<h1>
		<spring:message
			code="label.ldapConfiguration.updateLdapServerIntegrationConfiguration" />
		<small></small>
	</h1>
</div>

<%-- NAVIGATION --%>
<div class="well well-sm" style="display: inline-block">
	<span class="glyphicon glyphicon-arrow-left" aria-hidden="true"></span>&nbsp;<a
		class=""
		href="${pageContext.request.contextPath}/ldap/ldapconfiguration/ldapserverintegrationconfiguration/read/${ldapServerIntegrationConfiguration.externalId}"><spring:message
			code="label.event.back" /></a> |&nbsp;&nbsp;
</div>
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

<form method="post" class="form-horizontal">
	<div class="panel panel-default">
		<div class="panel-body">
					<div class="form-group row">
				<div class="col-sm-2 control-label">
					<spring:message
						code="label.LdapServerIntegrationConfiguration.serverID" />
				</div>

				<div class="col-sm-10">
					<input id="ldapServerIntegrationConfiguration_serverid"
						class="form-control" type="text" name="serverid"
						value='<c:out value='${not empty param.serverid ? param.serverid : ldapServerIntegrationConfiguration.serverID}'/>' />
				</div>
			</div>
		
			<div class="form-group row">
				<div class="col-sm-2 control-label">
					<spring:message
						code="label.LdapServerIntegrationConfiguration.username" />
				</div>

				<div class="col-sm-10">
					<input id="ldapServerIntegrationConfiguration_username"
						class="form-control" type="text" name="username"
						value='<c:out value='${not empty param.username ? param.username : ldapServerIntegrationConfiguration.username }'/>' />
				</div>
			</div>
			<div class="form-group row">
				<div class="col-sm-2 control-label">
					<spring:message
						code="label.LdapServerIntegrationConfiguration.password" />
				</div>

				<div class="col-sm-10">
					<input id="ldapServerIntegrationConfiguration_password"
						class="form-control" type="password" name="password"
						value='<c:out value='${not empty param.password ? param.password : ldapServerIntegrationConfiguration.password }'/>' />
				</div>
			</div>

			<div class="form-group row">
				<div class="col-sm-2 control-label">
					<spring:message
						code="label.LdapServerIntegrationConfiguration.passwordConfirmation" />
				</div>

				<div class="col-sm-10">
					<input id="ldapServerIntegrationConfiguration_passwordConfirmation"
						class="form-control" type="password" name="passwordConfirmation"
						value='<c:out value='${not empty param.passwordConfirmation ? param.passwordConfirmation : ldapServerIntegrationConfiguration.password }'/>' />
				</div>
			</div>

			<div class="form-group row">
				<div class="col-sm-2 control-label">
					<spring:message code="label.LdapServerIntegrationConfiguration.url" />
				</div>

				<div class="col-sm-10">
					<input id="ldapServerIntegrationConfiguration_url"
						class="form-control" type="text" name="url"
						value='<c:out value='${not empty param.url ? param.url : ldapServerIntegrationConfiguration.url }'/>' />
				</div>
			</div>
			<div class="form-group row">
				<div class="col-sm-2 control-label">
					<spring:message
						code="label.LdapServerIntegrationConfiguration.baseDomain" />
				</div>

				<div class="col-sm-10">
					<input id="ldapServerIntegrationConfiguration_baseDomain"
						class="form-control" type="text" name="basedomain"
						value='<c:out value='${not empty param.basedomain ? param.basedomain : ldapServerIntegrationConfiguration.baseDomain }'/>' />
				</div>
			</div>
			<div class="form-group row">
				<div class="col-sm-2 control-label">
					<spring:message
						code="label.LdapServerIntegrationConfiguration.numberOfWorkers" />
				</div>

				<div class="col-sm-10">
					<input id="ldapServerIntegrationConfiguration_numberOfWorkers"
						class="form-control" type="text" name="numberOfWorkers"
						value='<c:out value='${not empty param.numberOfWorkers ? param.numberOfWorkers : ldapServerIntegrationConfiguration.numberOfWorkers }'/>' />
				</div>
			</div>
		</div>
		<div class="panel-footer">
			<input type="submit" class="btn btn-default" role="button"
				value="<spring:message code="label.submit" />" />
		</div>
	</div>
</form>

