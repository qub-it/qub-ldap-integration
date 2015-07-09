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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.fenixedu.academic.domain.Country;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.contacts.EmailAddress;
import org.fenixedu.academic.domain.contacts.PartyContact;
import org.fenixedu.academic.domain.contacts.PartyContactType;
import org.fenixedu.academic.domain.contacts.PhysicalAddress;
import org.fenixedu.academic.domain.person.Gender;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.domain.UsernameHack;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.YearMonthDay;
import org.springframework.format.datetime.joda.DateTimeFormatterFactory;

import pt.ist.fenixframework.Atomic;

import com.google.common.io.BaseEncoding;
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
    private static final String UL_BI_ATTRIBUTE = "ULBI";
    private static final String CO_ATTRIBUTE = "co";
    private static final String UL_SEX_ATTRIBUTE = "ULSex";
    private static final String LAST_NAME_ATTRIBUTE = "sn";
    private static final String GIVEN_NAME_ATTRIBUTE = "givenName";
    private static final String FULL_NAME_ATTRIBUTE = "FullName";

    // From ULFenixUser class
    private static final String UL_FENIXUSER = "ULFenixUser";

    private static final String USER_PASSWORD = "userPassword";

    private static final String UL_STUDENT_CODE = "ULStudentCode";
    private static final String UL_COURSES = "ULCourses";

    private static String[] OBJECT_CLASSES_TO_ADD = { "person", "ULAuxUser", "ULFenixUser" };
    private static String[] OBJECT_CLASSES_TO_ADD_TEMP_USER = { "person", "ULFenixUser" };

    private static String getSchoolCode() {
        return "FMV"; // Bennu.getInstance().getInstitutionUnit().getAcronym();
    }

    private static String getObjectCommonName(String username, LdapServerIntegrationConfiguration configuration) {
        return "cn=" + username + "," + configuration.getBaseDomain();

    }

    private static String getPersonCommonName(Person person, LdapServerIntegrationConfiguration configuration) {
        return getObjectCommonName(person.getUsername(), configuration);
    }

    public static String generateLdapPassword(String password, String salt) {
        byte[] hashedString = null;
        try {
            hashedString = java.security.MessageDigest.getInstance("SHA-1").digest((password + salt).getBytes());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Problems accessing SHA-1 algorithm", e);
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            outputStream.write(hashedString);
            outputStream.write(salt.getBytes());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        byte finalArray[] = outputStream.toByteArray();
        return "{SSHA}" + BaseEncoding.base64().encode(finalArray).trim();
    }

    private static AttributesMap collectAttributeMap(Person person) {
        String schooldCode = getSchoolCode();

        AttributesMap attributesMap = new AttributesMap();

        boolean isStudent = isStudent(person);
        boolean isTeacher = person.getTeacher() != null && person.getTeacher().isActiveContractedTeacher();
        boolean isEmployee = false; // NOT YET IMPLEMENTED;

        // REQUIRED 
        attributesMap.add(UL_STUDENT_ACTIVE_ATTRIBUTE + schooldCode, String.valueOf(isStudent).toUpperCase());
        attributesMap.add(UL_TEACHER_ACTIVE_ATTRIBUTE + schooldCode, String.valueOf(isTeacher).toUpperCase());
        attributesMap.add(UL_EMPLOYEE_ACTIVE_ATTRIBUTE + schooldCode, String.valueOf(isEmployee).toUpperCase());
        attributesMap.add(FULL_NAME_ATTRIBUTE, person.getName());
        attributesMap.add(GIVEN_NAME_ATTRIBUTE, person.getProfile().getGivenNames());
        attributesMap.add(LAST_NAME_ATTRIBUTE, person.getProfile().getFamilyNames());
        attributesMap.add(UL_BI_ATTRIBUTE, person.getDocumentIdNumber());
        YearMonthDay dateOfBirthYearMonthDay = person.getDateOfBirthYearMonthDay();
        if (dateOfBirthYearMonthDay != null) {
            attributesMap.add(UL_BIRTH_DATE_ATTRIBUTE, dateOfBirthYearMonthDay.toString("yyyyMMdd") + "000000Z");
        } else {
            attributesMap.add(UL_BIRTH_DATE_ATTRIBUTE, new DateTime().toString("yyyyMMddHHmmss.SSS") + "Z");
        }

        // OPTIONAL
        attributesMap.add(UL_EXTERNAL_EMAIL_ADDR_ATTRIBUTE, person.getInstitutionalEmailAddressValue());
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

    private static boolean isStudent(Person person) {
        boolean isStudent = person.getStudent() != null && !person.getStudent().getActiveRegistrations().isEmpty();
        return isStudent;
    }

    public static LdapServerIntegrationConfiguration getDefaultConfiguration() {
        LdapServerIntegrationConfiguration defaultLdapServerIntegrationConfiguration =
                Bennu.getInstance().getDefaultLdapServerIntegrationConfiguration();
        if (defaultLdapServerIntegrationConfiguration == null) {
            throw new IllegalStateException("No ldap configuration is set has default!");
        }
        return defaultLdapServerIntegrationConfiguration;
    }

    public static boolean deleteUser(User user) {
        return deleteUser(user.getUsername());
    }

    public static boolean deleteUser(String username) {
        return deleteUser(username, getDefaultConfiguration());
    }

    public static boolean deleteUser(String username, String password, LdapServerIntegrationConfiguration configuration) {
        AttributesMap attributesMap = new AttributesMap();
        attributesMap.add(UL_FENIXUSER, username);
        attributesMap.add(USER_PASSWORD, password);

        boolean ableToSend = false;
        LdapClient client = configuration.getClient();
        try {
            if (client.login()) {
                client.writeNewContext(getObjectCommonName(username, configuration),
                        Arrays.asList(OBJECT_CLASSES_TO_ADD_TEMP_USER), attributesMap);
                ableToSend = true;
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return ableToSend;
    }

    public static boolean createUser(String username, String password, String salt) {
//        String salt = RandomStringUtils.randomAscii(16);
        String generateLdapPassword = generateLdapPassword(password, salt);
        return createUser(username, generateLdapPassword, getDefaultConfiguration());
    }

    // Map with each property and a string array
    // position 0 local, position 1 ldap info 
    //
    public static Map<String, String[]> retrieveSyncInformation(Person person) {
        return retrieveSyncInformation(person, getDefaultConfiguration());
    }

    public static Map<String, String[]> retrieveSyncInformation(Person person,
            LdapServerIntegrationConfiguration defaultConfiguration) {
        AttributesMap collectAttributeMap = collectAttributeMap(person);
        Map<String, String[]> map = new LinkedHashMap<String, String[]>();

        LdapClient client = defaultConfiguration.getClient();
        String[] fields =
                new String[] { FULL_NAME_ATTRIBUTE, GIVEN_NAME_ATTRIBUTE, LAST_NAME_ATTRIBUTE, UL_BI_ATTRIBUTE, CO_ATTRIBUTE,
                        UL_SEX_ATTRIBUTE, UL_BIRTH_DATE_ATTRIBUTE, UL_POSTAL_ADDR_ATTRIBUTE, UL_POSTAL_CODE_ATTRIBUTE,
                        UL_INTERNAL_EMAIL_ADDR_ATTRIBUTE + getSchoolCode(), UL_EXTERNAL_EMAIL_ADDR_ATTRIBUTE,
                        UL_STUDENT_ACTIVE_ATTRIBUTE + getSchoolCode(), UL_FENIXUSER };

        for (String field : fields) {
            String[] values = new String[2];
            List<String> list = collectAttributeMap.get(field);
            StringBuilder builder = concatenateValues(list);
            values[0] = builder.toString();
            map.put(field, values);
        }

        if (client.login()) {
            try {
                QueryReply query = client.query("cn=" + person.getUsername(), fields);
                if (query.getNumberOfResults() == 1) {
                    QueryReplyElement queryReplyElement = query.getResults().get(0);
                    for (String field : fields) {
                        String[] values = map.get(field);
                        List<String> list = queryReplyElement.getListAttribute(field);
                        StringBuilder builder = concatenateValues(list);
                        values[1] = builder.toString();
                    }
                }

            } finally {
                client.logout();
            }
        }

        return map;
    }

    private static StringBuilder concatenateValues(List<String> list) {
        StringBuilder builder = new StringBuilder();
        if (list != null && !list.isEmpty()) {
            for (String element : list) {
                if (builder.length() > 0) {
                    builder.append(" ; ");
                }
                builder.append(element);
            }
        }
        return builder;
    }

    public static boolean createUser(String username, String password, LdapServerIntegrationConfiguration configuration) {
        AttributesMap attributesMap = new AttributesMap();
        attributesMap.add(LAST_NAME_ATTRIBUTE, username);
        attributesMap.add(UL_FENIXUSER, username);
        attributesMap.add(USER_PASSWORD, password);

        boolean ableToSend = false;
        LdapClient client = configuration.getClient();
        try {
            if (client.login()) {
                client.writeNewContext(getObjectCommonName(username, configuration),
                        Arrays.asList(OBJECT_CLASSES_TO_ADD_TEMP_USER), attributesMap);
                ableToSend = true;
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return ableToSend;
    }

    public static boolean deleteUser(String username, LdapServerIntegrationConfiguration configuration) {
        boolean ableToSend = false;
        LdapClient client = configuration.getClient();
        try {
            client.deleteContext(getObjectCommonName(username, configuration));
            ableToSend = true;
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return ableToSend;
    }

    public static boolean isUserAvailableInLdap(User user) {
        return isUserAvailableInLdap(user, getDefaultConfiguration());
    }

    public static boolean isPersonAvailableInLdap(Person person) {
        return isUserAvailableInLdap(person.getUser(), getDefaultConfiguration());
    }

    private static boolean isUserAvailableInLdap(User user, LdapServerIntegrationConfiguration defaultConfiguration) {
        LdapClient client = defaultConfiguration.getClient();
        boolean isAvailable = false;
        try {
            if (client.login()) {
                try {
                    QueryReply query = client.query("cn=" + user.getUsername(), new String[] { UL_FENIXUSER });
                    if (query.getNumberOfResults() == 1) {
                        isAvailable = true;
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        } finally {
            client.logout();
        }

        return isAvailable;
    }

    public static boolean updateStudentStatus(Student student) {
        return updateStudentStatus(student, getDefaultConfiguration());
    }

    public static boolean updateStudentStatus(Student student, LdapServerIntegrationConfiguration configuration) {
        Person person = student.getPerson();

        boolean ableToUpdateStudent = false;
        if (!isPersonAvailableInLdap(person)) {
            createPersonInLdap(person);
        }

        LdapClient client = configuration.getClient();
        if (client.login()) {
            try {
                AttributesMap attributesMap = new AttributesMap();
                List<String> objectClasses = new ArrayList<String>();

                if (isStudent(person)) {
                    attributesMap.add(UL_STUDENT_ACTIVE_ATTRIBUTE + getSchoolCode(), "TRUE");
                    attributesMap.add(UL_STUDENT_CODE + getSchoolCode(), String.valueOf(student.getNumber()));
                    List<String> courses = new ArrayList<String>();
                    for (Registration registration : student.getActiveRegistrations()) {
                        courses.add(registration.getDegreeName());
                    }
                    attributesMap.add(UL_COURSES + getSchoolCode(), courses.toArray(new String[] {}));
                    objectClasses.add("ULAuxFac" + getSchoolCode());
                } else {
                    attributesMap.add(UL_STUDENT_ACTIVE_ATTRIBUTE + getSchoolCode(), "FALSE");
                }

                client.addToExistingContext(getPersonCommonName(person, configuration), objectClasses, attributesMap);
                ableToUpdateStudent = true;
            } finally {
                client.logout();
            }
        }

        return ableToUpdateStudent;

    }

    public static boolean createPersonInLdap(Person person) {
        return createPersonInLdap(person, getDefaultConfiguration());
    }

    public static boolean createPersonInLdap(Person person, LdapServerIntegrationConfiguration configuration) {
        boolean ableToSend = false;
        LdapClient client = configuration.getClient();
        boolean login = client.login();
        try {
            if (login) {

                List<String> objectClasses = Arrays.asList(OBJECT_CLASSES_TO_ADD);

                try {
                    client.writeNewContext(getPersonCommonName(person, configuration), objectClasses, collectAttributeMap(person));
                    ableToSend = true;
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        } finally {
            client.logout();
        }
        return ableToSend;
    }

    public static boolean updatePersonInLdap(Person person) {
        return updatePersonInLdap(person, getDefaultConfiguration());
    }

    public static boolean updatePersonInLdap(Person person, LdapServerIntegrationConfiguration configuration) {
        if (!isUserAvailableInLdap(person.getUser(), configuration)) {
            return createPersonInLdap(person, configuration);
        } else {
            boolean ableToSend = false;
            LdapClient client = configuration.getClient();
            boolean login = client.login();
            try {
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
            } finally {
                client.logout();
            }
            return ableToSend;
        }
    }

    public static boolean updatePersonUsingLdap(Person person) {
        return updatePersonUsingLdap(person, getDefaultConfiguration());
    }

    public static boolean updatePersonUsingLdap(Person person, LdapServerIntegrationConfiguration configuration) {

        String schoolCode = getSchoolCode();
        LdapClient client = configuration.getClient();
        boolean ableToSync = false;

        try {
            if (client.login()) {
                try {
                    QueryReply query =
                            client.query("cn=" + person.getUsername(), new String[] {
                                    UL_INTERNAL_EMAIL_ADDR_ATTRIBUTE + schoolCode, UL_EXTERNAL_EMAIL_ADDR_ATTRIBUTE,
                                    UL_BIRTH_DATE_ATTRIBUTE, UL_BI_ATTRIBUTE, UL_SEX_ATTRIBUTE, GIVEN_NAME_ATTRIBUTE,
                                    LAST_NAME_ATTRIBUTE });
                    if (query.getNumberOfResults() == 1) {
                        QueryReplyElement queryReplyElement = query.getResults().get(0);
                        String institutionalEmail =
                                queryReplyElement.getSimpleAttribute(UL_INTERNAL_EMAIL_ADDR_ATTRIBUTE + schoolCode);
                        String personalEmail = queryReplyElement.getSimpleAttribute(UL_EXTERNAL_EMAIL_ADDR_ATTRIBUTE);
                        String birthDate = queryReplyElement.getSimpleAttribute(UL_BIRTH_DATE_ATTRIBUTE);
                        String documentID = queryReplyElement.getSimpleAttribute(UL_BI_ATTRIBUTE);
                        String gender = queryReplyElement.getSimpleAttribute(UL_SEX_ATTRIBUTE);
                        String givenNames = queryReplyElement.getSimpleAttribute(GIVEN_NAME_ATTRIBUTE);
                        String surnames = queryReplyElement.getSimpleAttribute(LAST_NAME_ATTRIBUTE);
                        updatePerson(person, institutionalEmail, personalEmail, birthDate, documentID, gender, givenNames,
                                surnames);
                    }
                    ableToSync = true;
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        } finally {
            client.logout();
        }
        return ableToSync;
    }

    @Atomic
    private static void updatePerson(Person person, String instituionalEmail, String personalEmail, String birthDate,
            String documentID, String sex, String givenNames, String surnames) {

        String institutionalEmailAddressValue = person.getInstitutionalEmailAddressValue();
        if (!StringUtils.isEmpty(instituionalEmail)
                && (institutionalEmailAddressValue == null || !institutionalEmailAddressValue.equals(instituionalEmail))) {
            person.setInstitutionalEmailAddressValue(instituionalEmail);
        }

        List<? extends PartyContact> personalEmails = person.getPartyContacts(EmailAddress.class, PartyContactType.PERSONAL);
        if (!StringUtils.isEmpty(personalEmail)
                && personalEmails.stream().filter(email -> email.getPresentationValue().equals(personalEmail)).count() == 0) {
            EmailAddress.createEmailAddress(person, personalEmail, PartyContactType.PERSONAL, false);
        }

        if (!StringUtils.isEmpty(birthDate)) {
            String format = "yyyyMMddHHmmss'Z'";
            if (birthDate.contains(".")) {
                format = "yyyyMMddHHmmss.SSS'Z'";
            }
            LocalDate parseLocalDate = new DateTimeFormatterFactory(format).createDateTimeFormatter().parseLocalDate(birthDate);
            if (!parseLocalDate.isEqual(person.getDateOfBirthYearMonthDay())) {
                YearMonthDay yearMonthDay =
                        new YearMonthDay(parseLocalDate.getYear(), parseLocalDate.getMonthOfYear(),
                                parseLocalDate.getDayOfMonth());
                person.setDateOfBirthYearMonthDay(yearMonthDay);
            }
        }

        if (!StringUtils.isEmpty(documentID) && !person.getDocumentIdNumber().equals(documentID)) {
            person.setDocumentIdNumber(documentID);
        }

        if (!StringUtils.isEmpty(sex)) {
            Gender genderInLdap = "M".equals(sex) ? Gender.MALE : "F".equals(sex) ? Gender.FEMALE : null;
            if (genderInLdap != null && person.getGender() != genderInLdap) {
                person.setGender(genderInLdap);
            }
        }

        if (!StringUtils.isEmpty(givenNames) && !StringUtils.isEmpty(surnames)
                && !person.getPartyName().equalInAnyLanguage(givenNames + " " + surnames)) {
            String displayName = givenNames.split(" ")[0] + " " + surnames.split(" ")[0];
            person.getProfile().changeName(givenNames, surnames, displayName);
        }

    }

}
