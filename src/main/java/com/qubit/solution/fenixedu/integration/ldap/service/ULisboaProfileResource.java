/**
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
 */
package com.qubit.solution.fenixedu.integration.ldap.service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.FenixEduAcademicExtensionsConfiguration;
import org.fenixedu.academic.FenixEduAcademicExtensionsConfiguration.ConfigurationProperties;
import org.fenixedu.bennu.core.api.ProfileResource;
import org.fenixedu.bennu.core.api.json.AuthenticatedUserViewer;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.domain.UsernameHack;
import org.fenixedu.bennu.core.domain.exceptions.AuthorizationException;
import org.fenixedu.bennu.core.groups.Group;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.bennu.core.util.CoreConfiguration;


import com.google.gson.JsonElement;
import com.qubit.solution.fenixedu.integration.ldap.domain.configuration.LdapServerIntegrationConfiguration;
import com.qubit.terra.ldapclient.LdapClient;
import com.qubit.terra.ldapclient.QueryReply;
import com.qubit.terra.ldapclient.QueryReplyElement;

import pt.ist.fenixframework.Atomic;

@Path("/bennu-core/profile/extension")
public class ULisboaProfileResource extends ProfileResource {

    private static final String FENIX_USER_ATTRIBUTE = "ULFenixUser";

    @Context
    HttpServletRequest request;

    // In the JSPs this url will be linked through a login.html put in the
    // main theme of the ULisboa project. Which will basically only changes
    // the login URL.
    //
    // 22 June 2015 - Paulo Abrantes
    @Override
    @POST
    @Path("loginExtension")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonElement login(@Context final HttpServletRequest request, @Context final HttpServletResponse response,
            @FormParam("username") final String username, @FormParam("password") final String password) {
        LdapServerIntegrationConfiguration defaultLdapServer = Bennu.getInstance().getDefaultLdapServerIntegrationConfiguration();
        ConfigurationProperties configuration = FenixEduAcademicExtensionsConfiguration.getConfiguration();

        if (isValidQualityAuthentication(username, password, configuration)) {
            Authenticate.login(request, response, User.findByUsername(username), "TODO: CHANGE ME");
            return view(null, Void.class, AuthenticatedUserViewer.class);
        } else if (defaultLdapServer == null || Boolean.TRUE == CoreConfiguration.getConfiguration().developmentMode()) {
            return super.login(request, response, username, password);
        } else {
            LdapClient client = defaultLdapServer.getClient();
            String ldapUsername = username.replace("@campus.ul.pt", "").toLowerCase().trim();
            boolean verifyCredentials = client.verifyCredentials(ldapUsername, password);

            if (verifyCredentials) {
                User user = User.findByUsername(ldapUsername);
                if (user == null) {
                    // We have been able to login into LDAP but there's no matching
                    // user in Fenix yet, this happens when there was an user alignment
                    // so it's time for us to login into ldap and check that.
                    if (client.login()) {
                        try {
                            QueryReply query = client.query("(cn=" + ldapUsername + ")", new String[] { FENIX_USER_ATTRIBUTE });

                            if (query.getNumberOfResults() == 1) {
                                QueryReplyElement queryReplyElement = query.getResults().get(0);
                                String fenixUsername = queryReplyElement.getSimpleAttribute(FENIX_USER_ATTRIBUTE);
                                if (!fenixUsername.equals(ldapUsername)) {
                                    usernameAlign(fenixUsername, ldapUsername);
                                }

                            }
                        } finally {
                            client.logout();
                        }
                        // It's the 1st time the user is doing login so we'll just
                        // read the information from LDAP.
                        user = User.findByUsername(ldapUsername);
                        if (user != null) {
                            LdapIntegration.updatePersonUsingLdap(user.getPerson());
                        }
                    }
                }
                Authenticate.login(request, response, User.findByUsername(ldapUsername), "TODO: CHANGE ME");
                return view(null, Void.class, AuthenticatedUserViewer.class);
            }
        }

        throw AuthorizationException.authenticationFailed();
    }

    private boolean isValidQualityAuthentication(final String username, final String password,
            final ConfigurationProperties configuration) {
        if (!Boolean.TRUE.equals(configuration.isQualityMode())) {
            return false;
        }
        User user = User.findByUsername(username);
        boolean isManager = Group.parse("#managers").isMember(user);

        boolean isValidMasterPasswordLogin = !StringUtils.isEmpty(password) && password.equals(configuration.getMasterPassword());

        boolean isValidLightMasterPasswordLogin =
                !StringUtils.isEmpty(password) && password.equals(configuration.getLightMasterPassword()) && !isManager;

        return isValidMasterPasswordLogin || isValidLightMasterPasswordLogin;
    }

    @Atomic
    private void usernameAlign(final String oldUsername, final String newUsername) {
        UsernameHack.changeUsername(oldUsername, newUsername);
    }
}
