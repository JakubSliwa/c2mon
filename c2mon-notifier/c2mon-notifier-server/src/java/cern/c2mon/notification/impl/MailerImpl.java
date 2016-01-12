/******************************************************************************
 * Copyright (C) 2010-2016 CERN. All rights not expressly granted are reserved.
 * 
 * This file is part of the CERN Control and Monitoring Platform 'C2MON'.
 * C2MON is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the license.
 * 
 * C2MON is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for
 * more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with C2MON. If not, see <http://www.gnu.org/licenses/>.
 *****************************************************************************/
package cern.c2mon.notification.impl;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.net.InetAddress;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.Message.RecipientType;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cern.c2mon.notification.Mailer;

/**
 * An implementation of the {@link Mailer} interface. You can create a own one or use the
 * {@link #fromPropertiesFile(String)} to have a {@link MailerImpl} created from a properties file. In order to use
 * latter, please check {@link #fromPropertiesFile(String)}.
 * 
 * @author felixehm
 * @see #fromPropertiesFile(String)
 */
public class MailerImpl implements Mailer {

    /**
     * our logger
     */
    private Logger logger = LoggerFactory.getLogger(MailerImpl.class);

    /**
     * For speed reasons we initialize it only once.
     */
    private InternetAddress me = new InternetAddress();

    private Session session = null;

    private String replyTo;

    /**
     * Creates a MailerImpl from a properties file: 
     * <ul>
     * <li>mailer.from</li>
     * <li>mailer.name</li>
     * <li>mailer.password</li>
     * <li>mailer.server</li>
     * <li>mailer.port</li>
     * </ul>
     * 
     * @param fileName the properties-conform file 
     * @return a new MailerImpl object
     * @throws Exception in case of an error
     */
    public static MailerImpl fromPropertiesFile(String fileName) throws Exception {
        Properties properties = new Properties();
        BufferedInputStream stream = new BufferedInputStream(new FileInputStream(fileName));
        properties.load(stream);
        stream.close();

        return new MailerImpl(properties.getProperty("mailer.from"), properties.getProperty("mailer.name"), properties
                .getProperty("mailer.password"), properties.getProperty("mailer.server"), Integer.parseInt(properties
                .getProperty("mailer.port")),properties.getProperty("mailer.replyTo"));

    }

    /**
     * @param from the sender email address
     * @param name the username for authorizing at the mail server.
     * @param password the password for authorizing at the mail server.
     * @param server the mailserver hostname
     * @param port the mailserver port
     * @throws AddressException in case this {@link MailerImpl} instance cannot create an {@link InetAddress} object
     *             (required).
     */
    public MailerImpl(String from, final String name, final String password, String server, int port, String replyTo)
            throws AddressException {
        Properties props = System.getProperties();

        props.put("mail.smtp.host", server);
        props.put("mail.smtp.port", Integer.valueOf(port));
        props.put("mail.smtp.from", from);
        // props.put("mail.smtp.auth", "true");

        props.put("mail.transport.protocol", "smtp");

        // props.put("mail.smtp.starttls.enable", "true");

        // props.put("mail.debug", "true");

        me = new InternetAddress(from);
        this.replyTo = replyTo;

        // get the session / connection to the mailserver
        session = Session.getInstance(props, new Authenticator() {
            @Override
            public PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(name, password);
            }
        });

        logger.info("Started Mailer. FROM={}, SERVER={}:{}", from , server , port);
    }

    /**
     * Constructor. Provides access to a remote mail server on port 25.
     * 
     * @param from the sender email address
     * @param name the username for authorizing at the mail server.
     * @param password the password for authorizing at the mail server.
     * @param server the mailserver hostname
     * @throws AddressException in case this {@link MailerImpl} instance cannot create an {@link InetAddress} object
     *             (required).
     */
    public MailerImpl(final String from, final String name, final String password, final String server)
            throws AddressException {
        this(from, name, password, server, 25, null);
    }

    /**
     * Threadsafe call to send a mail.
     * 
     * @param to the user mail address. Cannot be null.
     * @param subject the subject of the mail. If null it will be set to an empty string.
     * @param mailText the mail text.
     * @throws MessagingException in case the message cannot be sent
     * @throws IllegalArgumentException in case the passed user mail is null.
     */
    @Override
    public synchronized void sendEmail(String to, String subject, String mailText) throws MessagingException,
            IllegalArgumentException {
        if (to == null) {
            throw new IllegalArgumentException("Passed argument for recipient is null.");
        }

        if (subject == null) {
            subject = "";
        }

        if (mailText == null) {
            mailText = "";
        }
        
        MimeMessage simpleMessage = new MimeMessage(session);

        // MimeMultipart content = new MimeMultipart();
        // MimeBodyPart html = new MimeBodyPart();
        // html.setContent(mailText, "text/html");
        // content.addBodyPart(html);

        simpleMessage.setContent(mailText, "text/html");
        //
        simpleMessage.setFrom(me);
        simpleMessage.setRecipient(RecipientType.TO, new InternetAddress(to));
        simpleMessage.setSubject(subject);
        if (replyTo != null) {
            simpleMessage.setReplyTo(new InternetAddress[] {new InternetAddress(replyTo)});
        }
        // simpleMessage.setText(mailText);

        Transport.send(simpleMessage);
    }
}
