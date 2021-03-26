/*
 * Copyright (c) 2021 Richard Allwood
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.ricall.greenmail.greenmaildemo.service;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

import javax.mail.BodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.SharedByteArrayInputStream;

import static com.icegreen.greenmail.configuration.GreenMailConfiguration.aConfig;
import static com.icegreen.greenmail.util.GreenMailUtil.getAddressList;
import static com.icegreen.greenmail.util.GreenMailUtil.getBody;
import static io.ricall.greenmail.greenmaildemo.service.EmailService.emailTo;
import static javax.mail.Message.RecipientType.TO;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
public class TestEmailService {

    @Autowired
    EmailService service;

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP)
            .withConfiguration(aConfig().withUser("user", "pass"))
            .withPerMethodLifecycle(false);

    @BeforeEach
    public void init() throws Exception {
        greenMail.purgeEmailFromAllMailboxes();
    }


    @Test
    public void verifyWeCanSendASimpleMessage() throws Exception {
        service.send(emailTo("TEST USER <test.user@company.com.au>")
                .from("JOHN SENDER <john.sender@company.com.au>")
                .replyTo("REPLY TO <reply.to@company.com.au>")
                .subject("Test Email")
                .text(String.join("\r\n",
                        "First Line",
                        "Second Line"
                ))
                .build());

        final MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
        assertThat(receivedMessages).hasSize(1);

        final MimeMessage message = receivedMessages[0];
        assertThat(getAddressList(message.getRecipients(TO))).isEqualTo("TEST USER <test.user@company.com.au>");
        assertThat(getAddressList(message.getFrom())).isEqualTo("JOHN SENDER <john.sender@company.com.au>");
        assertThat(getAddressList(message.getReplyTo())).isEqualTo("REPLY TO <reply.to@company.com.au>");
        assertThat(message.getSubject()).isEqualTo("Test Email");
        assertThat(getBody(message)).isEqualTo("First Line\r\nSecond Line");
    }

    @Test
    public void verifyWeCanSendAHtmlMessage() throws Exception {
        service.send(emailTo("TEST USER <test.user@company.com.au>")
                .from("JOHN SENDER <john.sender@company.com.au>")
                .replyTo("REPLY TO <reply.to@company.com.au>")
                .subject("Test Email")
                .html("<b>Message Body</b>")
                .attachment("application.yml", new ClassPathResource("/application.yml"))
                .build());

        final MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
        assertThat(receivedMessages).hasSize(1);

        final MimeMessage message = receivedMessages[0];
        assertThat(getAddressList(message.getRecipients(TO))).isEqualTo("TEST USER <test.user@company.com.au>");
        assertThat(getAddressList(message.getFrom())).isEqualTo("JOHN SENDER <john.sender@company.com.au>");
        assertThat(getAddressList(message.getReplyTo())).isEqualTo("REPLY TO <reply.to@company.com.au>");
        assertThat(message.getSubject()).isEqualTo("Test Email");

        assertThat(message.getContent()).isInstanceOf(MimeMultipart.class);
        MimeMultipart multipart = (MimeMultipart) message.getContent();
        assertThat(multipart.getCount()).isEqualTo(2);

        BodyPart body = getBodyPart(multipart, "multipart/related");
        assertThat(body).isNotNull();

        MimeMultipart related = (MimeMultipart) body.getContent();
        BodyPart text = getBodyPart(related, "text/html");
        assertThat(text.getContent()).isEqualTo("<b>Message Body</b>");

        BodyPart attachment = getBodyPart(multipart, "application/octet-stream");
        assertThat(attachment).isNotNull();
        SharedByteArrayInputStream is = (SharedByteArrayInputStream) attachment.getContent();
        String fileText = new String(is.readAllBytes());
        assertThat(fileText).contains("password: pass");
    }

    private BodyPart getBodyPart(MimeMultipart multipart, String mimeType) throws Exception {
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart part = multipart.getBodyPart(i);
            if (part.getContentType().startsWith(mimeType)) {
                return part;
            }
        }
        return null;
    }

}
