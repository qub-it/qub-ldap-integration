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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
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
import org.fenixedu.bennu.core.domain.UsernameHack;
import org.fenixedu.bennu.core.groups.DynamicGroup;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.ulisboa.specifications.ULisboaConfiguration;
import org.fenixedu.ulisboa.specifications.domain.idcards.CgdCard;
import org.fenixedu.ulisboa.specifications.domain.student.ActiveStudentOverride;
import org.fenixedu.ulisboa.specifications.service.StudentActive;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.YearMonthDay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.datetime.joda.DateTimeFormatterFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.io.BaseEncoding;
import com.qubit.solution.fenixedu.integration.ldap.domain.configuration.LdapServerIntegrationConfiguration;
import com.qubit.terra.ldapclient.AttributesMap;
import com.qubit.terra.ldapclient.LdapClient;
import com.qubit.terra.ldapclient.QueryReply;
import com.qubit.terra.ldapclient.QueryReplyElement;

import edu.emory.mathcs.backport.java.util.Arrays;
import edu.emory.mathcs.backport.java.util.Collections;
import pt.ist.fenixframework.Atomic;

public class LdapIntegration {

    private static final Logger logger = LoggerFactory.getLogger(LdapIntegration.class);

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
    private static final String GIVEN_NAME_ATTRIBUTE =
            ULisboaConfiguration.getConfiguration().getUseCustomGivenNames() ? "ULGivenName" : "givenName";
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
    private static String[] OBJECT_CLASSES_TO_ADD = { "person", "ULAuxUser", "ULAuxFac" + getSchoolCode(), "ULFenixUser" };
    private static String[] OBJECT_CLASSES_TO_ADD_TEMP_USER = { "person", "ULFenixUser" };

    // All the fields in this variable are from ULAuxUser LDAP class.
    //
    // This fields are used to show in the synchronized interface and also used
    // by isUpdatedNeeded(Person)
    //
    // To check if a given person is synchronized this fields are requested to
    // ldap and we also call
    // collectAttributeMap with the given person. Then for each field here we
    // iterate and check if both
    // values are equal or not. If all is equal then things are up to date,
    // otherwise they are not.
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
    private static final String[] STUDENT_FIELDS_TO_SYNC =
            new String[] { UL_STUDENT_CODE + getSchoolCode(), UL_COURSES + getSchoolCode() };

    private static String getSchoolCode() {
        return Bennu.getInstance().getInstitutionUnit().getAcronym();
    }

    // When we send a person to LDAP its CN is the person's username, which on
    // the first creation
    // is equal to the attribute in ULFenixUser. IDM then proceeds to do a match
    // and WILL REWRITE
    // CN to campus username.
    //
    // This means that if I create a user for me in the system called bennu123,
    // when it reaches
    // LDAP it will be CN=bennu123 ULFenixUser=bennu123. Since I have a campus
    // ID, IDM will do a
    // match and rewrite the CN. At this moment the register in LDAP will be
    // CN=paulo.abrantes
    // ULFenixUser=bennu123.
    //
    // If by that time no login has been done in Fenix my username in Fenix will
    // still be bennu123
    // but my CN will no longer be bennu123 but paulo.abrantes. This method
    // handles those situations
    // returning always the correct CN for any given username.
    //
    // Side note: When a user log's into Fenix the system detects the CN has
    // change and changes
    // the username accordingly.
    //
    // 30 July 2015 - Paulo Abrantes
    private static String getCorrectCN(final String username, final LdapClient client) {
        String ldapUsername = username;
        String[] replyAttributes = new String[] { COMMON_NAME };

        QueryReply query = client.query(UL_FENIXUSER + "=" + username, replyAttributes);
        if (query.getNumberOfResults() == 1) {
            QueryReplyElement next = query.getResults().iterator().next();
            ldapUsername = next.getSimpleAttribute(COMMON_NAME);
        } else if (query.getNumberOfResults() > 1) {
            logger.debug("Found more than one entry for username: " + username);
            String bennuPrefix = "bennu";
            for (QueryReplyElement reply : query.getResults()) {
                String simpleAttribute = reply.getSimpleAttribute(COMMON_NAME);
                if (!simpleAttribute.startsWith(bennuPrefix)) {
                    ldapUsername = simpleAttribute;
                    break;
                }
            }
        }
        return ldapUsername;
    }

