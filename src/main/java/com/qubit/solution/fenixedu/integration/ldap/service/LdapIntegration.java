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
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.domain.Country;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.contacts.EmailAddress;
import org.fenixedu.academic.domain.contacts.PartyContact;
import org.fenixedu.academic.domain.contacts.PartyContactType;
import org.fenixedu.academic.domain.contacts.PhysicalAddress;
import org.fenixedu.academic.domain.person.Gender;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicInterval;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.groups.DynamicGroup;
import org.fenixedu.ulisboa.specifications.domain.idcards.CgdCard;
import org.fenixedu.ulisboa.specifications.service.StudentActive;
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
import edu.emory.mathcs.backport.java.util.Collections;

public class LdapIntegration {

    //
    // Evaluate effort to make this fields dynamic using a domain entity 
    // 
    // 20 April 2015 - Paulo Abrantes
    private static final String UL_STUDENT_ACTIVE_ATTRIBUTE = "ULStudentActive";
    private static final String UL_ROLE_ATTRIBUTE = "ULRole";
    private static final String UL_INTERNAL_EMAIL_ADDR_ATTRIBUTE = "ULInternalEmailAddr";
    private static final String UL_EXTERNAL_EMAIL_ADDR_ATTRIBUTE = "ULExternalEmailAddr";
    private static final String INTERNET_EMAIL_ADDRESS = "InternetEmailAddress";
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
    private static final String UL_MIFARE_ATTRIBUTE = "ULMifare";
    private static final String UL_ALUMNI_ATTRIBUTE = "ULAlumni";
    private static final String USER_PASSWORD = "userPassword";
    private static final String UL_STUDENT_CODE = "ULStudentCode";
    private static final String UL_COURSES = "ULCourses";
    private static final String COMMON_NAME = "cn";

    // Attribute from ULFenixUser class (yes same name..I know) 
    // This must old the original username of the user in Fenix
    // 
    // 30 July 2015 - Paulo ABrantes
    private static final String UL_FENIXUSER = "ULFenixUser";

    // Object classes from Ldap
    private static String[] OBJECT_CLASSES_TO_ADD = { "person", "ULAuxUser", "ULFenixUser" };
    private static String[] OBJECT_CLASSES_TO_ADD_TEMP_USER = { "person", "ULFenixUser" };
    private static final String STUDENT_CLASS_PREFIX = "ULAuxFac";

    // All the fields in this variable are from ULAuxUser LDAP class.
    //
    // This fields are used to show in the synchronized interface and also used by isUpdatedNeeded(Person) 
    //
    // To check if a given person is synchronized this fields are requested to ldap and we also call
    // collectAttributeMap with the given person. Then for each field here we iterate and check if both
    // values are equal or not. If all is equal then things are up to date, otherwise they are not.
    //
    // 30 July 2015 - Paulo Abrantes
    private static final String[] PERSON_FIELDS_TO_SYNC = new String[] { FULL_NAME_ATTRIBUTE, GIVEN_NAME_ATTRIBUTE,
            LAST_NAME_ATTRIBUTE, UL_BI_ATTRIBUTE, CO_ATTRIBUTE, UL_SEX_ATTRIBUTE, UL_BIRTH_DATE_ATTRIBUTE,
            UL_POSTAL_ADDR_ATTRIBUTE, UL_POSTAL_CODE_ATTRIBUTE, UL_EXTERNAL_EMAIL_ADDR_ATTRIBUTE,
            UL_INTERNAL_EMAIL_ADDR_ATTRIBUTE + getSchoolCode(), UL_STUDENT_ACTIVE_ATTRIBUTE + getSchoolCode(),
            UL_TEACHER_ACTIVE_ATTRIBUTE + getSchoolCode(), UL_EMPLOYEE_ACTIVE_ATTRIBUTE + getSchoolCode(),
            UL_ALUMNI_ATTRIBUTE + getSchoolCode(), UL_MIFARE_ATTRIBUTE + getSchoolCode() };

    // All the fields in this variable are from ULAuxFac<SchoolCode> ldap class.
    //
    // Same thing as the PERSON_FIELDS_INTERFACE but for the Student class
    //
    // 30 July 2015 - Paulo Abrantes
    private static final String[] STUDENT_FIELDS_TO_SYNC = new String[] { UL_STUDENT_CODE + getSchoolCode(),
            UL_COURSES + getSchoolCode() };

