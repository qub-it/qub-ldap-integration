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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.bennu.core.domain.Bennu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qubit.solution.fenixedu.integration.ldap.service.AttributeResolver;
import com.qubit.solution.fenixedu.integration.ldap.service.LdapIntegration;
import com.qubit.terra.ldapclient.LdapClient;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.CallableWithoutException;

public class LdapServerIntegrationConfiguration extends LdapServerIntegrationConfiguration_Base {

    private static final Logger LOG = LoggerFactory.getLogger(LdapServerIntegrationConfiguration.class);

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
        setEmployeeProfile(null);
        setAllowNonBennusToLogin(true);
    }

    public boolean isNonBennuUserAllowedToLogin() {
        return Boolean.TRUE.equals(getAllowNonBennusToLogin());
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

    public void applyOperationToAllUsers(List<Person> collect, final Class<? extends BatchWorker<Person>> callableClass,
            int threadNumber, boolean block) {
        int totalSize = collect.size();
        int split = totalSize / threadNumber + (totalSize % threadNumber);

        List<Thread> threads = new ArrayList<Thread>();

        for (int i = 0; i < threadNumber; i++) {
            int start = i * split;
            int end = Math.min(start + split, totalSize);
            BatchWorker<Person> worker = null;
            try {
                worker = callableClass.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
            if (worker != null) {
                worker.configure(collect.subList(start, end), this);
                final BatchWorker<Person> finalReference = worker;
                Thread t = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        execute(finalReference);
                    }
                });
                threads.add(t);
                t.start();
            }
        }

        if (block) {
            for (Thread t : threads) {
                try {
                    t.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Atomic(mode = TxMode.READ)
    private static void execute(BatchWorker<Person> batchWorker) {
        batchWorker.call();
    }

    public static interface BatchWorker<T extends Object> extends CallableWithoutException<Object> {

        public void configure(List<T> objects, LdapServerIntegrationConfiguration configuration);
    }

    public static abstract class AttributeWriter implements BatchWorker<Person> {

        private List<Person> people;
        private LdapServerIntegrationConfiguration configuration;

        private String attributeName;
        private AttributeResolver attributeResolver;

        public AttributeWriter(String attributeName, AttributeResolver attributeResolver) {
            this.attributeName = attributeName;
            this.attributeResolver = attributeResolver;
        }

        @Override
        public Object call() {
            long threadID = Thread.currentThread().getId();
            int totalSize = this.people.size();
            long startTime = System.currentTimeMillis();
            LOG.info("Starting thread  " + threadID + ". Writting attribute " + attributeName + " for " + totalSize);
            Thread.currentThread().setName(attributeName + "-" + threadID);
            for (Person person : people) {
                LdapIntegration.writeAtttribute(person, attributeName, attributeResolver.getAttributeValueFor(person));
            }
            long endTime = System.currentTimeMillis();
            LOG.info("Stopping thread  " + threadID + " [took: " + ((endTime - startTime) / 1000) + " seconds]");
            return null;
        }

        @Override
        public void configure(List<Person> people, LdapServerIntegrationConfiguration configuration) {
            this.people = people;
            this.configuration = configuration;

        }

    }

    public static class DeletePersonFromLdapWorker implements BatchWorker<Person> {
        private List<Person> people;
        private LdapServerIntegrationConfiguration configuration;

        @Override
        public Object call() {
            long threadID = Thread.currentThread().getId();
            int totalSize = this.people.size();
            long startTime = System.currentTimeMillis();
            LOG.info("Starting thread  " + threadID + ". Deleting " + totalSize);
            Thread.currentThread().setName(DeletePersonFromLdapWorker.class.getSimpleName() + "-" + threadID);
            LdapIntegration.deleteUsers(people, configuration);
            long endTime = System.currentTimeMillis();
            LOG.info("Stopping thread  " + threadID + " [took: " + ((endTime - startTime) / 1000) + " seconds]");
            return null;
        }

        @Override
        public void configure(List<Person> people, LdapServerIntegrationConfiguration configuration) {
            this.people = people;
            this.configuration = configuration;
        }

    }

    public static class SendPersonToLdapWorker implements BatchWorker<Person> {

        private List<Person> people;
        private LdapServerIntegrationConfiguration configuration;

        @Override
        public Object call() {
            long threadID = Thread.currentThread().getId();
            int totalSize = this.people.size();
            long startTime = System.currentTimeMillis();
            LOG.info("Starting thread  " + threadID + ". Processing " + totalSize);
            Thread.currentThread().setName(SendPersonToLdapWorker.class.getSimpleName() + "-" + threadID);

            if (totalSize < 1000) {
                LOG.info("Less than 1000 elements sending a single batch");
                send(people);
            } else {
                int pageSize = 1000;
                int pages = (totalSize / pageSize) + (totalSize % 1000 > 0 ? 1 : 0);
                LOG.info("More than 1000 elements, number of pages: " + pages);
                for (int i = 0; i < pages; i++) {
                    int min = pageSize * i;
                    int max = Math.min(min + pageSize, totalSize);
                    LOG.info("Sending page : " + i + " out of " + pages);
                    // Forcing that each batch send is a thread of it its own, so it's a diferent Tx. Otherwise we might create
                    // very large transactions that affects system performance. 
                    //
                    // 28 April 2017 - Paulo Abrantes
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
            LdapIntegration.createOrUpdatePeopleInLdap(people, this.configuration);
        }

        @Override
        public void configure(List<Person> people, LdapServerIntegrationConfiguration configuration) {
            this.people = people;
            this.configuration = configuration;
        }
    }

    public void sendAllUsers() {
        applyOperationToAllUsers(Bennu.getInstance().getPartysSet().stream().filter(p -> p instanceof Person)
                .map(Person.class::cast).collect(Collectors.toList()), SendPersonToLdapWorker.class, getNumberOfWorkers(), false);
    }

    public void deleteAllUsers() {
        applyOperationToAllUsers(Bennu.getInstance().getPartysSet().stream().filter(p -> p instanceof Person)
                .map(Person.class::cast).collect(Collectors.toList()), DeletePersonFromLdapWorker.class, getNumberOfWorkers(),
                false);
    }

    @Override
    public Integer getNumberOfWorkers() {
        Integer numberOfWorkers = super.getNumberOfWorkers();
        return numberOfWorkers != null ? numberOfWorkers : 10;
    }
}
