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

import io.ricall.greenmail.greenmaildemo.service.EmailService.Email.EmailBuilder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamSource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.mail.MessagingException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender sender;

    public static EmailBuilder emailTo(String emailAddress) {
        return Email.builder()
                .to(emailAddress);
    }

    private String[] asArray(List<String> values) {
        return values.toArray(String[]::new);
    }

    public void send(Email email) {
        if (email.isSimpleMessage()) {
            sendSimpleMessage(email);
        } else {
            sendMimeMessage(email);
        }
    }

    private void sendSimpleMessage(Email email) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(asArray(email.toList));
        message.setFrom(email.from);
        message.setBcc(asArray(email.bccList));
        message.setCc(asArray(email.ccList));
        message.setReplyTo(email.replyTo);
        message.setSubject(email.subject);
        message.setText(email.text);

        sender.send(message);
    }

    private void sendMimeMessage(Email email) {
        sender.send(mimeMessage -> {
            MimeMessageHelper message = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            message.setFrom(email.from);
            message.setTo(asArray(email.toList));
            message.setBcc(asArray(email.bccList));
            message.setCc(asArray(email.ccList));
            message.setReplyTo(email.replyTo);
            message.setSubject(email.subject);
            message.setText(email.html, true);
            email.inlineParts.forEach((attachmentFilename, inputStreamSource) -> {
                try {
                    message.addAttachment(attachmentFilename, inputStreamSource);
                } catch (MessagingException e) {
                    throw new EmailSendException("Failed to add inline attachment", e);
                }
            });
            email.attachmentParts.forEach((attachmentFilename, inputStreamSource) -> {
                try {
                    message.addAttachment(attachmentFilename, inputStreamSource);
                } catch (MessagingException e) {
                    throw new EmailSendException("Failed to add attachment", e);
                }
            });
        });
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Email {

        private final List<String> toList;
        private final List<String> ccList;
        private final List<String> bccList;
        @Builder.Default
        private final String from = "test@test.com";
        private final String replyTo;
        private final String subject;
        private final String text;
        private final String html;
        private final Map<String, InputStreamSource> inlineParts;
        private final Map<String, InputStreamSource> attachmentParts;

        public boolean isSimpleMessage() {
            return html == null && inlineParts.isEmpty() && attachmentParts.isEmpty();
        }

        public static class EmailBuilder {
            private List<String> toList = new ArrayList<>();
            private List<String> bccList = new ArrayList<>();
            private List<String> ccList = new ArrayList<>();
            private Map<String, InputStreamSource> inlineParts = new LinkedHashMap<>();
            private Map<String, InputStreamSource> attachmentParts = new LinkedHashMap<>();

            public EmailBuilder to(String emailAddress) {
                toList.add(emailAddress);
                return this;
            }

            public EmailBuilder bcc(String emailAddress) {
                bccList.add(emailAddress);
                return this;
            }

            public EmailBuilder cc(String emailAddress) {
                ccList.add(emailAddress);
                return this;
            }

            public EmailBuilder inline(String name, InputStreamSource inputStreamSource) {
                inlineParts.put(name, inputStreamSource);
                return this;
            }

            public EmailBuilder attachment(String name, InputStreamSource inputStreamSource) {
                inlineParts.put(name, inputStreamSource);
                return this;
            }

        }

    }

}
