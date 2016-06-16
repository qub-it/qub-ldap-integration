/**
 * This file was created by Quorum Born IT <http://www.qub-it.com/> and its 
 * copyright terms are bind to the legal agreement regulating the FenixEdu@ULisboa 
 * software development project between Quorum Born IT and Serviços Partilhados da
 * Universidade de Lisboa:
 *  - Copyright © 2016 Quorum Born IT (until any Go-Live phase)
 *  - Copyright © 2016 Universidade de Lisboa (after any Go-Live phase)
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
package com.qubit.solution.fenixedu.integration.ldap.service.task;

import java.util.List;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.groups.DynamicGroup;
import org.fenixedu.bennu.scheduler.annotation.Task;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

import com.qubit.solution.fenixedu.integration.ldap.domain.configuration.LdapServerIntegrationConfiguration;
import com.qubit.solution.fenixedu.integration.ldap.domain.configuration.LdapServerIntegrationConfiguration.AttributeWriter;
import com.qubit.solution.fenixedu.integration.ldap.service.AttributeResolver;

// Extract this to fcul module
@Task(englishTitle = "Marks externals users in Ldap for FC only")
public class SendFculExternalPersonAttribute extends CustomTask {

    public static class WriteExternalAttributes extends AttributeWriter {

        public WriteExternalAttributes() {
            super("ULExternalFC", new AttributeResolver() {

                @Override
                public String getAttributeValueFor(Person person) {
                    DynamicGroup dynamicGroup = DynamicGroup.get("external-users");
                    if (!dynamicGroup.isDefined()) {
                        dynamicGroup.toPersistentGroup();
                    }
                    return String.valueOf(dynamicGroup.isMember(person.getUser())).toUpperCase();
                }
            });
        }

    }

    @Override
    public void runTask() throws Exception {
        LdapServerIntegrationConfiguration defaultLdapServerIntegrationConfiguration =
                Bennu.getInstance().getDefaultLdapServerIntegrationConfiguration();
        List<Person> collect =
                Bennu.getInstance().getPartysSet().stream().filter(p -> p instanceof Person).map(Person.class::cast)
                        .collect(Collectors.toList());
        defaultLdapServerIntegrationConfiguration.applyOperationToAllUsers(collect, WriteExternalAttributes.class, 10, false);
    }

}