    private static String getSchoolCode() {
        return Bennu.getInstance().getInstitutionUnit().getAcronym();
    }

    // When we send a person to LDAP its CN is the person's username, which on the first creation
    // is equal to the attribute in ULFenixUser. IDM then proceeds to do a match and WILL REWRITE
    // CN to campus username.
    //
    // This means that if I create a user for me in the system called bennu123, when it reaches 
    // LDAP it will be CN=bennu123 ULFenixUser=bennu123. Since I have a campus ID, IDM will do a
    // match and rewrite the CN. At this moment the register in LDAP will be CN=paulo.abrantes
    // ULFenixUser=bennu123.
    //
    // If by that time no login has been done in Fenix my username in Fenix will still be bennu123
    // but my CN will no longer be bennu123 but paulo.abrantes. This method handles those situations
    // returning always the correct CN for any given username.
    //
    // Side note: When a user log's into Fenix the system detects the CN has change and changes
    // the username accordingly.
    //
    // 30 July 2015 - Paulo Abrantes
    private static String getCorrectCN(String username, LdapClient client) {
        String ldapUsername = username;
        String[] replyAttributes = new String[] { COMMON_NAME };

        QueryReply query = client.query(UL_FENIXUSER + "=" + username, replyAttributes);
        if (query.getNumberOfResults() == 1) {
            QueryReplyElement next = query.getResults().iterator().next();
            ldapUsername = next.getSimpleAttribute(COMMON_NAME);
        }
        return ldapUsername;
    }

    // When refering to an object in ldap we can't only give his CN but also the domain. Both the 
    // methods below 
    //
    private static String getObjectCommonName(String username, LdapClient client, LdapServerIntegrationConfiguration configuration) {
        return COMMON_NAME + "=" + getCorrectCN(username, client) + "," + configuration.getBaseDomain();
    }

    private static String getPersonCommonName(Person person, LdapClient client, LdapServerIntegrationConfiguration configuration) {
        return getObjectCommonName(person.getUsername(), client, configuration);
    }

    // This is an extraction of collectAttributeMap(Student student) because collectAttributeMap(Person person)
    // also calls this if a person has a student so every field is filled singleshot.
    //
    // 30 July 2015 - Paulo Abrantes
    private static void collecStudentAttributes(Student student, AttributesMap attributesMap) {
        attributesMap.add(UL_STUDENT_CODE + getSchoolCode(), String.valueOf(student.getNumber()));
        if (isStudent(student.getPerson())) {
            List<String> courses = new ArrayList<String>();
            for (Registration registration : student.getActiveRegistrations()) {
                courses.add(registration.getDegreeCurricularPlanName() + " " + registration.getDegreeName());
            }
            attributesMap.add(UL_COURSES + getSchoolCode(), courses.toArray(new String[] {}));
        }

    }

    // Collects data from a student, these are fields from ULAUXFac<School> ldap class
    private static AttributesMap collectAttributeMap(Student student) {
        AttributesMap attributesMap = new AttributesMap();
        collecStudentAttributes(student, attributesMap);
        return attributesMap;
    }

