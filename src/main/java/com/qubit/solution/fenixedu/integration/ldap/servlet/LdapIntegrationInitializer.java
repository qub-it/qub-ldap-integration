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
package com.qubit.solution.fenixedu.integration.ldap.servlet;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.ulisboa.specifications.domain.student.access.StudentAccessServices;

import com.qubit.solution.fenixedu.integration.ldap.service.LdapIntegration;
import com.qubit.solution.fenixedu.integration.ldap.service.integration.SyncPersonWithLdap;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;
import pt.ist.fenixframework.dml.DeletionListener;

@WebListener
public class LdapIntegrationInitializer implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent event) {
        FenixFramework.getDomainModel().registerDeletionListener(Person.class, new DeletionListener<Person>() {

            @Override
            public void deleting(Person person) {
                if (Bennu.getInstance().getDefaultLdapServerIntegrationConfiguration() != null) {
                    LdapIntegration.deleteUser(person);
                }
            }
        });
        StudentAccessServices.subscribeSyncPerson(new SyncPersonWithLdap());

        migrateLoginFlag();

    }

    @Atomic
    private void migrateLoginFlag() {
        Bennu.getInstance().getLdapServerIntegrationConfigurationsSet().stream().filter(s -> s.getAllowNonBennusToLogin() == null)
                .forEach(s -> s.setAllowNonBennusToLogin(true));
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
    }
}