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

<%--${portal.angularToolkit()} --%>
${portal.toolkit()}

<%-- TITLE --%>
<div class="page-header">
	<h1>
		<spring:message
			code="label.ldapConfiguration.searchLdapServerIntegrationConfiguration" />
		<small></small>
	</h1>
</div>

<%-- NAVIGATION --%>
<div class="well well-sm" style="display: inline-block">
	<span class="glyphicon glyphicon-plus-sign" aria-hidden="true"></span>&nbsp;<a
		class=""
		href="${pageContext.request.contextPath}/ldap/ldapconfiguration/ldapserverintegrationconfiguration/create"><spring:message
			code="label.event.create" /></a> |&nbsp;&nbsp;
			
	<span class="glyphicon glyphicon-plus-sign" aria-hidden="true"></span>&nbsp;<a
		class=""
		href="javascript:disableDefaultLdapServer()"><spring:message
			code="label.event.disableDefaultLdapServer" /></a> 
			
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

<form id="submitForm" action="" method="POST">
</form>

<script type="text/javascript">

	function testConnection(externalId) {
		$("#submitForm").attr('action','${pageContext.request.contextPath}/ldap/ldapconfiguration/ldapserverintegrationconfiguration/search/testconnection/' + externalId);
		$("#submitForm").submit();
	}
	
	function setDefaultLdapServer(externalId) {
		$("#submitForm").attr('action','${pageContext.request.contextPath}/ldap/ldapconfiguration/ldapserverintegrationconfiguration/search/setdefault/' + externalId);
		$("#submitForm").submit();
	}
	
	function disableDefaultLdapServer() {
		$("#submitForm").attr('action','${pageContext.request.contextPath}/ldap/ldapconfiguration/ldapserverintegrationconfiguration/search/disableDefault');
		$("#submitForm").submit();
	}
	
	function addModal(confirmationMessage, formID) {
		$("body").append('<div class="modal fade" id="confirmModal-' + formID + '"><div class="modal-dialog"><div class="modal-content"><div class="modal-header"><button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button><h4 class="modal-title"><spring:message code="label.confirmation" /></h4></div><div class="modal-body"><p>' + confirmationMessage + '</p></div><div class="modal-footer"><button type="button" class="btn btn-default" data-dismiss="modal"><spring:message code="label.close" /></button><a class="btn btn-danger" onClick="javascript:$("#' + formID + '").submit();"><spring:message code="label.delete" /></a></div></div></div></div>');
	}

	function submitOptions(tableID, formID, attributeName, confirmationMessage) {
		array = $("#" + tableID).DataTable().rows(".selected")[0];	
		$("#" + formID).empty();
		if (array.length>0) {
			$.each(array,function(index, value) {
				externalId = $("#" + tableID).DataTable().row(value).data()["DT_RowId"];
				$("#" + formID).append("<input type='hidden' name='" + attributeName+ "' value='" + externalId + "'/>");
			});
		}
		if (typeof confirmationMessage === 'undefined') {
			$("#" + formID).submit();
		}else {
			addModal(confirmationMessage, formID);
			$("#confirmModal-" + formID).modal('toggle');
		}
	}
</script>

<div class="panel panel-default">
	<form method="get" class="form-horizontal">
		<div class="panel-body">
			<div class="form-group row">
				<div class="col-sm-2 control-label">
					<spring:message
						code="label.LdapServerIntegrationConfiguration.serverID" />
				</div>

				<div class="col-sm-10">
					<input id="ldapServerIntegrationConfiguration_serverID"
						class="form-control" type="text" name="serverid"
						value='<c:out value='${not empty param.serverid ? param.serverid : ldapServerIntegrationConfiguration.serverID }'/>' />
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
		</div>
		<div class="panel-footer">
			<input type="submit" class="btn btn-default" role="button"
				value="<spring:message code="label.search" />" />
		</div>
	</form>
</div>