    public static boolean isPersonAligned(final Person person) {
        return isPersonAligned(person, getDefaultConfiguration());
    }

    public static boolean isPersonAligned(final Person person, final LdapServerIntegrationConfiguration configuration) {
        LdapClient client = configuration.getClient();
        try {
            if (client.login()) {
                String correctCN = getCorrectCN(person.getUsername(), client);
                return correctCN != null && !correctCN.startsWith("bennu");
            }
        } finally {
            client.logout();
        }
        return false;
    }

    private static String getCNCommonName(final String cn, final LdapServerIntegrationConfiguration configuration) {
        return COMMON_NAME + "=" + cn + "," + configuration.getBaseDomain();
    }

    // When refering to an object in ldap we can't only give his CN but also the
    // domain. Both the
    // methods below
    //
    private static String getObjectCommonName(final String username, final LdapClient client,
            final LdapServerIntegrationConfiguration configuration) {
        return COMMON_NAME + "=" + getCorrectCN(username, client) + "," + configuration.getBaseDomain();
    }

    private static String getPersonCommonName(final Person person, final LdapClient client,
            final LdapServerIntegrationConfiguration configuration) {
        return getObjectCommonName(person.getUsername(), client, configuration);
    }

    // This is an extraction of collectAttributeMap(Student student) because
    // collectAttributeMap(Person person)
    // also calls this if a person has a student so every field is filled
    // singleshot.
    //
    // 30 July 2015 - Paulo Abrantes
    private static void collecStudentAttributes(final Student student, final AttributesMap attributesMap) {
        Person person = student.getPerson();
        ActiveStudentOverride overrideFor = ActiveStudentOverride.getOverrideFor(person);
        String studentNumber = String.valueOf(student.getNumber());
        if (overrideFor != null && !StringUtils.isEmpty(overrideFor.getOldStudentNumber())) {
            studentNumber = overrideFor.getOldStudentNumber();
        }

        attributesMap.add(UL_STUDENT_CODE + getSchoolCode(), studentNumber);
        if (isStudent(student.getPerson())) {
            List<String> courses = new ArrayList<>();
            for (Registration registration : student.getActiveRegistrations()) {
                try {
                    courses.add(registration.getDegreeCurricularPlanName() + " " + registration.getDegreeType().getName() + " "
                            + registration.getDegreeName());
                } catch (java.lang.Throwable t) {
                    courses.add("Error retrieving course");
                }
            }
            attributesMap.add(UL_COURSES + getSchoolCode(), courses.toArray(new String[] {}));
        }

    }

    // Collects data from a student, these are fields from ULAUXFac<School> ldap
    // class
    private static AttributesMap collectAttributeMap(final Student student) {
        AttributesMap attributesMap = new AttributesMap();
        collecStudentAttributes(student, attributesMap);
        return attributesMap;
    }

