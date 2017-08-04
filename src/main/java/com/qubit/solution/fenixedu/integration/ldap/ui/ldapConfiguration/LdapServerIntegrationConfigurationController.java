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
package com.qubit.solution.fenixedu.integration.ldap.ui.ldapConfiguration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.exceptions.DomainException;
import org.fenixedu.bennu.spring.portal.SpringFunctionality;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.qubit.solution.fenixedu.integration.ldap.domain.configuration.LdapServerIntegrationConfiguration;
import com.qubit.solution.fenixedu.integration.ldap.ui.LdapBaseController;
import com.qubit.solution.fenixedu.integration.ldap.ui.LdapController;
import com.qubit.terra.ldapclient.LdapClient;

import pt.ist.fenixframework.Atomic;

@SpringFunctionality(app = LdapController.class, title = "label.title.ldapConfiguration", accessGroup = "#managers")
@RequestMapping("/ldap/ldapconfiguration/ldapserverintegrationconfiguration")
public class LdapServerIntegrationConfigurationController extends LdapBaseController {

    @RequestMapping
    public String home(Model model) {
        return "forward:/ldap/ldapconfiguration/ldapserverintegrationconfiguration/";
    }

    private LdapServerIntegrationConfiguration getLdapServerIntegrationConfiguration(Model m) {
        return (LdapServerIntegrationConfiguration) m.asMap().get("ldapServerIntegrationConfiguration");
    }

    private void setLdapServerIntegrationConfiguration(LdapServerIntegrationConfiguration ldapServerIntegrationConfiguration,
            Model m) {
        m.addAttribute("ldapServerIntegrationConfiguration", ldapServerIntegrationConfiguration);
    }

    @Atomic
    public void deleteLdapServerIntegrationConfiguration(
            Collection<LdapServerIntegrationConfiguration> ldapServerIntegrationConfigurations) {
        for (LdapServerIntegrationConfiguration ldapServerIntegrationConfiguration : ldapServerIntegrationConfigurations) {
            ldapServerIntegrationConfiguration.delete();
        }
    }

    @RequestMapping(value = "/")
    public String search(@RequestParam(value = "serverid", required = false) java.lang.String serverID,
            @RequestParam(value = "basedomain", required = false) java.lang.String baseDomain, Model model) {
        List<LdapServerIntegrationConfiguration> searchldapserverintegrationconfigurationResultsDataSet =
                filterSearchLdapServerIntegrationConfiguration(serverID, baseDomain);

        model.addAttribute("searchldapserverintegrationconfigurationResultsDataSet",
                searchldapserverintegrationconfigurationResultsDataSet);
        return "ldap/ldapconfiguration/ldapserverintegrationconfiguration/search";
    }

    private List<LdapServerIntegrationConfiguration> getSearchUniverseSearchLdapServerIntegrationConfigurationDataSet() {
        return new ArrayList<LdapServerIntegrationConfiguration>(Bennu.getInstance().getLdapServerIntegrationConfigurationsSet());
    }

    private List<LdapServerIntegrationConfiguration> filterSearchLdapServerIntegrationConfiguration(java.lang.String serverID,
            java.lang.String baseDomain) {

        return getSearchUniverseSearchLdapServerIntegrationConfigurationDataSet().stream()
                .filter(ldapServerIntegrationConfiguration -> serverID == null || serverID.length() == 0
                        || (ldapServerIntegrationConfiguration.getServerID() != null
                                && ldapServerIntegrationConfiguration.getServerID().length() > 0
                                && ldapServerIntegrationConfiguration.getServerID().toLowerCase()
                                        .contains(serverID.toLowerCase())))
                .filter(ldapServerIntegrationConfiguration -> baseDomain == null || baseDomain.length() == 0
                        || (ldapServerIntegrationConfiguration.getBaseDomain() != null
                                && ldapServerIntegrationConfiguration.getBaseDomain().length() > 0
                                && ldapServerIntegrationConfiguration.getBaseDomain().toLowerCase()
                                        .contains(baseDomain.toLowerCase())))
                .collect(Collectors.toList());
    }

    @RequestMapping(value = "/search/disableDefault", method = RequestMethod.POST)
    public String processSearchToDisableDefault(Model model) {
        resetDefault();
        return "redirect:/ldap/ldapconfiguration/ldapserverintegrationconfiguration/";
    }

    @Atomic
    private void resetDefault() {
        Bennu.getInstance().setDefaultLdapServerIntegrationConfiguration(null);
    }

    @RequestMapping(value = "/search/deleteSelected", method = RequestMethod.POST)
    public String processSearchToDeleteSelected(
            @RequestParam("ldapServerIntegrationConfigurations") List<LdapServerIntegrationConfiguration> ldapServerIntegrationConfigurations,
            Model model) {

        deleteLdapServerIntegrationConfiguration(ldapServerIntegrationConfigurations);
        return "redirect:/ldap/ldapconfiguration/ldapserverintegrationconfiguration/";
    }

    @RequestMapping(value = "/search/view/{oid}")
    public String processSearchToViewAction(
            @PathVariable("oid") LdapServerIntegrationConfiguration ldapServerIntegrationConfiguration, Model model) {

        return "redirect:/ldap/ldapconfiguration/ldapserverintegrationconfiguration/read" + "/"
                + ldapServerIntegrationConfiguration.getExternalId();
    }

    @RequestMapping(value = "/search/setdefault/{oid}", method = RequestMethod.POST)
    public String processSearchToSetDefaultAction(
            @PathVariable("oid") LdapServerIntegrationConfiguration ldapServerIntegrationConfiguration, Model model) {

        ldapServerIntegrationConfiguration.configureAsDefault();
        return search(null, null, model);
    }

