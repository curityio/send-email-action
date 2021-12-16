/*
 *  Copyright 2020 Curity AB
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.curity.identityserver.plugin.SendEmailAction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.curity.identityserver.sdk.Nullable;
import se.curity.identityserver.sdk.attribute.AccountAttributes;
import se.curity.identityserver.sdk.attribute.Attribute;
import se.curity.identityserver.sdk.attribute.AuthenticationAttributes;
import se.curity.identityserver.sdk.authentication.AuthenticatedSessions;
import se.curity.identityserver.sdk.authenticationaction.AuthenticationAction;
import se.curity.identityserver.sdk.authenticationaction.AuthenticationActionResult;
import se.curity.identityserver.sdk.data.email.Email;
import se.curity.identityserver.sdk.service.AccountManager;
import se.curity.identityserver.sdk.service.EmailSender;
import se.curity.identityserver.sdk.service.authenticationaction.AuthenticatorDescriptor;
import se.curity.identityserver.sdk.web.Request;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

public final class SendEmailActionAuthenticationAction implements AuthenticationAction
{
    private static final Logger _logger = LoggerFactory.getLogger(SendEmailActionAuthenticationAction.class);
    private static final String SHOULD_SEND_EMAIL_PARAM = "sendemailaction-should-send-email";

    private final boolean _alwaysSendEmail;
    private final EmailSender _emailSender;
    private final AccountManager _accountManager;
    private final DateTimeFormatter _formatter;
    private final boolean _emailShouldIncludeIpAddress;
    private final Request _request;

    public SendEmailActionAuthenticationAction(SendEmailActionAuthenticationActionConfig configuration, Request request)
    {
        _emailSender = configuration.getEmailSender();
        _alwaysSendEmail = configuration.getAlwaysSendEmailNotification();
        _accountManager = configuration.getAccountManager().orElse(null);
        _formatter = RFC_1123_DATE_TIME.withZone(ZoneId.of("GMT"));
        _emailShouldIncludeIpAddress = !configuration.getDoNotSendIpAddressInEmail();
        _request = request;
    }

    @Override
    public AuthenticationActionResult apply(AuthenticationAttributes authenticationAttributes,
                                            AuthenticatedSessions authenticatedSessions,
                                            String authenticationTransactionId,
                                            AuthenticatorDescriptor authenticatorDescriptor)
    {

        if (shouldSendEmail(authenticationAttributes))
        {
            @Nullable String recipientMail = getRecipientEmail(authenticationAttributes.getSubject());

            if (recipientMail == null)
            {
                _logger.info("No email address available for user {}, skipping sending e-mail.",
                        authenticationAttributes.getSubject());
                return AuthenticationActionResult.successfulResult(authenticationAttributes);
            }

            Email email = prepareEmail(authenticationAttributes);

            try
            {
                _emailSender.sendEmail(recipientMail, email, "sendemailaction/email/message");
            }
            catch (RuntimeException exception)
            {
                _logger.warn(
                        "Exception during sending of e-mail informing about a new login to: {}, with the following " +
                                "data. username: {}, IP: {}, User-agent: {}, time of login: {}",
                        recipientMail,
                        authenticationAttributes.getSubject(),
                        _request.getClientIpAddress(),
                        _request.getHeaders().firstValue("User-Agent"),
                        _formatter.format(Instant.ofEpochSecond(authenticationAttributes.getAuthTime())),
                        exception
                );
            }
        }

        return AuthenticationActionResult.successfulResult(authenticationAttributes);
    }

    @Nullable
    private String getRecipientEmail(String subject)
    {
        // If there is no Account Manager passed to the plugin we assume that the username is the e-mail address.
        if (_accountManager == null || _accountManager.useUsernameAsEmail())
        {
            return subject;
        }

        try
        {
            AccountAttributes userAttributes = _accountManager.getByUserName(subject);

            if (userAttributes == null)
            {
                return null;
            }

            return userAttributes.getEmails().getPrimaryOrFirst().getSignificantValue();
        }
        catch (RuntimeException exception)
        {
            _logger.warn("Could not get user data from an account manager. Username: {}", subject, exception);
        }

        return null;
    }

    private Email prepareEmail(AuthenticationAttributes attributes)
    {
        Map<String, Object> model = new HashMap<>(4);
        model.put("_username", attributes.getSubject());

        String formattedDateTime = _formatter.format(Instant.ofEpochSecond(attributes.getAuthTime()));
        model.put("_time", formattedDateTime);
        model.put("_user_agent", _request.getHeaders().firstValue("User-Agent"));

        if (_emailShouldIncludeIpAddress)
        {
            model.put("_client_ip", _request.getClientIpAddress());
        }

        return new Email(model);
    }

    private boolean shouldSendEmail(AuthenticationAttributes authenticationAttributes)
    {
        Optional<Attribute> shouldSendEmailAttr = Optional.ofNullable(
                authenticationAttributes.getSubjectAttributes().get(SHOULD_SEND_EMAIL_PARAM));

        return _alwaysSendEmail || (shouldSendEmailAttr.isPresent() && (boolean) shouldSendEmailAttr.get().getValue());
    }
}
