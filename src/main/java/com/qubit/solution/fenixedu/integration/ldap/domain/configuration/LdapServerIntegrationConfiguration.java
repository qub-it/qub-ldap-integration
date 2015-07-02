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
package com.qubit.solution.fenixedu.integration.ldap.domain.configuration;

import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.bennu.core.domain.Bennu;

import pt.ist.fenixframework.Atomic;

import com.qubit.terra.ldapclient.LdapClient;

public class LdapServerIntegrationConfiguration extends LdapServerIntegrationConfiguration_Base {

    public LdapServerIntegrationConfiguration(String serverID, String username, String password, String url, String baseDomain) {
        super();
        if (readByServerID(serverID) != null) {
            throw new DomainException("label.error.serverID.mustBeUnique", null);
        }
        setServerID(serverID);
        setUsername(username);
        setPassword(password);
        setUrl(url);
        setBaseDomain(baseDomain);
        boolean setHasDefault = Bennu.getInstance().getLdapServerIntegrationConfigurationsSet().isEmpty();
        setRootDomainObject(Bennu.getInstance());
        if (setHasDefault) {
            configureAsDefault();
        }

    }

    public void delete() {
        if (this == Bennu.getInstance().getDefaultLdapServerIntegrationConfiguration()) {
            Bennu.getInstance().setDefaultLdapServerIntegrationConfiguration(null);
        }
        setRootDomainObject(null);

        super.deleteDomainObject();
    }

    public static LdapServerIntegrationConfiguration readByServerID(String serverID) {
        return Bennu.getInstance().getLdapServerIntegrationConfigurationsSet().stream()
                .filter(configuration -> serverID != null && serverID.equals(configuration.getServerID())).findFirst()
                .orElse(null);
    }

    @Atomic
    public void configureAsDefault() {
        Bennu.getInstance().setDefaultLdapServerIntegrationConfiguration(this);
    }

    public boolean isDefaultConfiguration() {
        return Bennu.getInstance().getDefaultLdapServerIntegrationConfiguration() == this;
    }

    public LdapClient getClient() {
        return new LdapClient(getUsername(), getPassword(), getUrl(), getBaseDomain());
    }
}
