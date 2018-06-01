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
package org.fenixedu.bennu.core.domain;

//
// This is an hack to allow username align from ldap into FenixEdu system.
// The importData system also uses a similiar mechanism, we should refactor
// this to fenixedu-ulisboa-specification.
//
// 24 June 2015 - Paulo Abrantes
public class UsernameHack {

    public static void changeUsername(String oldUsername, String newUsername) {
        User findByUsername = User.findByUsername(oldUsername);
        if (findByUsername != null) {
            findByUsername.setUsername(newUsername);
            UserProfile profile = findByUsername.getProfile();
            if (profile != null) {
                String avatarUrl = profile.getAvatarUrl();
                if (avatarUrl != null && avatarUrl.contains(oldUsername)) {
                    String newAvatarURL = avatarUrl.replace(oldUsername, newUsername);
                    profile.setAvatarUrl(newAvatarURL);
                }
            }
        }
    }

    public static String getUserSalt(User user) {
        return user.getSalt();
    }
}
