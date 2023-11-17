<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jstl/fmt" %>
<spring:url var="datatablesUrl" value="/javaScript/dataTables/media/js/jquery.dataTables.latest.min.js"/>
<spring:url var="datatablesBootstrapJsUrl" value="/javaScript/dataTables/media/js/jquery.dataTables.bootstrap.min.js"></spring:url>
<script type="text/javascript" src="${datatablesUrl}"></script>
<script type="text/javascript" src="${datatablesBootstrapJsUrl}"></script>
<spring:url var="datatablesCssUrl" value="/CSS/dataTables/dataTables.bootstrap.min.css"/>
<link rel="stylesheet" href="${datatablesCssUrl}"/>
<spring:url var="datatablesI18NUrl" value="/javaScript/dataTables/media/i18n/${portal.locale.language}.json"/>

<link rel="stylesheet" type="text/css"
	href="${pageContext.request.contextPath}/CSS/dataTables/dataTables.bootstrap.min.css"/>

<link href="//cdn.datatables.net/responsive/1.0.4/css/dataTables.responsive.css" rel="stylesheet"/>
<script src="//cdn.datatables.net/responsive/1.0.4/js/dataTables.responsive.js"></script>
<link href="//cdn.datatables.net/tabletools/2.2.3/css/dataTables.tableTools.css" rel="stylesheet"/>
<script src="//cdn.datatables.net/tabletools/2.2.3/js/dataTables.tableTools.min.js"></script>
<link href="//cdnjs.cloudflare.com/ajax/libs/select2/4.0.0-rc.1/css/select2.min.css" rel="stylesheet" />
<script src="//cdnjs.cloudflare.com/ajax/libs/select2/4.0.0-rc.1/js/select2.min.js"></script>

<!-- Choose ONLY ONE:  bennuToolkit OR bennuAngularToolkit -->
<%--${portal.angularToolkit()} --%>
${portal.toolkit()}

<%-- TITLE --%>
<div class="page-header">
	<h1><spring:message code="label.ldap.sync.searchPerson" />
		<small></small>
	</h1>
</div>
<%-- NAVIGATION --%>
<c:if test="${allowedMassiveOperations}">
<div class="well well-sm" style="display: inline-block">
		<span
		class="glyphicon glyphicon-cog" aria-hidden="true"></span>&nbsp;<a
		class="" href='javascript:sendAllUsersToLdap();'><spring:message
			code="label.event.sendAllUsersToLdap" /></a> | &nbsp; &nbsp; <span
		class="glyphicon glyphicon-cog" aria-hidden="true"></span>&nbsp;<a
		class="" href='javascript:deleteAllUsersFromLdap();'><spring:message
			code="label.event.deleteAllUsersFromLdap" /></a> 
</div>

<script type="text/javascript">
	function sendAllUsersToLdap() {
		$('#sendAllUsersToLdap').modal('toggle')
	}

	function deleteAllUsersFromLdap() {
		$('#deleteAllUsersFromLdap').modal('toggle')
	}
</script>

<div class="modal fade" id="sendAllUsersToLdap">
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
					<spring:message code="label.confirmation.sendAllToLdap" />
				</p>
			</div>
			<div class="modal-footer">
				<button type="button" class="btn btn-default" data-dismiss="modal">
					<spring:message code="label.close" />
				</button>
				<a id="deleteLink" class="btn btn-danger"
					href='javascript:$("#sendAllToLdapForm").submit()'> <spring:message
						code="label.send" /></a>
			</div>
		</div>
		<!-- /.modal-content -->
	</div>
	<!-- /.modal-dialog -->
</div>
<!-- /.modal -->

<div class="modal fade" id="deleteAllUsersFromLdap">
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
					<spring:message code="label.confirmation.deleteAllUsersFromLdap" />
				</p>
			</div>
			<div class="modal-footer">
				<button type="button" class="btn btn-default" data-dismiss="modal">
					<spring:message code="label.close" />
				</button>
				<a id="deleteLink" class="btn btn-danger"
					href='javascript:$("#deleteAllUsersFromLdapForm").submit()'> <spring:message
						code="label.send" /></a>
			</div>
		</div>
		<!-- /.modal-content -->
	</div>
	<!-- /.modal-dialog -->
