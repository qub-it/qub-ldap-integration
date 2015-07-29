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
import java.util.List;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.bennu.core.domain.Bennu;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.CallableWithoutException;
import pt.ist.fenixframework.FenixFramework;

import com.qubit.solution.fenixedu.integration.ldap.service.LdapIntegration;
import com.qubit.terra.ldapclient.LdapClient;

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

    public void sendAllUsers() {
        List<Person> collect =
                Bennu.getInstance().getPartysSet().stream().filter(p -> p instanceof Person).map(Person.class::cast)
                        .collect(Collectors.toList());

        int threadNumber = 10;
        int totalSize = collect.size();
        int split = totalSize / threadNumber + (totalSize % threadNumber);

        List<Thread> threads = new ArrayList<Thread>();

        for (int i = 0; i < threadNumber; i++) {
            int start = i * split;
            int end = Math.min(start + split, totalSize);
            SendPersonToLdapWorker sendPersonToLdapWorker = new SendPersonToLdapWorker(collect.subList(start, end), this);
            Thread t = new Thread(new Runnable() {

                @Override
                public void run() {
                    FenixFramework.getTransactionManager().withTransaction(sendPersonToLdapWorker);
                }
            });
            threads.add(t);
            t.start();
        }

        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static class SendPersonToLdapWorker implements CallableWithoutException<Object> {

        private List<Person> people;
        private LdapServerIntegrationConfiguration configuration;

        public SendPersonToLdapWorker(List<Person> people, LdapServerIntegrationConfiguration configuration) {
            this.people = people;
            this.configuration = configuration;
        }

        @Override
        public Object call() {
            long threadID = Thread.currentThread().getId();
            int totalSize = this.people.size();
            LOG.info("Starting thread  " + threadID + ". Processing " + totalSize);
            Thread.currentThread().setName(SendPersonToLdapWorker.class.getSimpleName() + "-" + threadID);
            int i = 0;
            for (Person person : this.people) {
                try {
                    if (++i % 100 == 0) {
                        LOG.info(new DateTime().toString("HH:mm:ss") + ": " + i + " / " + totalSize + " done");
                    }
                    if (LdapIntegration.isUpdateNeeded(person, configuration)) {
                        LdapIntegration.updatePersonInLdap(person, configuration);
                        Student student = person.getStudent();
                        if (student != null) {
                            LdapIntegration.updateStudentStatus(student, configuration);
                        }
                    }
                } catch (Throwable t) {
                    LOG.error("Problem sending person : " + person.getName() + "(user: " + person.getUsername() + ") to ldap", t);
                }
            }
            LOG.info("Finished thread " + threadID);
            return null;
        }
    }

    public void deleteAllUsers() {
        LdapIntegration.deleteUsers(Bennu.getInstance().getPartysSet().stream().filter(party -> party.isPerson())
                .map(Person.class::cast).collect(Collectors.toList()));
    }
}
