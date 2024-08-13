package de.saschaufer.apps.tally.services;

import de.saschaufer.apps.tally.config.email.EmailProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final EmailProperties emailProperties;
    private final JavaMailSender emailSender;

    public void sendRegistrationEmail(final String email, final String secret) {

        log.atInfo().setMessage("Send registration email to '{}'.").addArgument(email).log();

        try {

            final StringBuilder url = new StringBuilder();
            url.append(emailProperties.registrationUrl());
            url.append("/#/register/confirm");
            url.append("?e=").append(URLEncoder.encode(Base64.getEncoder().encodeToString(email.getBytes()), StandardCharsets.UTF_8));
            url.append("&s=").append(URLEncoder.encode(Base64.getEncoder().encodeToString(secret.getBytes()), StandardCharsets.UTF_8));

            sendEmail(emailProperties.from(), email, "Tally app registration", """
                    Hello,

                    click on the link to complete the registration on the Tally app:

                    %s

                    If you don't registered to the app, ignore this email. Since the registration is not completed then, your email address will be deleted from the Tally app system.

                    Kind regards,
                    Tally app
                    """.formatted(url));
        } catch (final Exception e) {
            log.atError().setMessage("Error sending registration email.").setCause(e).log();
        }

        log.atInfo().setMessage("Registration email sent.").log();
    }

    private void sendEmail(final String from, final String to, final String subject, final String text) {

        final SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);

        emailSender.send(message);
    }
}
