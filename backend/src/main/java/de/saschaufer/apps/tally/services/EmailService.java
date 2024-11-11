package de.saschaufer.apps.tally.services;

import de.saschaufer.apps.tally.config.email.EmailProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error sending registration email");
        }

        log.atInfo().setMessage("Registration email sent.").log();
    }

    public void sendResetPasswordEmail(final String email, final String password) {

        log.atInfo().setMessage("Send reset password email to '{}'.").addArgument(email).log();

        try {

            sendEmail(emailProperties.from(), email, "Tally app reset password", """
                    Hello,
                    
                    the password for the Tally app has been changed. Log in with the new password and change it.
                    
                    New password: %s
                    
                    Kind regards,
                    Tally app
                    """.formatted(password));
        } catch (final Exception e) {
            log.atError().setMessage("Error sending reset password email.").setCause(e).log();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error sending reset password email");
        }

        log.atInfo().setMessage("Reset password email sent.").log();
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