    // Collects data from a person, these are fields from ULAuxUser ldap class 
    private static AttributesMap collectAttributeMap(Person person) {
        String schooldCode = getSchoolCode();

        AttributesMap attributesMap = new AttributesMap();

        boolean isStudent = isStudent(person);
        boolean isTeacher = isTeacher(person);
        boolean isEmployee = isEmployee(person);
        boolean isAlumni = isAlumni(person);

        // REQUIRED 
        attributesMap.add(UL_STUDENT_ACTIVE_ATTRIBUTE + schooldCode, String.valueOf(isStudent).toUpperCase());
        attributesMap.add(UL_TEACHER_ACTIVE_ATTRIBUTE + schooldCode, String.valueOf(isTeacher).toUpperCase());
        attributesMap.add(UL_EMPLOYEE_ACTIVE_ATTRIBUTE + schooldCode, String.valueOf(isEmployee).toUpperCase());
        attributesMap.add(UL_ALUMNI_ATTRIBUTE + schooldCode, String.valueOf(isAlumni).toUpperCase());
        attributesMap.add(FULL_NAME_ATTRIBUTE, person.getName());
        attributesMap.add(GIVEN_NAME_ATTRIBUTE, person.getProfile().getGivenNames());
        String familyNames = person.getProfile().getFamilyNames();
        if (!StringUtils.isEmpty(familyNames)) {
            attributesMap.add(LAST_NAME_ATTRIBUTE, familyNames);
        } else {
            attributesMap.add(LAST_NAME_ATTRIBUTE, "-");
        }
        attributesMap.add(UL_BI_ATTRIBUTE, person.getDocumentIdNumber());
        YearMonthDay dateOfBirthYearMonthDay = person.getDateOfBirthYearMonthDay();
        if (dateOfBirthYearMonthDay != null) {
            attributesMap.add(UL_BIRTH_DATE_ATTRIBUTE, dateOfBirthYearMonthDay.toString("yyyyMMdd") + "000000Z");
        } else {
            attributesMap.add(UL_BIRTH_DATE_ATTRIBUTE, new DateTime().toString("yyyyMMddHHmmss.SSS") + "Z");
        }

        // OPTIONAL

        // José Lima said during the presentation of 1st year 1st time on the 25th August 
        // 2015 that we should send the personal email as soon as you have it, even if not
        // validated yet.
        //
        // 25 August 2015 
        Optional<? extends PartyContact> personalEmail =
                person.getPartyContacts(EmailAddress.class).stream()
                        .filter(emailAddress -> Boolean.TRUE.equals(emailAddress.getActive()) && emailAddress.isPersonalType())
                        .findFirst();
        if (personalEmail.isPresent()) {
            attributesMap.add(UL_EXTERNAL_EMAIL_ADDR_ATTRIBUTE, personalEmail.get().getPresentationValue());
        } else {
            attributesMap.add(UL_EXTERNAL_EMAIL_ADDR_ATTRIBUTE, person.getInstitutionalEmailAddressValue());
        }

        Gender gender = person.getGender();
        if (gender != null) {
            attributesMap.add(UL_SEX_ATTRIBUTE, gender.toString().substring(0, 1));
        }
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

        CgdCard cgdCard = CgdCard.findByPerson(person);
        if (cgdCard != null) {
            attributesMap.add(UL_MIFARE_ATTRIBUTE + getSchoolCode(), cgdCard.getMifareCode());
        }

        if (person.getStudent() != null) {
            collecStudentAttributes(person.getStudent(), attributesMap);
        }

        return attributesMap;
    }

    private static boolean isAlumni(Person person) {
        return !isStudent(person) && person.getStudent() != null && !person.getStudent().getRegistrationsSet().isEmpty();
    }

    private static boolean isStudent(Person person) {
        return StudentActive.isActiveStudent(person.getStudent());
    }

    private static boolean isTeacher(Person person) {
        ExecutionYear readCurrentExecutionYear = ExecutionYear.readCurrentExecutionYear();
        ExecutionYear previousExecutionYear = readCurrentExecutionYear.getPreviousExecutionYear();

        List<AcademicInterval> intervals = new ArrayList<AcademicInterval>();
        intervals.add(readCurrentExecutionYear.getAcademicInterval());
        intervals.add(previousExecutionYear.getAcademicInterval());
        intervals.addAll(readCurrentExecutionYear.getExecutionPeriodsSet().stream().map(ExecutionSemester::getAcademicInterval)
                .collect(Collectors.toList()));
        intervals.addAll(previousExecutionYear.getExecutionPeriodsSet().stream().map(ExecutionSemester::getAcademicInterval)
                .collect(Collectors.toList()));

        return person.getTeacher() != null
                && !person.getTeacher().getTeacherAuthorizationStream()
                        .filter(authorization -> intervals.contains(authorization.getExecutionSemester().getAcademicInterval()))
                        .collect(Collectors.toList()).isEmpty();
    }

    private static boolean isEmployee(Person person) {
        return DynamicGroup.get("employees").isMember(person.getUser());
    }

    public static LdapServerIntegrationConfiguration getDefaultConfiguration() {
        LdapServerIntegrationConfiguration defaultLdapServerIntegrationConfiguration =
                Bennu.getInstance().getDefaultLdapServerIntegrationConfiguration();
        if (defaultLdapServerIntegrationConfiguration == null) {
            throw new IllegalStateException("No ldap configuration is set has default!");
        }
        return defaultLdapServerIntegrationConfiguration;
    }

    public static boolean deleteUser(Person person) {
        return deleteUsers(Collections.singletonList(person), getDefaultConfiguration());
    }

    public static boolean deleteUsers(Collection<Person> people) {
        return deleteUsers(people, getDefaultConfiguration());
    }

