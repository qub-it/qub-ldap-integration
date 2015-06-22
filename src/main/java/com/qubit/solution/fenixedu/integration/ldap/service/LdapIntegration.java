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

import java.util.List;

import org.fenixedu.academic.domain.Country;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.contacts.PhysicalAddress;
import org.fenixedu.bennu.core.domain.Bennu;
import org.joda.time.DateTime;
import org.joda.time.YearMonthDay;

import com.qubit.solution.fenixedu.integration.ldap.domain.configuration.LdapServerIntegrationConfiguration;
import com.qubit.terra.ldapclient.AttributesMap;
import com.qubit.terra.ldapclient.LdapClient;
import com.qubit.terra.ldapclient.QueryReply;
import com.qubit.terra.ldapclient.QueryReplyElement;

import edu.emory.mathcs.backport.java.util.Arrays;

public class LdapIntegration {

    //
    // Evaluate effort to make this fields dynamic using a domain entity 
    // 
    // 20 April 2015 - Paulo Abrantes
    private static final String UL_STUDENT_ACTIVE_ATTRIBUTE = "ULStudentActive";
    private static final String UL_ROLE_ATTRIBUTE = "ULRole";
    private static final String UL_INTERNAL_EMAIL_ADDR_ATTRIBUTE = "ULInternalEmailAddr";
    private static final String UL_EXTERNAL_EMAIL_ADDR_ATTRIBUTE = "ULExternalEmailAddr";
    private static final String UL_POSTAL_CODE_ATTRIBUTE = "ULPostalCode";
    private static final String UL_POSTAL_ADDR_ATTRIBUTE = "ULPostalAddr";
    private static final String UL_BIRTH_DATE_ATTRIBUTE = "ULBirthDate";
    private static final String UL_TEACHER_ACTIVE_ATTRIBUTE = "ULTeacherActive";
    private static final String UL_EMPLOYEE_ACTIVE_ATTRIBUTE = "ULEmployeeActive";
    private static final String ULBI_ATTRIBUTE = "ULBI";
    private static final String CO_ATTRIBUTE = "co";
    private static final String UL_SEX_ATTRIBUTE = "ULSex";
    private static final String LAST_NAME_ATTRIBUTE = "sn";
    private static final String GIVEN_NAME_ATTRIBUTE = "givenName";
    private static final String FULL_NAME_ATTRIBUTE = "FullName";
    private static final String DESCRIPTION_ATTRIBUTE = "description";
    private static final String UL_FENIXUSER = "ULFenixUser";

    private static final String UL_USER_ID = "ULUserId";

    private static String[] OBJECT_CLASSES_TO_ADD = { "person", "ULAuxUser" };

    private static String getSchoolCode() {
        return "FMV"; // Bennu.getInstance().getInstitutionUnit().getAcronym();
    }

    private static String getPersonCommonName(Person person, LdapServerIntegrationConfiguration configuration) {
        return "cn=" + getSchoolCode() + "-" + person.getUsername() + ",ou=people," + configuration.getBaseDomain();
    }

