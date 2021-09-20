package com.qubit.solution.fenixedu.integration.ldap.service.task;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.CronTask;
import org.fenixedu.bennu.scheduler.annotation.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qubit.solution.fenixedu.integration.ldap.domain.configuration.LdapServerIntegrationConfiguration;
import com.qubit.solution.fenixedu.integration.ldap.domain.configuration.LdapServerIntegrationConfiguration.BatchWorker;
import com.qubit.solution.fenixedu.integration.ldap.service.LdapIntegration;

import pt.ist.fenixframework.FenixFramework;

@Task(englishTitle = "Retrieve institutional emails for bennu users.")
public class RetrieveInstitutionalEmailTask extends CronTask {

    private static final Logger LOG = LoggerFactory.getLogger(RetrieveInstitutionalEmailTask.class);

    @Override
    public void runTask() throws Exception {
        List<Person> people = Person.readAllPersons().stream()
                .filter(p -> p.getUsername().startsWith("bennu") && p.getInstitutionalEmailAddress() == null)
                .collect(Collectors.toList());;

        LdapServerIntegrationConfiguration defaultConfiguration = LdapIntegration.getDefaultConfiguration();
        defaultConfiguration.applyOperationToAllUsers(people, RetrieveEmailWorker.class,
                defaultConfiguration.getNumberOfWorkers(), false);
    }

    public static class RetrieveEmailWorker implements BatchWorker<Person> {

        private List<Person> people;
        private LdapServerIntegrationConfiguration configuration;
        private String schoolCode;

        @Override
        public Object call() {
            long threadID = Thread.currentThread().getId();
            int totalSize = this.people.size();
            long startTime = System.currentTimeMillis();
            String internalMailField = "ULInternalEmailAddr" + schoolCode;
            String mailField = "mail";
            LOG.info("Starting thread  " + threadID + "synching institutional emails for " + totalSize);
            Thread.currentThread().setName(RetrieveEmailWorker.class.getSimpleName() + "-" + threadID);
            for (Person person : people) {
                Map<String, String> fieldValues =
                        LdapIntegration.getFieldValues(person, configuration, internalMailField, mailField);
                if (!fieldValues.isEmpty()) {
                    String mail = fieldValues.get(internalMailField);
                    if (StringUtils.isBlank(mail)) {
                        mail = fieldValues.get(mailField);
                    }
                    if (mail != null && StringUtils.isNotBlank(mail)) {
                        updateInstitutionalEmail(person, mail);
                    }
                }
            }

            long endTime = System.currentTimeMillis();
            LOG.info("Stopping thread  " + threadID + " [took: " + ((endTime - startTime) / 1000) + " seconds]");
            return null;
        }

        private void updateInstitutionalEmail(Person person, String email) {
            FenixFramework.atomic(() -> person.setInstitutionalEmailAddressValue(email));
            LOG.info("Synching email for: " + person.getUsername() + " to " + email);
        }

        @Override
        public void configure(List<Person> objects, LdapServerIntegrationConfiguration configuration) {
            this.people = objects;
            this.configuration = configuration;
            this.schoolCode = Bennu.getInstance().getInstitutionUnit().getAcronym();
        }

    }
}
