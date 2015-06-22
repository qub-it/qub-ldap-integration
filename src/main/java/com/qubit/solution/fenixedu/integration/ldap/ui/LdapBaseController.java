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
package com.qubit.solution.fenixedu.integration.ldap.ui;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;

public class LdapBaseController {
    // The HTTP Request that can be used internally in the controller
    protected @Autowired HttpServletRequest request;

    // The entity in the Model

    // The list of INFO messages that can be showed on View
    protected void addInfoMessage(String message, Model m) {
        ((List<String>) m.asMap().get("infoMessages")).add(message);
    }

    // The list of WARNING messages that can be showed on View
    protected void addWarningMessage(String message, Model m) {
        ((List<String>) m.asMap().get("warningMessages")).add(message);
    }

    // The list of ERROR messages that can be showed on View
    protected void addErrorMessage(String message, Model m) {
        ((List<String>) m.asMap().get("errorMessages")).add(message);
    }

    protected void clearMessages(Model model) {
        model.addAttribute("infoMessages", new ArrayList<String>());
        model.addAttribute("warningMessages", new ArrayList<String>());
        model.addAttribute("errorMessages", new ArrayList<String>());
    }

    @ModelAttribute
    protected void addModelProperties(Model model) {
        model.addAttribute("infoMessages", new ArrayList<String>());
        model.addAttribute("warningMessages", new ArrayList<String>());
        model.addAttribute("errorMessages", new ArrayList<String>());

        // Add here more attributes to the Model
        // model.addAttribute(<attr1Key>, <attr1Value>);
        // ....
    }

}
