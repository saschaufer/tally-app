package de.saschaufer.tallyapp.services;

import de.saschaufer.tallyapp.config.email.EmailProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EmailServiceTest {

    private JavaMailSender emailSenderMock;
    private EmailService emailService;

    @BeforeEach
    void beforeEach() {
        emailSenderMock = mock(JavaMailSender.class);
        final EmailProperties emailProperties = new EmailProperties("from@mail.com", "http://url-register", Duration.ofSeconds(1));
        emailService = new EmailService(emailProperties, emailSenderMock);
    }

    @Test
    void sendRegistrationEmail_positive() {

        assertDoesNotThrow(() -> emailService.sendRegistrationEmail("to@mail.com", "secret"));

        final ArgumentCaptor<SimpleMailMessage> argumentCaptorEmail = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(emailSenderMock, times(1)).send(argumentCaptorEmail.capture());

        assertThat(argumentCaptorEmail.getValue().getFrom(), is("from@mail.com"));
        assertThat(argumentCaptorEmail.getValue().getTo(), notNullValue());
        assertThat(argumentCaptorEmail.getValue().getTo().length, is(1));
        assertThat(argumentCaptorEmail.getValue().getTo()[0], is("to@mail.com"));
        assertThat(argumentCaptorEmail.getValue().getSubject(), is("Tally App registration"));
        assertThat(argumentCaptorEmail.getValue().getText(), containsString("http://url-register/#/register/confirm?e=dG9AbWFpbC5jb20%3D&s=c2VjcmV0"));
    }

    @Test
    void sendRegistrationEmail_negative() {

        doThrow(new RuntimeException("Bad")).when(emailSenderMock).send(any(SimpleMailMessage.class));

        final ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> emailService.sendRegistrationEmail("to@mail.com", "secret"));

        assertThat(e.getStatusCode(), is(HttpStatus.INTERNAL_SERVER_ERROR));
        assertThat(e.getMessage(), containsString("Error sending registration email"));

        final ArgumentCaptor<SimpleMailMessage> argumentCaptorEmail = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(emailSenderMock, times(1)).send(argumentCaptorEmail.capture());

        assertThat(argumentCaptorEmail.getValue().getFrom(), is("from@mail.com"));
        assertThat(argumentCaptorEmail.getValue().getTo(), notNullValue());
        assertThat(argumentCaptorEmail.getValue().getTo().length, is(1));
        assertThat(argumentCaptorEmail.getValue().getTo()[0], is("to@mail.com"));
        assertThat(argumentCaptorEmail.getValue().getSubject(), is("Tally App registration"));
        assertThat(argumentCaptorEmail.getValue().getText(), containsString("http://url-register/#/register/confirm?e=dG9AbWFpbC5jb20%3D&s=c2VjcmV0"));
    }

    @Test
    void sendResetPasswordEmail_positive() {

        assertDoesNotThrow(() -> emailService.sendResetPasswordEmail("to@mail.com", "abc123"));

        final ArgumentCaptor<SimpleMailMessage> argumentCaptorEmail = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(emailSenderMock, times(1)).send(argumentCaptorEmail.capture());

        assertThat(argumentCaptorEmail.getValue().getFrom(), is("from@mail.com"));
        assertThat(argumentCaptorEmail.getValue().getTo(), notNullValue());
        assertThat(argumentCaptorEmail.getValue().getTo().length, is(1));
        assertThat(argumentCaptorEmail.getValue().getTo()[0], is("to@mail.com"));
        assertThat(argumentCaptorEmail.getValue().getSubject(), is("Tally App reset password"));
        assertThat(argumentCaptorEmail.getValue().getText(), containsString("abc123"));
    }

    @Test
    void sendResetPasswordEmail_negative() {

        doThrow(new RuntimeException("Bad")).when(emailSenderMock).send(any(SimpleMailMessage.class));

        final ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> emailService.sendResetPasswordEmail("to@mail.com", "abc123"));

        assertThat(e.getStatusCode(), is(HttpStatus.INTERNAL_SERVER_ERROR));
        assertThat(e.getMessage(), containsString("Error sending reset password email"));

        final ArgumentCaptor<SimpleMailMessage> argumentCaptorEmail = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(emailSenderMock, times(1)).send(argumentCaptorEmail.capture());

        assertThat(argumentCaptorEmail.getValue().getFrom(), is("from@mail.com"));
        assertThat(argumentCaptorEmail.getValue().getTo(), notNullValue());
        assertThat(argumentCaptorEmail.getValue().getTo().length, is(1));
        assertThat(argumentCaptorEmail.getValue().getTo()[0], is("to@mail.com"));
        assertThat(argumentCaptorEmail.getValue().getSubject(), is("Tally App reset password"));
        assertThat(argumentCaptorEmail.getValue().getText(), containsString("abc123"));
    }
}
