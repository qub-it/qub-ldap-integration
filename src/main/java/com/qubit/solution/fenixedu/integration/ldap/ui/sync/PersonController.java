package com.qubit.solution.fenixedu.integration.ldap.ui.sync;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.organizationalStructure.Party;
import org.fenixedu.bennu.spring.portal.SpringFunctionality;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.qubit.solution.fenixedu.integration.ldap.service.LdapIntegration;
import com.qubit.solution.fenixedu.integration.ldap.ui.LdapBaseController;
import com.qubit.solution.fenixedu.integration.ldap.ui.LdapController;

@Component("com.qubit.solution.fenixedu.integration.ldap.ui.sync")
@SpringFunctionality(app = LdapController.class, title = "label.title.syncManagment", accessGroup = "#managers")
//CHANGE_ME accessGroup = "group1 | group2 | groupXPTO"
@RequestMapping("/ldap/sync/person")
public class PersonController extends LdapBaseController {

    @RequestMapping
    public String home(Model model) {
        return "forward:/ldap/sync/person/";
    }

    private void setPerson(Person person, Model m) {
        m.addAttribute("person", person);
    }

    private Person getPerson(Model m) {
        return (Person) m.asMap().get("person");
    }

    @RequestMapping(value = "/")
    public String search(@RequestParam(value = "name", required = false, defaultValue = "") String name, @RequestParam(
            value = "username", required = false) String username,
            @RequestParam(value = "documentidnumber", required = false) String documentIdNumber, Model model) {

        List<Person> searchpersonResultsDataSet = filterSearchPerson(name, username, documentIdNumber);
        model.addAttribute("searchpersonResultsDataSet", searchpersonResultsDataSet);
        return "ldap/sync/person/search";
    }

    private List<Person> filterSearchPerson(String name, String username, String documentIdNumber) {
        Stream<Person> stream =
                StringUtils.isEmpty(name) ? Party.getPartysSet(Person.class).stream() : Person.findPersonStream(name,
                        Integer.MAX_VALUE);
        return stream.filter(person -> StringUtils.isEmpty(username) || username.equals(person.getUsername()))
                .filter(person -> StringUtils.isEmpty(documentIdNumber) || documentIdNumber.equals(person.getDocumentIdNumber()))
                .collect(Collectors.toList());
    }

    @RequestMapping(value = "/search/view/{oid}")
    public String processSearchToViewAction(@PathVariable("oid") Person person, Model model) {
        return "redirect:/ldap/sync/person/syncPerson" + "/" + person.getExternalId();
    }

    @RequestMapping(value = "/syncPerson/{oid}")
    public String syncPerson(@PathVariable("oid") Person person, Model model) {
        setPerson(person, model);
        model.addAttribute("syncInformation", LdapIntegration.retrieveSyncInformation(person));
        return "ldap/sync/person/syncPerson";
    }

    @RequestMapping(value = "/sendtoldap/{oid}", method = RequestMethod.POST)
    public String sendToLdap(@PathVariable("oid") Person person, Model model) {
        setPerson(person, model);
        LdapIntegration.updatePersonInLdap(person);
        return "redirect:/ldap/sync/person/syncPerson" + "/" + person.getExternalId();
    }

    @RequestMapping(value = "/receivefromldap/{oid}", method = RequestMethod.POST)
    public String receiveFromLdap(@PathVariable("oid") Person person, Model model) {
        setPerson(person, model);
        LdapIntegration.updatePersonUsingLdap(person);
        return "redirect:/ldap/sync/person/syncPerson" + "/" + person.getExternalId();
    }
}