</div>
<!-- /.modal -->
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


<script type="text/javascript">
	function processDelete(externalId) {
		url = "${pageContext.request.contextPath}/ldap/sync/person/search/delete/" + externalId;
		$("#deleteLink").attr("href", url);
		$('#deleteModal').modal('toggle')
	}
</script>


<div class="panel panel-default">
<form method="get" class="form-horizontal">
<div class="panel-body">
<div class="form-group row">
<div class="col-sm-2 control-label"><spring:message code="label.Person.name"/></div> 

<div class="col-sm-10">
	<input id="person_name" class="form-control" type="text" name="name"  value='<c:out value='${not empty param.name ? param.name : person.name }'/>' />
</div>	
</div>		
<div class="form-group row">
<div class="col-sm-2 control-label"><spring:message code="label.Person.username"/></div> 

<div class="col-sm-10">
	<input id="person_username" class="form-control" type="text" name="username"  value='<c:out value='${not empty param.username ? param.username : person.username }'/>' />
</div>	
</div>		
<div class="form-group row">
<div class="col-sm-2 control-label"><spring:message code="label.Person.idDocumentNumbber"/></div> 

<div class="col-sm-10">
	<input id="person_idDocumentNumbber" class="form-control" type="text" name="documentidnumber"  value='<c:out value='${not empty param.documentidnumber ? param.documentidnumber : person.documentIdNumber }'/>' />
</div>	
</div>		
</div>
<div class="panel-footer">
	<input type="submit" class="btn btn-default" role="button" value="<spring:message code="label.search" />"/>
</div>
</form>
</div>


<c:choose>
	<c:when test="${not empty searchpersonResultsDataSet}">
		<table id="searchpersonTable" class="table responsive table-bordered table-hover">
			<thead>
				<tr>
					<%--!!!  Field names here --%>
<th><spring:message code="label.Person.name"/></th>
<th><spring:message code="label.Person.username"/></th>
<th><spring:message code="label.Person.idDocumentNumber"/></th>
<%-- Operations Column --%>
					<th></th>
				</tr>
			</thead>
			<tbody>
				
			</tbody>
		</table>
	</c:when>
	<c:otherwise>
				<div class="alert alert-info" role="alert">
					
					<spring:message code="label.noResultsFound"/>
					
				</div>	
		
	</c:otherwise>
</c:choose>


<c:if test="${allowedMassiveOperations}">
<form id="sendAllToLdapForm"
	action="${pageContext.request.contextPath}/ldap/sync/person/search/sendalluserstoldap/"
	method="POST"></form>
	
	
<form id="deleteAllUsersFromLdapForm"
	action="${pageContext.request.contextPath}/ldap/sync/person/search/removeallusersfromldap/"
	method="POST"></form>
</c:if>
	
<script>
	var searchpersonDataSet = [
			<c:forEach items="${searchpersonResultsDataSet}" var="searchResult">
				<%-- Field access / formatting  here CHANGE_ME --%>
				{
				"DT_RowId" : '<c:out value='${searchResult.externalId}'/>',
"name" : "<c:out value='${searchResult.name}'/>",
"username" : "<c:out value='${searchResult.username}'/>",
"documentidnumber" : "<c:out value='${searchResult.documentIdNumber}'/>",
"actions" :
" <a  class=\"btn btn-default btn-xs\" href=\"${pageContext.request.contextPath}/ldap/sync/person/search/view/${searchResult.externalId}\"><spring:message code='label.view'/></a>" +
                "" },
            </c:forEach>
    ];
	
	$(document).ready(function() {

	


		var table = $('#searchpersonTable').DataTable({language : {
			url : "${datatablesI18NUrl}",			
		},
		"columns": [
			{ data: 'name' },
			{ data: 'username' },
			{ data: 'documentidnumber' },
			{ data: 'actions' }
			
		],
		"data" : searchpersonDataSet,
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
		
		  $('#searchpersonTable tbody').on( 'click', 'tr', function () {
		        $(this).toggleClass('selected');
		    } );
		  
	}); 
</script>