    // Collects data from a person, these are fields from ULAuxUser ldap class
    private static AttributesMap collectAttributeMap(final Person person) {
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

        // José Lima said during the presentation of 1st year 1st time on the
        // 25th August
        // 2015 that we should send the personal email as soon as you have it,
        // even if not
        // validated yet.
        //
        // 25 August 2015
        Optional<? extends PartyContact> personalEmail =
                person.getPartyContactsSet().stream().filter(partyContact -> partyContact instanceof EmailAddress
                        && Boolean.TRUE.equals(partyContact.getActive()) && partyContact.isPersonalType()).findFirst();
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
        if (defaultPhysicalAddress != null && !StringUtils.isEmpty(defaultPhysicalAddress.getAddress())
                && !StringUtils.isEmpty(defaultPhysicalAddress.getPostalCode())) {
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

    private static boolean isAlumni(final Person person) {
        return !isStudent(person) && person.getStudent() != null;
    }

    private static boolean isStudent(final Person person) {
        return StudentActive.isActiveStudent(person.getStudent());
    }

    private static boolean isTeacher(final Person person) {
        ExecutionYear readCurrentExecutionYear = ExecutionYear.readCurrentExecutionYear();
        ExecutionYear previousExecutionYear = readCurrentExecutionYear.getPreviousExecutionYear();

        List<AcademicInterval> intervals = new ArrayList<>();
        intervals.add(readCurrentExecutionYear.getAcademicInterval());
        intervals.add(previousExecutionYear.getAcademicInterval());
        intervals.addAll(readCurrentExecutionYear.getExecutionPeriodsSet().stream().map(ExecutionSemester::getAcademicInterval)
                .collect(Collectors.toList()));
        intervals.addAll(previousExecutionYear.getExecutionPeriodsSet().stream().map(ExecutionSemester::getAcademicInterval)
                .collect(Collectors.toList()));

        return person.getTeacher() != null && !person.getTeacher().getTeacherAuthorizationStream()
                .filter(authorization -> intervals.contains(authorization.getExecutionSemester().getAcademicInterval()))
                .collect(Collectors.toList()).isEmpty();
    }

    private static boolean isEmployee(final Person person) {
        return DynamicGroup.get("employees").isMember(person.getUser());
    }

    public static void resetUsername(final Person person) {
        resetUsername(person, getDefaultConfiguration());
    }

    public static void resetUsername(final Person person, final LdapServerIntegrationConfiguration configuration) {
        LdapClient ldapClient = configuration.getClient();
        try {
            if (ldapClient.login()) {
                QueryReply query = ldapClient.query(COMMON_NAME + "=" + getCorrectCN(person.getUsername(), ldapClient),
                        new String[] { UL_FENIXUSER });
                if (query.getNumberOfResults() == 1) {
                    QueryReplyElement next = query.getResults().iterator().next();
                    String originalFenixUsername = next.getSimpleAttribute(UL_FENIXUSER);
                    changeUsername(person.getUsername(), originalFenixUsername);
                }
            }
        } finally {
            ldapClient.logout();
        }
    }

    @Atomic
    private static void changeUsername(final String username, final String originalFenixUsername) {
        UsernameHack.changeUsername(username, originalFenixUsername);
    }

    public static LdapServerIntegrationConfiguration getDefaultConfiguration() {
        LdapServerIntegrationConfiguration defaultLdapServerIntegrationConfiguration =
                Bennu.getInstance().getDefaultLdapServerIntegrationConfiguration();
        if (defaultLdapServerIntegrationConfiguration == null) {
            throw new IllegalStateException("No ldap configuration is set has default!");
        }
        return defaultLdapServerIntegrationConfiguration;
    }

    public static boolean deleteUser(final Person person) {
        return deleteUsers(Collections.singletonList(person), getDefaultConfiguration());
    }

    public static boolean deleteUsers(final Collection<Person> people) {
        return deleteUsers(people, getDefaultConfiguration());
    }

    public static boolean deleteUsers(final Collection<Person> people, final LdapServerIntegrationConfiguration configuration) {
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
                            EXISTING_USERNAME_CACHE.invalidate(person.getUsername());
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

    private static Map<String, String[]> retrieveSyncInfo(final AttributesMap collectedMap, final String[] fields,
            final String personCN, final LdapClient ldapClient, final LdapServerIntegrationConfiguration defaultConfiguration) {
        Map<String, String[]> map = new LinkedHashMap<>();

        for (String field : fields) {
            String[] values = new String[2];
            List<String> list = collectedMap.get(field);
            StringBuilder builder = concatenateValues(list);
            values[0] = builder.toString();
            map.put(field, values);
        }

        QueryReply query = ldapClient.query(COMMON_NAME + "=" + personCN, fields);
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

    public static Map<String, String[]> retrieveSyncInformation(final Student student) {
        return retrieveSyncInformation(student, null, getDefaultConfiguration());
    }

    public static Map<String, String[]> retrieveSyncInformation(final Student student, final String personCN,
            final LdapServerIntegrationConfiguration defaultConfiguration) {
        Map<String, String[]> map = null;
        LdapClient client = defaultConfiguration.getClient();
        try {
            if (client.login()) {
                map = retrieveSyncInformation(student, personCN, client, defaultConfiguration);
            }
        } finally {
            client.logout();
        }
        return map;
    }

    private static Map<String, String[]> retrieveSyncInformation(final Student student, String personCN,
            final LdapClient ldapClient, final LdapServerIntegrationConfiguration defaultConfiguration) {
        if (personCN == null) {
            personCN = getCorrectCN(student.getPerson().getUsername(), ldapClient);
        }
        return retrieveSyncInfo(collectAttributeMap(student), STUDENT_FIELDS_TO_SYNC, personCN, ldapClient, defaultConfiguration);
    }

    public static Map<String, String[]> retrieveSyncInformation(final Person person) {
        return retrieveSyncInformation(person, null, getDefaultConfiguration());
    }

    public static Map<String, String[]> retrieveSyncInformation(final Person person, final String personCN,
            final LdapServerIntegrationConfiguration configuration) {

        Map<String, String[]> map = null;
        LdapClient client = configuration.getClient();
        try {
            if (client.login()) {
                map = retrieveSyncInformation(person, personCN, client, configuration);
            }
        } finally {
            client.logout();
        }
        return map;
    }

    private static Map<String, String[]> retrieveSyncInformation(final Person person, String personCN,
            final LdapClient ldapClient, final LdapServerIntegrationConfiguration defaultConfiguration) {
        if (personCN == null) {
            personCN = getCorrectCN(person.getUsername(), ldapClient);
        }
        return retrieveSyncInfo(collectAttributeMap(person), PERSON_FIELDS_TO_SYNC, personCN, ldapClient, defaultConfiguration);
    }

    private static StringBuilder concatenateValues(final List<String> list) {
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

    private static boolean deleteCommonName(final String commonName, final LdapServerIntegrationConfiguration configuration) {
        boolean ableToSend = false;
        LdapClient client = configuration.getClient();
        try {
            if (client.login()) {
                client.deleteContext(commonName);
                ableToSend = true;
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return ableToSend;
    }

    public static boolean deleteUser(final String username) {
        return deleteUser(username, getDefaultConfiguration());
    }

    public static boolean deleteUser(final String username, final LdapServerIntegrationConfiguration configuration) {
        LdapClient client = configuration.getClient();
        try {
            if (client.login()) {
                return deleteCommonName(getObjectCommonName(username, client, configuration), configuration);
            }
            return false;
        } finally {
            client.logout();
        }
    }

    private static boolean isSynched(final Map<String, String[]> syncInformation) {
        Set<Entry<String, String[]>> entrySet = syncInformation.entrySet();
        for (Entry<String, String[]> entry : entrySet) {
            String[] parameter = entry.getValue();
            String value0 = parameter[0];
            String value1 = parameter[1];
            //
            // Yes yes... I know it's ugly.. but the date also has the time and
            // we
            // don't want that!
            //
            // 29 July 2015 - Paulo Abrantes
            if (UL_BIRTH_DATE_ATTRIBUTE.equals(entry.getKey())) {
                value0 = value0 != null ? value0.substring(0, 9) : null;
                value1 = value1 != null ? value1.substring(0, 9) : null;
            }
            if (value0 == null && value1 != null || value0 != null && value1 == null || !value0.equals(value1)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isUpdateNeeded(final Person person, final LdapServerIntegrationConfiguration configuration) {
        LdapClient client = configuration.getClient();
        boolean isUpdated = false;
        try {
            if (client.login()) {
                isUpdated = isUpdateNeeded(person, null, client, configuration);
            }
        } finally {
            client.logout();
        }
        return isUpdated;
    }

    private static boolean isUpdateNeeded(final Person person, final String personCN, final LdapClient client,
            final LdapServerIntegrationConfiguration configuration) {
        Map<String, String[]> retrieveSyncInformation = retrieveSyncInformation(person, personCN, client, configuration);
        boolean isPersonUpdated = isSynched(retrieveSyncInformation);
        boolean isStudentUpdated = isPersonUpdated && (person.getStudent() == null
                || isSynched(retrieveSyncInformation(person.getStudent(), personCN, client, configuration)));

        return !isPersonUpdated || !isStudentUpdated;
    }

    public static boolean isUpdateNeeded(final Person person) {
        return isUpdateNeeded(person, getDefaultConfiguration());
    }

    public static boolean isPersonAvailableInLdap(final Person person) {
        return isPersonAvailableInLdap(person, getDefaultConfiguration());
    }

    private static boolean isPersonAvailableInLdap(final Person person,
            final LdapServerIntegrationConfiguration defaultConfiguration) {
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

    public static Map<String, String> getFieldValues(final Person person, final String... fields) {
        return getFieldValues(person, getDefaultConfiguration(), fields);
    }

    public static Map<String, String> getFieldValues(final Person person,
            final LdapServerIntegrationConfiguration defaultConfiguration, final String... fields) {
        Map<String, String> result = new HashMap<>();
        LdapClient client = defaultConfiguration.getClient();
        try {
            if (client.login()) {
                QueryReply query =
                        client.query("(" + COMMON_NAME + "=" + getCorrectCN(person.getUsername(), client) + ")", fields);
                if (query.getNumberOfResults() == 1) {
                    QueryReplyElement next = query.getResults().iterator().next();
                    for (String field : fields) {
                        String simpleAttribute = next.getSimpleAttribute(field);
                        result.put(field, simpleAttribute);
                    }
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            client.logout();
        }

        return result;
    }

    private final static Cache<String, Boolean> EXISTING_USERNAME_CACHE =
            CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.HOURS).maximumSize(10000).build();

    private static boolean isPersonAvailableInLdap(final Person person, final LdapClient client,
            final LdapServerIntegrationConfiguration defaultConfiguration) {
        boolean isAvailable = false;

        try {
            String usernameToSearch = person.getUsername();
            isAvailable = EXISTING_USERNAME_CACHE.getIfPresent(usernameToSearch) != null;

            if (!isAvailable) {
                QueryReply query = client.query(
                        "(|(" + COMMON_NAME + "=" + usernameToSearch + ")(" + UL_FENIXUSER + "=" + usernameToSearch + "))",
                        new String[] { COMMON_NAME, UL_FENIXUSER });
                if (query.getNumberOfResults() > 0) {
                    isAvailable = true;
                    EXISTING_USERNAME_CACHE.put(usernameToSearch, true);
                }
                if (query.getNumberOfResults() > 1) {
                    logger.debug("Found duplicate entry for user: " + person.getUsername());
                }
                if (query.getNumberOfResults() == 2) {
                    tryToFix(query, usernameToSearch);
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return isAvailable;
    }

    private static boolean isPersonAvailableWithAttribute(final Person person, final String attributeName,
            final LdapClient client, final LdapServerIntegrationConfiguration defaultConfiguration) {
        boolean isAvailable = false;
        try {
            String usernameToSearch = person.getUsername();

            QueryReply query = client.query("(& (|(" + COMMON_NAME + "=" + usernameToSearch + ")(" + UL_FENIXUSER + "="
                    + usernameToSearch + ")) (" + attributeName + "=*))", new String[] { UL_FENIXUSER });
            if (query.getNumberOfResults() > 0) {
                isAvailable = true;
            }
            if (query.getNumberOfResults() > 1) {
                logger.debug("Found duplicate entry for user: " + person.getUsername());
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return isAvailable;
    }

    public static boolean updateStudentStatus(final Student student) {
        return updateStudentStatus(student, getDefaultConfiguration());
    }

    public static boolean updateStudentStatus(final Student student, final LdapServerIntegrationConfiguration configuration) {
        Person person = student.getPerson();

        boolean ableToUpdateStudent = false;
        if (!isPersonAvailableInLdap(person)) {
            createPersonInLdap(person);
        }

        LdapClient client = configuration.getClient();
        if (client.login()) {
            try {
                AttributesMap attributesMap = collectAttributeMap(student);
                client.replaceInExistingContext(getPersonCommonName(person, client, configuration),
                        Arrays.asList(OBJECT_CLASSES_TO_ADD), attributesMap);
                ableToUpdateStudent = true;
            } finally {
                client.logout();
            }
        }

        return ableToUpdateStudent;

    }

    private static void tryToFix(final QueryReply query, final String username) {
        String bennuPrefix = "bennu";
        QueryReplyElement replyOne = query.getResults().get(0);
        QueryReplyElement replyTwo = query.getResults().get(1);

        String usernameNameOne = replyOne.getSimpleAttribute(COMMON_NAME);
        String usernameNameTwo = replyTwo.getSimpleAttribute(COMMON_NAME);
        if (usernameNameOne.startsWith(bennuPrefix) && !usernameNameTwo.startsWith(bennuPrefix)) {
            logger.info("Detected duplicate user: " + usernameNameOne + " and " + usernameNameTwo + " for " + username
                    + ". Removing " + usernameNameOne + " from ldap");
            deleteCommonName(COMMON_NAME + "=" + usernameNameOne + "," + getDefaultConfiguration().getBaseDomain(),
                    getDefaultConfiguration());
        } else if (!usernameNameOne.startsWith(bennuPrefix) && usernameNameTwo.startsWith(bennuPrefix)) {
            logger.info("Detected duplicate user: " + usernameNameOne + " and " + usernameNameTwo + " for " + username
                    + ". Removing " + usernameNameTwo + " from ldap");
            deleteCommonName(COMMON_NAME + "=" + usernameNameTwo + "," + getDefaultConfiguration().getBaseDomain(),
                    getDefaultConfiguration());
        }
    }

    public static boolean createPersonInLdap(final Person person) {
        return createPersonInLdap(person, getDefaultConfiguration());
    }

    public static boolean createPersonInLdap(final Person person, final LdapServerIntegrationConfiguration configuration) {
        return createOrUpdatePeopleInLdap(Collections.singletonList(person), configuration);
    }

    public static boolean createOrUpdatePeopleInLdap(final Collection<Person> people,
            final LdapServerIntegrationConfiguration configuration) {
        boolean ableToSend = false;
        LdapClient client = configuration.getClient();
        boolean login = client.login();
        try {
            if (login) {

                for (Person person : people) {
                    String personCN = getCorrectCN(person.getUsername(), client);
                    String personCommonName = getCNCommonName(personCN, configuration);
                    // This is the admin username we do not want to sync that
                    // one.
                    // 10 September 2015 - Paulo Abrantes
                    if (personCommonName.equals(configuration.getUsername())) {
                        continue;
                    }
                    if (!isPersonAvailableInLdap(person, client, configuration)) {
                        List<String> objectClasses = new ArrayList<String>(Arrays.asList(OBJECT_CLASSES_TO_ADD));
                        try {
                            AttributesMap collectAttributeMap = collectAttributeMap(person);
                            // Only when creating the person we want to add this
                            // field
                            // afterwards we want this field to stay put.
                            //
                            // 30 July 2015 - Paulo Abrantes
                            collectAttributeMap.add(UL_FENIXUSER, person.getUsername());
                            client.writeNewContext(personCommonName, objectClasses, collectAttributeMap);
                        } catch (Throwable t) {
                            t.printStackTrace();
                        }
                    } else if (isUpdateNeeded(person, personCN, client, configuration)) {
                        List<String> objectClasses = new ArrayList<String>(Arrays.asList(OBJECT_CLASSES_TO_ADD));
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

    public static boolean updatePersonInLdap(final Person person) {
        return updatePersonInLdap(person, getDefaultConfiguration());
    }

    public static boolean updatePersonInLdap(final Person person, final LdapServerIntegrationConfiguration configuration) {
        return createOrUpdatePeopleInLdap(Collections.singletonList(person), configuration);
    }

    public static boolean updatePersonUsingLdap(final Person person) {
        return updatePersonUsingLdap(person, getDefaultConfiguration());
    }

    public static boolean updatePersonUsingLdap(final Person person, final LdapServerIntegrationConfiguration configuration) {

        String schoolCode = getSchoolCode();
        LdapClient client = configuration.getClient();
        boolean ableToSync = false;

        try {
            if (client.login()) {
                try {
                    QueryReply query = client.query(COMMON_NAME + "=" + person.getUsername(),
                            new String[] { UL_INTERNAL_EMAIL_ADDR_ATTRIBUTE + schoolCode, UL_EXTERNAL_EMAIL_ADDR_ATTRIBUTE,
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
    private static void updatePerson(final Person person, final String instituionalEmail, final String personalEmail,
            final String birthDate, final String documentID, final String sex, final String givenNames, final String surnames) {

        // if (person.getDocumentIdNumber() != null &&
        // !person.getDocumentIdNumber().equals(documentID)) {
        // throw new IllegalStateException(
        // "Seems we are trying to update a person that does not match the ID.
        // This should not happen!");
        // }

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
                YearMonthDay yearMonthDay = new YearMonthDay(parseLocalDate.getYear(), parseLocalDate.getMonthOfYear(),
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
                && !equalInAnyLanguage(person.getPartyName(), givenNames + " " + surnames)) {
            String displayName = givenNames.split(" ")[0] + " " + surnames.split(" ")[0];
            person.getProfile().changeName(givenNames, surnames, displayName);
        }

    }

    private static boolean equalInAnyLanguage(final LocalizedString string, final String match) {
        for (Locale l : string.getLocales()) {
            if (string.getContent(l).equals(match)) {
                return true;
            }
        }
        return false;
    }

    public static boolean writeAtttribute(final Person person, final String attributeName, final String attributeValue) {
        return writeAtttribute(person, attributeName, attributeValue, getDefaultConfiguration());
    }

    public static boolean writeAtttribute(final Person person, final String attributeName, final String attributeValue,
            final LdapServerIntegrationConfiguration configuration) {

        LdapClient client = configuration.getClient();
        boolean attributeSent = false;

        try {
            if (client.login()) {
                AttributesMap attributesMap = new AttributesMap();
                attributesMap.add(attributeName, attributeValue);
                if (isPersonAvailableWithAttribute(person, attributeName, client, configuration)) {
                    client.replaceInExistingContext(getPersonCommonName(person, client, configuration), new ArrayList<String>(),
                            attributesMap);
                } else {
                    client.addToExistingContext(getPersonCommonName(person, client, configuration), new ArrayList<String>(),
                            attributesMap);
                }
            }
        } finally {
            client.logout();
        }
        return attributeSent;
    }

    // Tools to help creating a fenix users only. This user will no be aligned
    // by IDM.
    // This will be used by the candidates.
    //
    // 30 July 2015 - Paulo Abrantes
    public static String generateLdapPassword(final String password, final String salt) {
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

    public static boolean removePassword(final String username) {
        return removePassword(username, getDefaultConfiguration());
    }

    private static boolean removePassword(final String username, final LdapServerIntegrationConfiguration configuration) {
        AttributesMap attributesMap = new AttributesMap();
        attributesMap.add(USER_PASSWORD, (String) null);

        boolean ableToSend = false;
        LdapClient client = configuration.getClient();
        try {
            if (client.login()) {
                QueryReply query =
                        client.query("(& (" + COMMON_NAME + "=" + getCorrectCN(username, client) + ") (" + USER_PASSWORD + "=*))",
                                new String[] { COMMON_NAME });
                if (query.getNumberOfResults() == 1) {
                    String objectCommonName = getObjectCommonName(username, client, configuration);
                    logger.info("Removing password in ldap for " + objectCommonName);
                    client.removeFromExistingContext(objectCommonName, attributesMap);
                }
                ableToSend = true;
            }
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            client.logout();
        }
        return ableToSend;
    }

    public static boolean changePassword(final String username, final String password, final String salt) {
        return changePassword(username, password, salt, getDefaultConfiguration());
    }

    public static boolean changePassword(final String username, final String password, final String salt,
            final LdapServerIntegrationConfiguration configuration) {
        String generateLdapPassword =
                ULisboaConfiguration.getConfiguration().getSendHashedPassword() ? generateLdapPassword(password, salt) : password;
        AttributesMap attributesMap = new AttributesMap();
        attributesMap.add(USER_PASSWORD, generateLdapPassword);

        boolean ableToSend = false;
        LdapClient client = configuration.getClient();
        try {
            if (client.login()) {
                client.replaceInExistingContext(getObjectCommonName(username, client, configuration), new ArrayList<String>(),
                        attributesMap);
                ableToSend = true;
            }
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            client.logout();
        }
        return ableToSend;
    }

    public static boolean changeCN(final Person person, final String newUsername) {
        return changeCN(person, newUsername, getDefaultConfiguration());
    }

    public static boolean changeCN(final Person person, final String newUsername,
            final LdapServerIntegrationConfiguration configuration) {
        boolean ableToRename = false;
        LdapClient client = configuration.getClient();
        try {
            if (client.login()) {
                ableToRename = client.renameContext(getPersonCommonName(person, client, configuration),
                        COMMON_NAME + "=" + newUsername + "," + configuration.getBaseDomain());
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return ableToRename;
    }

    public static boolean existsUser(final String username, final LdapServerIntegrationConfiguration configuration) {
        LdapClient client = configuration.getClient();
        try {
            if (client.login()) {
                QueryReply query = client.query("(" + COMMON_NAME + "=" + username + ")", new String[] { COMMON_NAME });
                return query.getNumberOfResults() > 0;
            }
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            client.logout();
        }
        return false;
    }

    public static boolean existsUser(final String username) {
        return existsUser(username, getDefaultConfiguration());
    }

    public static boolean createUser(final String username, final String password, final String salt) {
        return createUser(username, password, salt, getDefaultConfiguration());
    }

    public static boolean createUser(final String username, final String password, final String salt,
            final LdapServerIntegrationConfiguration configuration) {
        String generateLdapPassword =
                ULisboaConfiguration.getConfiguration().getSendHashedPassword() ? generateLdapPassword(password, salt) : password;
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
        } finally {
            client.logout();
        }
        return ableToSend;
    }
}
