/*
 * DavMail POP/IMAP/SMTP/CalDav/LDAP Exchange Gateway
 * Copyright (C) 2010  Mickael Guessant
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package davmail.exchange;

import davmail.Settings;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Test contact search.
 */
@SuppressWarnings({"UseOfSystemOutOrSystemErr"})
public class TestExchangeSessionSearchContact extends AbstractExchangeSessionTestCase {
    public void testSearchPublicContacts() throws IOException {
        String folderPath = Settings.getProperty("davmail.publicContactFolder");
        List<ExchangeSession.Contact> contacts = session.searchContacts(folderPath, ExchangeSession.CONTACT_ATTRIBUTES, null, 0);
        int count = 0;
        for (ExchangeSession.Contact contact : contacts) {
            System.out.println("Contact " + (++count) + '/' + contacts.size() + session.getItem(folderPath, contact.getName()));
        }
    }

    public void testSearchPublicContactsRange() throws IOException {
        String folderPath = Settings.getProperty("davmail.publicContactFolder");
        List<ExchangeSession.Contact> contacts = session.searchContacts(folderPath, ExchangeSession.CONTACT_ATTRIBUTES, null, 10);
        assertEquals(10, contacts.size());
    }

    public void testSearchPublicContactsWithPicture() throws IOException {
        String folderPath = Settings.getProperty("davmail.publicContactFolder");
        List<ExchangeSession.Contact> contacts = session.searchContacts(folderPath, ExchangeSession.CONTACT_ATTRIBUTES, session.isTrue("haspicture"), 0);
        int count = 0;
        for (ExchangeSession.Contact contact : contacts) {
            System.out.println("Contact " + (++count) + '/' + contacts.size() + contact.getBody());
            assertNotNull(session.getContactPhoto(contact));
        }
    }

    public void testSearchContacts() throws IOException {
        List<ExchangeSession.Contact> contacts = session.searchContacts(ExchangeSession.CONTACTS, ExchangeSession.CONTACT_ATTRIBUTES, null, 0);
        for (ExchangeSession.Contact contact : contacts) {
            System.out.println(session.getItem(ExchangeSession.CONTACTS, contact.getName()));
        }
    }

    public void testSearchContactsUidOnly() throws IOException {
        Set<String> attributes = new HashSet<String>();
        attributes.add("uid");
        List<ExchangeSession.Contact> contacts = session.searchContacts(ExchangeSession.CONTACTS, attributes, null, 0);
        for (ExchangeSession.Contact contact : contacts) {
            System.out.println(contact);
        }
    }

    public void testSearchContactsByUid() throws IOException {
        Set<String> attributes = new HashSet<String>();
        attributes.add("uid");
        List<ExchangeSession.Contact> contacts = session.searchContacts(ExchangeSession.CONTACTS, attributes, null, 0);
        for (ExchangeSession.Contact contact : contacts) {
            System.out.println(session.searchContacts(ExchangeSession.CONTACTS, attributes, session.isEqualTo("uid", contact.get("uid")), 0));
        }
    }

    public void testGalFind() throws IOException {
        // find a set of contacts
        Map<String, ExchangeSession.Contact> contacts = session.galFind(session.startsWith("cn", "a"), null, 100);
        for (ExchangeSession.Contact contact : contacts.values()) {
            System.out.println(contact);
        }
        if (!contacts.isEmpty()) {
            ExchangeSession.Contact testContact = contacts.values().iterator().next();
            contacts = session.galFind(session.isEqualTo("cn", testContact.get("cn")), null, 100);
            assertEquals(1, contacts.size());
            contacts = session.galFind(session.isEqualTo("smtpemail1", testContact.get("smtpemail1")), null, 100);
            assertEquals(1, contacts.size());
            contacts = session.galFind(session.startsWith("smtpemail1", testContact.get("smtpemail1")), null, 100);
            assertEquals(1, contacts.size());
            contacts = session.galFind(session.and(session.isEqualTo("cn", testContact.get("cn")),
                    session.startsWith("smtpemail1", testContact.get("smtpemail1"))), null, 100);
            assertEquals(1, contacts.size());
        }
    }

}