    @RequestMapping(value = "/search/testconnection/{oid}", method = RequestMethod.POST)
    public String processSearchToTestConnectionAction(
            @PathVariable("oid") LdapServerIntegrationConfiguration ldapServerIntegrationConfiguration, Model model) {

        LdapClient client = ldapServerIntegrationConfiguration.getClient();
        boolean login = client.login();
        if (login) {
            addInfoMessage("Logged in successful to server: " + ldapServerIntegrationConfiguration.getServerID(), model);
            client.logout();
        } else {
            addErrorMessage("Unable to log to server" + ldapServerIntegrationConfiguration.getServerID()
                    + ", check error log for more information", model);
        }
        return search(null, null, model);
    }

    @RequestMapping(value = "/create", method = RequestMethod.GET)
    public String create(Model model) {
        return "ldap/ldapconfiguration/ldapserverintegrationconfiguration/create";
    }

    @RequestMapping(value = "/create", method = RequestMethod.POST)
    public String create(@RequestParam(value = "serverid", required = false) java.lang.String serverID,
            @RequestParam(value = "username", required = false) java.lang.String username,
            @RequestParam(value = "password", required = false) java.lang.String password,
            @RequestParam(value = "passwordConfirmation", required = false) java.lang.String passwordConfirmation,
            @RequestParam(value = "url", required = false) java.lang.String url,
            @RequestParam(value = "basedomain", required = false) java.lang.String baseDomain,
            @RequestParam(value = "numberOfWorkers", required = false) java.lang.Integer numberOfWorkers, Model model) {

        if (!password.equals(passwordConfirmation)) {
            addErrorMessage("Password and password confirmation do not match", model);
            return create(model);
        }

        LdapServerIntegrationConfiguration ldapServerIntegrationConfiguration = null;

        try {
            ldapServerIntegrationConfiguration =
                    createLdapServerIntegrationConfiguration(serverID, username, password, url, baseDomain, numberOfWorkers);
        } catch (DomainException e) {
            addErrorMessage(e.getMessage(), model);
            return create(model);
        }
        model.addAttribute("ldapServerIntegrationConfiguration", ldapServerIntegrationConfiguration);

        return "redirect:/ldap/ldapconfiguration/ldapserverintegrationconfiguration/read/"
                + getLdapServerIntegrationConfiguration(model).getExternalId();
    }

    @Atomic
    public LdapServerIntegrationConfiguration createLdapServerIntegrationConfiguration(java.lang.String serverID,
            java.lang.String username, java.lang.String password, java.lang.String url, java.lang.String baseDomain,
            Integer numberOfWorkers) {
        LdapServerIntegrationConfiguration ldapServerIntegrationConfiguration =
                new LdapServerIntegrationConfiguration(serverID, username, password, url, baseDomain);
        ldapServerIntegrationConfiguration.setNumberOfWorkers(numberOfWorkers);
        return ldapServerIntegrationConfiguration;
    }

    @RequestMapping(value = "/read/{oid}")
    public String read(@PathVariable("oid") LdapServerIntegrationConfiguration ldapServerIntegrationConfiguration, Model model) {
        setLdapServerIntegrationConfiguration(ldapServerIntegrationConfiguration, model);
        return "ldap/ldapconfiguration/ldapserverintegrationconfiguration/read";
    }

    @RequestMapping(value = "/update/{oid}", method = RequestMethod.GET)
    public String update(@PathVariable("oid") LdapServerIntegrationConfiguration ldapServerIntegrationConfiguration,
            Model model) {
        setLdapServerIntegrationConfiguration(ldapServerIntegrationConfiguration, model);
        return "ldap/ldapconfiguration/ldapserverintegrationconfiguration/update";
    }

    @RequestMapping(value = "/update/{oid}", method = RequestMethod.POST)
    public String update(@PathVariable("oid") LdapServerIntegrationConfiguration ldapServerIntegrationConfiguration,
            @RequestParam(value = "serverid", required = false) java.lang.String serverID,
            @RequestParam(value = "username", required = false) java.lang.String username,
            @RequestParam(value = "password", required = false) java.lang.String password,
            @RequestParam(value = "passwordConfirmation", required = false) java.lang.String passwordConfirmation,
            @RequestParam(value = "url", required = false) java.lang.String url,
            @RequestParam(value = "basedomain", required = false) java.lang.String baseDomain,
            @RequestParam(value = "numberOfWorkers", required = false) java.lang.Integer numberOfWorkers, Model model) {

        if (!password.equals(passwordConfirmation)) {
            addErrorMessage("Password and password confirmation do not match", model);
            return update(ldapServerIntegrationConfiguration, model);
        }

        setLdapServerIntegrationConfiguration(ldapServerIntegrationConfiguration, model);
        updateLdapServerIntegrationConfiguration(serverID, username, password, url, baseDomain, numberOfWorkers, model);

        return "redirect:/ldap/ldapconfiguration/ldapserverintegrationconfiguration/read/"
                + getLdapServerIntegrationConfiguration(model).getExternalId();

    }

    @Atomic
    public void updateLdapServerIntegrationConfiguration(java.lang.String serverID, java.lang.String username,
            java.lang.String password, java.lang.String url, java.lang.String baseDomain, Integer numberOfWorkers, Model m) {
        getLdapServerIntegrationConfiguration(m).setServerID(serverID);
        getLdapServerIntegrationConfiguration(m).setUsername(username);
        getLdapServerIntegrationConfiguration(m).setPassword(password);
        getLdapServerIntegrationConfiguration(m).setUrl(url);
        getLdapServerIntegrationConfiguration(m).setBaseDomain(baseDomain);
        getLdapServerIntegrationConfiguration(m).setNumberOfWorkers(numberOfWorkers);
    }
}
