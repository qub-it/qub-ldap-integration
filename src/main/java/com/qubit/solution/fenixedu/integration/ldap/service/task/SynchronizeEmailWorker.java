package com.qubit.solution.fenixedu.integration.ldap.service.task;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.bennu.core.domain.Bennu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qubit.solution.fenixedu.integration.ldap.domain.configuration.LdapServerIntegrationConfiguration;
import com.qubit.solution.fenixedu.integration.ldap.domain.configuration.LdapServerIntegrationConfiguration.BatchWorker;
import com.qubit.solution.fenixedu.integration.ldap.service.LdapIntegration;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;

public class SynchronizeEmailWorker implements BatchWorker<Person> {

    private List<Person> people;
    private LdapServerIntegrationConfiguration configuration;
    private static final Logger LOG = LoggerFactory.getLogger(SynchronizeEmailWorker.class);
    private static final String UL_INTERNAL_EMAIL_ADDR_ATTRIBUTE = "ULInternalEmailAddr";
    private static final String INTERNET_EMAIL_ADDRESS = "mail";

    private String schoolCode;

    @Override
    public Object call() {
        long threadID = Thread.currentThread().getId();
        int totalSize = this.people.size();
        long startTime = System.currentTimeMillis();
        LOG.info("Starting thread  " + threadID + ". Processing " + totalSize);
        Thread.currentThread().setName(SynchronizeEmailWorker.class.getSimpleName() + "-" + threadID);

        int pageSize = 500;
        if (totalSize < pageSize) {
            LOG.info("Less than " + pageSize + " elements sending a single batch");
            send(people);
        } else {
            int pages = (totalSize / pageSize) + (totalSize % pageSize > 0 ? 1 : 0);
            LOG.info("More than " + pageSize + " elements, number of pages: " + pages);
            for (int i = 0; i < pages; i++) {
                int min = pageSize * i;
                int max = Math.min(min + pageSize, totalSize);
                LOG.info("Sending page : " + i + " out of " + pages);
                Thread thread = new Thread(() -> send(this.people.subList(min, max)));
                thread.start();
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        long endTime = System.currentTimeMillis();
        LOG.info("Finished thread " + threadID + " [took: " + ((endTime - startTime) / 1000) + " seconds]");
        return null;
    }

    @Atomic(mode = TxMode.READ)
    private void send(Collection<Person> people) {
        for (Person person : people) {
            Map<String, String> fieldValues = LdapIntegration.getFieldValues(person, this.configuration, INTERNET_EMAIL_ADDRESS,
                    UL_INTERNAL_EMAIL_ADDR_ATTRIBUTE + schoolCode);
            String mailField = fieldValues.get(INTERNET_EMAIL_ADDRESS);
            String internalMailField = fieldValues.get(UL_INTERNAL_EMAIL_ADDR_ATTRIBUTE);
            if (!StringUtils.isEmpty(mailField) && !mailField.equals(internalMailField)) {
                LOG.info(person.getUsername() + " had institutional email: " + internalMailField + " will be changed to: "
                        + mailField);
                FenixFramework.atomic(() -> person.setInstitutionalEmailAddressValue(mailField));
            } else {
                LOG.debug(person.getUsername() + " is ok");

            }
        }
    }

    @Override
    public void configure(List<Person> people, LdapServerIntegrationConfiguration configuration) {
        this.people = people;
        this.configuration = configuration;
        this.schoolCode = Bennu.getInstance().getInstitutionUnit().getAcronym();
    }
}