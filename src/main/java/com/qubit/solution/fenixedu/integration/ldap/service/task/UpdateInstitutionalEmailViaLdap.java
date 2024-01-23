package com.qubit.solution.fenixedu.integration.ldap.service.task;

import java.util.stream.Collectors;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.CronTask;
import org.fenixedu.bennu.scheduler.annotation.Task;

import com.qubit.solution.fenixedu.integration.ldap.domain.configuration.LdapServerIntegrationConfiguration;

@Task(englishTitle = "Ensures that if ULInternalEmailAddr<school> deviates from mail attributes it's updated again")
public class UpdateInstitutionalEmailViaLdap extends CronTask {

    @Override
    public void runTask() throws Exception {
        LdapServerIntegrationConfiguration defaultLdapServerIntegrationConfiguration =
                Bennu.getInstance().getDefaultLdapServerIntegrationConfiguration();
        if (defaultLdapServerIntegrationConfiguration != null) {
            defaultLdapServerIntegrationConfiguration.applyOperationToAllUsers(
                    Person.readAllPersons().stream().filter(p -> !p.getUser().isAuthManageable()).collect(Collectors.toList()),
                    SynchronizeEmailWorker.class, defaultLdapServerIntegrationConfiguration.getNumberOfWorkers(), false);
        }
    }

}