    public static boolean deleteUsers(Collection<Person> people, LdapServerIntegrationConfiguration configuration) {
        boolean ableToSend = false;
        LdapClient client = configuration.getClient();
        try {
            if (client.login()) {
                ableToSend = true;
                for (Person person : people) {
                    try {
                        String personCommonName = getPersonCommonName(person, client, configuration);
                        if (!personCommonName.equals(configuration.getUsername())) {
                            client.deleteContext(personCommonName);
                        }
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
            }
        } finally {
            client.logout();
        }
        return ableToSend;
    }

    // Map with each property and a string array
    // position 0 local, position 1 ldap info 
    //

    private static Map<String, String[]> retrieveSyncInfo(AttributesMap collectedMap, String[] fields, Person person,
            LdapClient ldapClient, LdapServerIntegrationConfiguration defaultConfiguration) {
        Map<String, String[]> map = new LinkedHashMap<String, String[]>();

        for (String field : fields) {
            String[] values = new String[2];
            List<String> list = collectedMap.get(field);
            StringBuilder builder = concatenateValues(list);
            values[0] = builder.toString();
            map.put(field, values);
        }

        QueryReply query = ldapClient.query(COMMON_NAME + "=" + getCorrectCN(person.getUsername(), ldapClient), fields);
        if (query.getNumberOfResults() == 1) {
            QueryReplyElement queryReplyElement = query.getResults().get(0);
            for (String field : fields) {
                String[] values = map.get(field);
                List<String> list = queryReplyElement.getListAttribute(field);
                StringBuilder builder = concatenateValues(list);
                values[1] = builder.toString();
            }
        }

        return map;
    }

    public static Map<String, String[]> retrieveSyncInformation(Student student) {
        return retrieveSyncInformation(student, getDefaultConfiguration());
    }

    public static Map<String, String[]> retrieveSyncInformation(Student student,
            LdapServerIntegrationConfiguration defaultConfiguration) {
        Map<String, String[]> map = null;
        LdapClient client = defaultConfiguration.getClient();
        try {
            if (client.login()) {
                map = retrieveSyncInformation(student, client, defaultConfiguration);
            }
        } finally {
            client.logout();
        }
        return map;
    }

    private static Map<String, String[]> retrieveSyncInformation(Student student, LdapClient ldapClient,
            LdapServerIntegrationConfiguration defaultConfiguration) {

        return retrieveSyncInfo(collectAttributeMap(student), STUDENT_FIELDS_TO_SYNC, student.getPerson(), ldapClient,
                defaultConfiguration);
    }

    public static Map<String, String[]> retrieveSyncInformation(Person person) {
        return retrieveSyncInformation(person, getDefaultConfiguration());
    }

    public static Map<String, String[]> retrieveSyncInformation(Person person, LdapServerIntegrationConfiguration configuration) {

        Map<String, String[]> map = null;
        LdapClient client = configuration.getClient();
        try {
            if (client.login()) {
                map = retrieveSyncInformation(person, client, configuration);
            }
        } finally {
            client.logout();
        }
        return map;
    }

    private static Map<String, String[]> retrieveSyncInformation(Person person, LdapClient ldapClient,
            LdapServerIntegrationConfiguration defaultConfiguration) {
        return retrieveSyncInfo(collectAttributeMap(person), PERSON_FIELDS_TO_SYNC, person, ldapClient, defaultConfiguration);
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

    public static boolean deleteUser(String username, LdapServerIntegrationConfiguration configuration) {
        boolean ableToSend = false;
        LdapClient client = configuration.getClient();
        try {
            client.deleteContext(getObjectCommonName(username, client, configuration));
            ableToSend = true;
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return ableToSend;
    }

    private static boolean isSynched(Map<String, String[]> syncInformation) {
        Set<Entry<String, String[]>> entrySet = syncInformation.entrySet();
        for (Entry<String, String[]> entry : entrySet) {
            String[] parameter = entry.getValue();
            String value0 = parameter[0];
            String value1 = parameter[1];
            //
            // Yes yes... I know it's ugly.. but the date also has the time and we
            // don't want that!
            //
            // 29 July 2015 - Paulo Abrantes
            if (UL_BIRTH_DATE_ATTRIBUTE.equals(entry.getKey())) {
                value0 = value0 != null ? value0.substring(0, 9) : null;
                value1 = value1 != null ? value1.substring(0, 9) : null;
            }
            if ((value0 == null && value1 != null) || (value0 != null && value1 == null) || (!value0.equals(value1))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isUpdateNeeded(Person person, LdapServerIntegrationConfiguration configuration) {
        LdapClient client = configuration.getClient();
        boolean isUpdated = false;
        try {
            if (client.login()) {
                isUpdated = isUpdateNeeded(person, client, configuration);
            }
        } finally {
            client.logout();
        }
        return isUpdated;
    }

    private static boolean isUpdateNeeded(Person person, LdapClient client, LdapServerIntegrationConfiguration configuration) {
        Map<String, String[]> retrieveSyncInformation = retrieveSyncInformation(person, client, configuration);
        boolean isPersonUpdated = isSynched(retrieveSyncInformation);
        boolean isStudentUpdated =
                isPersonUpdated
                        && (person.getStudent() == null || isSynched(retrieveSyncInformation(person.getStudent(), client,
                                configuration)));

        return !isPersonUpdated || !isStudentUpdated;
    }

    public static boolean isUpdateNeeded(Person person) {
        return isUpdateNeeded(person, getDefaultConfiguration());
    }

    public static boolean isPersonAvailableInLdap(Person person) {
        return isPersonAvailableInLdap(person, getDefaultConfiguration());
    }

    private static boolean isPersonAvailableInLdap(Person person, LdapServerIntegrationConfiguration defaultConfiguration) {
        boolean isAvailable = false;
        LdapClient client = defaultConfiguration.getClient();
        try {
            if (client.login()) {
                isAvailable = isPersonAvailableInLdap(person, client, defaultConfiguration);
            }
        } finally {
            client.logout();
        }

        return isAvailable;
    }

    private static boolean isPersonAvailableInLdap(Person person, LdapClient client,
            LdapServerIntegrationConfiguration defaultConfiguration) {
        boolean isAvailable = false;
        try {
            String usernameToSearch = person.getUsername();

            QueryReply query =
                    client.query("(|(" + COMMON_NAME + "=" + usernameToSearch + ")(" + UL_FENIXUSER + "=" + usernameToSearch
                            + "))", new String[] { UL_FENIXUSER });
            if (query.getNumberOfResults() == 1) {
                isAvailable = true;
            }
        } catch (Throwable t) {
            t.printStackTrace();
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
                AttributesMap attributesMap = collectAttributeMap(student);
                List<String> objectClasses = new ArrayList<String>();
                QueryReply query =
                        client.query("(&(" + COMMON_NAME + "=" + getCorrectCN(person.getUsername(), client) + ")(objectClass="
                                + STUDENT_CLASS_PREFIX + getSchoolCode() + "))", new String[] { COMMON_NAME });
                if (query.getNumberOfResults() == 1) {
                    client.replaceInExistingContext(getPersonCommonName(person, client, configuration), objectClasses,
                            attributesMap);
                } else {
                    objectClasses.add(STUDENT_CLASS_PREFIX + getSchoolCode());
                    client.addToExistingContext(getPersonCommonName(person, client, configuration), objectClasses, attributesMap);
                }
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
        return createOrUpdatePeopleInLdap(Collections.singletonList(person), configuration);
    }

    public static boolean createOrUpdatePeopleInLdap(Collection<Person> people, LdapServerIntegrationConfiguration configuration) {
        boolean ableToSend = false;
        LdapClient client = configuration.getClient();
        boolean login = client.login();
        try {
            if (login) {

                for (Person person : people) {
                    String personCommonName = getPersonCommonName(person, client, configuration);
                    if (!isPersonAvailableInLdap(person, client, configuration)) {
                        List<String> objectClasses = new ArrayList<String>(Arrays.asList(OBJECT_CLASSES_TO_ADD));
                        if (person.getStudent() != null) {
                            objectClasses.add(STUDENT_CLASS_PREFIX + getSchoolCode());
                        }
                        try {
                            AttributesMap collectAttributeMap = collectAttributeMap(person);
                            // Only when creating the person we want to add this field
                            // afterwards we want this field to stay put. 
                            //
                            // 30 July 2015 - Paulo Abrantes
                            collectAttributeMap.add(UL_FENIXUSER, person.getUsername());
                            client.writeNewContext(personCommonName, objectClasses, collectAttributeMap);
                        } catch (Throwable t) {
                            t.printStackTrace();
                        }
                    } else if (isUpdateNeeded(person, client, configuration)) {
                        List<String> objectClasses = new ArrayList<String>(Arrays.asList(OBJECT_CLASSES_TO_ADD));
                        QueryReply query =
                                client.query("(&(" + COMMON_NAME + "=" + getCorrectCN(person.getUsername(), client)
                                        + ")(objectClass=" + STUDENT_CLASS_PREFIX + getSchoolCode() + "))",
                                        new String[] { COMMON_NAME });

                        if (query.getNumberOfResults() == 1) {
                            // The person is a student so we have to add this class as well
                            objectClasses.add(STUDENT_CLASS_PREFIX + getSchoolCode());
                        } else if (person.getStudent() != null) {
                            // In the creation we are considering that a person with a student in fenix will also have the student class in LDAP.
                            // This must also be considered here since further logic (e.g in collecStudentAttributes)
                            // Will add student attributes to the LDAP message based on this condition 
                            // PS: This condition could be tested before the ldap query improving performance, but let's keep it here temporarly to avoid risk
                            // Nuno Pinheiro 03/08/2015
                            objectClasses.add(STUDENT_CLASS_PREFIX + getSchoolCode());
                        }
                        try {
                            client.replaceInExistingContext(personCommonName, objectClasses, collectAttributeMap(person));
                            ableToSend = true;
                        } catch (Throwable t) {
                            t.printStackTrace();
                        }
                    }
                }
                ableToSend = true;
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
        return createOrUpdatePeopleInLdap(Collections.singletonList(person), configuration);
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
                            client.query(COMMON_NAME + "=" + person.getUsername(), new String[] {
                                    UL_INTERNAL_EMAIL_ADDR_ATTRIBUTE + schoolCode, UL_EXTERNAL_EMAIL_ADDR_ATTRIBUTE,
                                    INTERNET_EMAIL_ADDRESS, UL_BIRTH_DATE_ATTRIBUTE, UL_BI_ATTRIBUTE, UL_SEX_ATTRIBUTE,
                                    GIVEN_NAME_ATTRIBUTE, LAST_NAME_ATTRIBUTE });
                    if (query.getNumberOfResults() == 1) {
                        QueryReplyElement queryReplyElement = query.getResults().get(0);
                        String institutionalEmail =
                                queryReplyElement.getSimpleAttribute(UL_INTERNAL_EMAIL_ADDR_ATTRIBUTE + schoolCode);
                        if (StringUtils.isEmpty(institutionalEmail)) {
                            institutionalEmail = queryReplyElement.getSimpleAttribute(INTERNET_EMAIL_ADDRESS);
                        }
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

        if (person.getDocumentIdNumber() != null && !person.getDocumentIdNumber().equals(documentID)) {
            throw new IllegalStateException(
                    "Seems we are trying to update a person that does not match the ID. This should not happen!");
        }
        
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
            YearMonthDay dateOfBirthYearMonthDay = person.getDateOfBirthYearMonthDay();
            if (dateOfBirthYearMonthDay == null || !parseLocalDate.isEqual(dateOfBirthYearMonthDay)) {
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

    // Tools to help creating a fenix users only. This user will no be aligned by IDM. 
    // This will be used by the candidates.
    //
    // 30 July 2015 - Paulo Abrantes
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
            e.printStackTrace();
        }

        byte finalArray[] = outputStream.toByteArray();
        return "{SSHA}" + BaseEncoding.base64().encode(finalArray).trim();
    }

    public static boolean createUser(String username, String password, String salt) {
        return createUser(username, password, salt, getDefaultConfiguration());
    }

    public static boolean createUser(String username, String password, String salt,
            LdapServerIntegrationConfiguration configuration) {
        String generateLdapPassword = generateLdapPassword(password, salt);
        AttributesMap attributesMap = new AttributesMap();
        attributesMap.add(LAST_NAME_ATTRIBUTE, username);
        attributesMap.add(UL_FENIXUSER, username);
        attributesMap.add(USER_PASSWORD, generateLdapPassword);

        boolean ableToSend = false;
        LdapClient client = configuration.getClient();
        try {
            if (client.login()) {
                @SuppressWarnings("unchecked")
                List<String> classesList = new ArrayList<String>(Arrays.asList(OBJECT_CLASSES_TO_ADD_TEMP_USER));
                client.writeNewContext(getObjectCommonName(username, client, configuration), classesList, attributesMap);
                ableToSend = true;
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return ableToSend;
    }

}
