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
package com.qubit.solution.fenixedu.integration.ldap.service.task;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.annotation.Task;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ist.fenixframework.CallableWithoutException;
import pt.ist.fenixframework.FenixFramework;

import com.qubit.solution.fenixedu.integration.ldap.service.LdapIntegration;

@Task(englishTitle = "Send users to ldap")
public class SendPersonToLdapTask extends CustomTask {

    private static final Logger LOG = LoggerFactory.getLogger(SendPersonToLdapTask.class);

    @Override
    public void runTask() throws Exception {
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
            SendPersonToLdapWorker sendPersonToLdapWorker = new SendPersonToLdapWorker(collect.subList(start, end));
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
            t.join();
        }
    }

    private static class SendPersonToLdapWorker implements CallableWithoutException<Object> {

        private List<Person> people;

        public SendPersonToLdapWorker(List<Person> people) {
            this.people = people;
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
                    LdapIntegration.updatePersonInLdap(person);
                    Student student = person.getStudent();
                    if (student != null) {
                        LdapIntegration.updateStudentStatus(student);
                    }
                } catch (Throwable t) {
                    LOG.error("Problem sending person : " + person.getName() + "(user: " + person.getUsername() + ") to ldap", t);
                }
            }
            LOG.info("Finished thread " + threadID);
            return null;
        }
    }

}
