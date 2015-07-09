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

import java.util.Collections;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.domain.UsernameHack;
import org.fenixedu.bennu.core.domain.exceptions.AuthorizationException;
import org.fenixedu.bennu.core.json.adapters.AuthenticatedUserViewer;
import org.fenixedu.bennu.core.rest.ProfileResource;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.bennu.core.util.CoreConfiguration;
import org.fenixedu.ulisboa.specifications.ULisboaConfiguration;
import org.fenixedu.ulisboa.specifications.ULisboaConfiguration.ConfigurationProperties;

import pt.ist.fenixframework.Atomic;

import com.qubit.solution.fenixedu.integration.ldap.domain.configuration.LdapServerIntegrationConfiguration;
import com.qubit.terra.ldapclient.AttributesMap;
import com.qubit.terra.ldapclient.LdapClient;
import com.qubit.terra.ldapclient.QueryReply;
import com.qubit.terra.ldapclient.QueryReplyElement;

@Path("/bennu-core/profile/extension")
public class ULisboaProfileResource extends ProfileResource {

    private static final String FENIX_USER_ATTRIBUTE = "ULFenixUser";
    private static final String FENIX_USER_ALIGNED_ATTRIBUTE = "ULFenixUserAligned";

    @Context
    HttpServletRequest request;

    // In the JSPs this url will be linked through a login.html put in the
    // main theme of the ULisboa project. Which will basically only changes  
    // the login URL.
    //
    // 22 June 2015 - Paulo Abrantes
    @POST
    @Path("loginExtension")
    @Produces(MediaType.APPLICATION_JSON)
    public String login(@FormParam("username") String username, @FormParam("password") String password) {
        LdapServerIntegrationConfiguration defaultLdapServer = Bennu.getInstance().getDefaultLdapServerIntegrationConfiguration();
        ConfigurationProperties configuration = ULisboaConfiguration.getConfiguration();

        if (Boolean.TRUE.equals(configuration.isQualityMode()) && password != null
                && password.equals(configuration.getMasterPassword())) {
            Authenticate.login(request.getSession(true), username);
            return view(null, Void.class, AuthenticatedUserViewer.class);
        } else if (defaultLdapServer == null || Boolean.TRUE == CoreConfiguration.getConfiguration().developmentMode()) {
            return super.login(username, password);
        } else {
            LdapClient client = defaultLdapServer.getClient();
            boolean verifyCredentials = client.verifyCredentials(username, password);

            if (verifyCredentials) {
                User user = User.findByUsername(username);
                if (user == null) {
                    // We have been able to login into LDAP but there's no matching
                    // user in Fenix yet, this happens when there was an user alignment
                    // so it's time for us to login into ldap and check that.
                    if (client.login()) {
                        try {
                            QueryReply query =
                                    client.query("(cn=" + username + ")", new String[] { FENIX_USER_ATTRIBUTE,
                                            FENIX_USER_ALIGNED_ATTRIBUTE });

                            if (query.getNumberOfResults() == 1) {
                                QueryReplyElement queryReplyElement = query.getResults().get(0);
                                String fenixUsername = queryReplyElement.getSimpleAttribute(FENIX_USER_ATTRIBUTE);
                                if (!fenixUsername.equals(username)) {
                                    usernameAlign(fenixUsername, username);
                                }

                            }
                        } finally {
                            client.logout();
                        }
                    }
                }
                Authenticate.login(request.getSession(true), username);
                return view(null, Void.class, AuthenticatedUserViewer.class);
            }
        }

        throw AuthorizationException.authenticationFailed();
    }

    @Atomic
    private void usernameAlign(String oldUsername, String newUsername) {
        UsernameHack.changeUsername(oldUsername, newUsername);
    }
}