    private static AttributesMap collectAttributeMap(Person person) {
        String schooldCode = getSchoolCode();

        AttributesMap attributesMap = new AttributesMap();

        boolean isStudent = person.getStudent() != null && !person.getStudent().getActiveRegistrations().isEmpty();
        boolean isTeacher = person.getTeacher() != null && person.getTeacher().isActiveContractedTeacher();
        boolean isEmployee = false; // NOT YET IMPLEMENTED;

        // REQUIRED 
        attributesMap.add(UL_STUDENT_ACTIVE_ATTRIBUTE + schooldCode, String.valueOf(isStudent).toUpperCase());
        attributesMap.add(UL_TEACHER_ACTIVE_ATTRIBUTE + schooldCode, String.valueOf(isTeacher).toUpperCase());
        attributesMap.add(UL_EMPLOYEE_ACTIVE_ATTRIBUTE + schooldCode, String.valueOf(isEmployee).toUpperCase());
        attributesMap.add(FULL_NAME_ATTRIBUTE, person.getName());
        String[] split = person.getName().split(" ");
        attributesMap.add(GIVEN_NAME_ATTRIBUTE, split[0]);
        attributesMap.add(LAST_NAME_ATTRIBUTE, split[split.length - 1]);
        attributesMap.add(ULBI_ATTRIBUTE, person.getDocumentIdNumber());
        YearMonthDay dateOfBirthYearMonthDay = person.getDateOfBirthYearMonthDay();
        if (dateOfBirthYearMonthDay != null) {
            attributesMap.add(UL_BIRTH_DATE_ATTRIBUTE, dateOfBirthYearMonthDay.toString("yyyyMMdd") + "000000.000Z");
        } else {
            attributesMap.add(UL_BIRTH_DATE_ATTRIBUTE, new DateTime().toString("yyyyMMddDDmmss.SSS") + "Z");
        }
        attributesMap.add(UL_EXTERNAL_EMAIL_ADDR_ATTRIBUTE, person.getInstitutionalEmailAddressValue());

        // OPTIONAL
        attributesMap.add(UL_SEX_ATTRIBUTE, person.getGender().toString().substring(0, 1));
        Country countryOfBirth = person.getCountryOfBirth();
        if (countryOfBirth != null) {
            attributesMap.add(CO_ATTRIBUTE, countryOfBirth.getName());
        }
        PhysicalAddress defaultPhysicalAddress = person.getDefaultPhysicalAddress();
        if (defaultPhysicalAddress != null) {
            attributesMap.add(UL_POSTAL_ADDR_ATTRIBUTE, defaultPhysicalAddress.getAddress());
            attributesMap.add(UL_POSTAL_CODE_ATTRIBUTE, defaultPhysicalAddress.getPostalCode());
        }
        attributesMap.add(UL_INTERNAL_EMAIL_ADDR_ATTRIBUTE + schooldCode, person.getInstitutionalEmailAddressValue());

        if (isStudent) {
            attributesMap.add(UL_ROLE_ATTRIBUTE, "STUDENT");
        }
        if (isTeacher) {
            attributesMap.add(UL_ROLE_ATTRIBUTE, "TEACHER");
        }
        if (isEmployee) {
            attributesMap.add(UL_ROLE_ATTRIBUTE, "EMPLOYEE");
        }

        attributesMap.add(UL_FENIXUSER, person.getUsername());
        return attributesMap;
    }

    public static LdapServerIntegrationConfiguration getDefaultConfiguration() {
        LdapServerIntegrationConfiguration defaultLdapServerIntegrationConfiguration =
                Bennu.getInstance().getDefaultLdapServerIntegrationConfiguration();
        if (defaultLdapServerIntegrationConfiguration == null) {
            throw new IllegalStateException("No ldap configuration is set has default!");
        }
        return defaultLdapServerIntegrationConfiguration;
    }

    public static boolean createPersonInLdap(Person person) {
        return createPersonInLdap(person, getDefaultConfiguration());
    }

    public static boolean createPersonInLdap(Person person, LdapServerIntegrationConfiguration configuration) {
        boolean ableToSend = false;
        LdapClient client =
                new LdapClient(configuration.getUsername(), configuration.getPassword(), configuration.getUrl(),
                        configuration.getBaseDomain());
        boolean login = client.login();
        if (login) {

            List<String> objectClasses = Arrays.asList(OBJECT_CLASSES_TO_ADD);

            try {
                client.writeNewContext(getPersonCommonName(person, configuration), objectClasses, collectAttributeMap(person));
                ableToSend = true;
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        return ableToSend;
    }

    public static String readULUserID(Person person) {
        return readULUserID(person, getDefaultConfiguration());
    }

    public static String readULUserID(Person person, LdapServerIntegrationConfiguration configuration) {
        String result = null;
        LdapClient client =
                new LdapClient(configuration.getUsername(), configuration.getPassword(), configuration.getUrl(),
                        configuration.getBaseDomain());
        boolean login = client.login();
        if (login) {

            QueryReply query =
                    client.query(
                            "(& (objectClass=ULAuxUser) (" + getPersonCommonName(person, configuration).split(",")[0] + "))",
                            new String[] { UL_USER_ID });

            if (query.getNumberOfResults() == 1) {
                QueryReplyElement queryReplyElement = query.getResults().get(0);
                result = queryReplyElement.getSimpleAttribute(UL_USER_ID);
            }
        }

        return result;
    }

    public static boolean updatePersonInLdap(Person person) {
        return updatePersonInLdap(person, getDefaultConfiguration());
    }

    public static boolean updatePersonInLdap(Person person, LdapServerIntegrationConfiguration configuration) {
        boolean ableToSend = false;
        LdapClient client =
                new LdapClient(configuration.getUsername(), configuration.getPassword(), configuration.getUrl(),
                        configuration.getBaseDomain());
        boolean login = client.login();
        if (login) {

            List<String> objectClasses = Arrays.asList(OBJECT_CLASSES_TO_ADD);

            try {
                client.replaceInExistingContext(getPersonCommonName(person, configuration), objectClasses,
                        collectAttributeMap(person));
                ableToSend = true;
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        return ableToSend;
    }
}