<c:choose>
	<c:when
		test="${not empty searchldapserverintegrationconfigurationResultsDataSet}">
		<table id="searchldapserverintegrationconfigurationTable"
			class="table responsive table-bordered table-hover">
			<thead>
				<tr>
					<%--!!!  Field names here --%>
					<th><spring:message
							code="label.LdapServerIntegrationConfiguration.serverID" /></th>
					<th><spring:message
							code="label.LdapServerIntegrationConfiguration.baseDomain" /></th>
					<th><spring:message
							code="label.LdapServerIntegrationConfiguration.url" /></th>
					<th><spring:message
							code="label.LdapServerIntegrationConfiguration.default" /></th>
					<%-- Operations Column --%>
					<th></th>
				</tr>
			</thead>
			<tbody>

			</tbody>
		</table>
		<form id="deleteselected"
			action="${pageContext.request.contextPath}/ldap/ldapconfiguration/ldapserverintegrationconfiguration/search/deleteSelected"
			style="display: none;" method="POST"></form>

		<button type="button"
			onClick="javascript:submitOptions('searchldapserverintegrationconfigurationTable', 'deleteselected', 'ldapServerIntegrationConfigurations','Do you want to delete the selected items?')">
			<spring:message
				code='label.ldapConfiguration.searchLdapServerIntegrationConfiguration.deleteSelected' />
		</button>

	</c:when>
	<c:otherwise>
		<div class="alert alert-info" role="alert">

			<spring:message code="label.noResultsFound" />

		</div>

	</c:otherwise>
</c:choose>

<script>
	var searchldapserverintegrationconfigurationDataSet = [
			<c:forEach items="${searchldapserverintegrationconfigurationResultsDataSet}" var="searchResult">
				{
				"DT_RowId" : '<c:out value='${searchResult.externalId}'/>',
"serverid" : "<c:out value='${searchResult.serverID}'/>",
"basedomain" : "<c:out value='${searchResult.baseDomain}'/>",
"url" : "<c:out value='${searchResult.url}'/>",
"defaultConfiguration" : "<spring:message code='label.${searchResult.defaultConfiguration}'/>",
"actions" :
" <a  class=\"btn btn-default btn-xs\" href=\"${pageContext.request.contextPath}/ldap/ldapconfiguration/ldapserverintegrationconfiguration/search/view/${searchResult.externalId}\"><spring:message code='label.view'/></a>" +
" <a  class=\"btn btn-default btn-xs\" href=\"javascript:setDefaultLdapServer(${searchResult.externalId})\"><spring:message code='label.setDefault'/></a>" +
" <a  class=\"btn btn-default btn-xs\" href=\"javascript:testConnection(${searchResult.externalId})\"><spring:message code='label.testConnection'/></a>" +
"" },
            </c:forEach>
    ];
	
	$(document).ready(function() {

	


		var table = $('#searchldapserverintegrationconfigurationTable').DataTable({language : {
			url : "${datatablesI18NUrl}",			
		},
		"columns": [
			{ data: 'serverid' },
			{ data: 'basedomain' },
			{ data: 'url' },
			{ data: 'defaultConfiguration' },
			{ data: 'actions' }
			
		],
		"data" : searchldapserverintegrationconfigurationDataSet,
		//Documentation: https://datatables.net/reference/option/dom
//"dom": '<"col-sm-6"l><"col-sm-3"f><"col-sm-3"T>rtip', //FilterBox = YES && ExportOptions = YES
"dom": 'T<"clear">lrtip', //FilterBox = NO && ExportOptions = YES
//"dom": '<"col-sm-6"l><"col-sm-6"f>rtip', //FilterBox = YES && ExportOptions = NO
//"dom": '<"col-sm-6"l>rtip', // FilterBox = NO && ExportOptions = NO
        "tableTools": {
            "sSwfPath": "//cdn.datatables.net/tabletools/2.2.3/swf/copy_csv_xls_pdf.swf"
        }
		});
		table.columns.adjust().draw();
		
		  $('#searchldapserverintegrationconfigurationTable tbody').on( 'click', 'tr', function () {
		        $(this).toggleClass('selected');
		    } );
		  
	}); 
</script>

