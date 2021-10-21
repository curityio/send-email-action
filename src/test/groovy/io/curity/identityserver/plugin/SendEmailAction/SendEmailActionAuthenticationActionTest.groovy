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

package io.curity.identityserver.plugin.SendEmailAction

import se.curity.identityserver.sdk.attribute.AccountAttributes
import se.curity.identityserver.sdk.attribute.AuthenticationAttributes
import se.curity.identityserver.sdk.attribute.ContextAttributes
import se.curity.identityserver.sdk.attribute.SubjectAttributes
import se.curity.identityserver.sdk.authentication.AuthenticatedSessions
import se.curity.identityserver.sdk.authenticationaction.AuthenticationActionResult
import se.curity.identityserver.sdk.service.AccountManager
import se.curity.identityserver.sdk.service.EmailSender
import se.curity.identityserver.sdk.service.authenticationaction.AuthenticatorDescriptor
import se.curity.identityserver.sdk.web.Request
import spock.lang.Specification


class SendEmailActionAuthenticationActionTest extends Specification
{
    def authenticationAttributes = AuthenticationAttributes.of("subject", ContextAttributes.empty())
    def authenticatedSessions = Mock(AuthenticatedSessions)
    def authenticationTransactionId = "someID"
    def authenticatorDescriptor = Mock(AuthenticatorDescriptor)

    def "should send email to subject, when no accountManager present"()
    {
        given:
        authenticationAttributes = AuthenticationAttributes.of("michal@curity.io", ContextAttributes.empty())
        def request = getRequestData()
        def emailSender = Mock(EmailSender)
        def configuration = new TestConfiguration(
                emailSender,
                Optional.empty()
        )

        def action = new SendEmailActionAuthenticationAction(configuration, request)

        when:
        def result = action.apply(authenticationAttributes, authenticatedSessions, authenticationTransactionId, authenticatorDescriptor)

        then:
        result instanceof AuthenticationActionResult.SuccessAuthenticationActionResult
        1 * emailSender.sendEmail("michal@curity.io", _, _)
    }

    def "should send email to subject if accountManager uses emails as logins"()
    {
        given:
        authenticationAttributes = AuthenticationAttributes.of("michal@curity.io", ContextAttributes.empty())
        def request = getRequestData()
        def emailSender = Mock(EmailSender)
        def accountManager = Mock(AccountManager)
        accountManager.useUsernameAsEmail() >> true

        def configuration = new TestConfiguration(
                emailSender,
                Optional.of(accountManager)
        )

        def action = new SendEmailActionAuthenticationAction(configuration, request)

        when:
        def result = action.apply(authenticationAttributes, authenticatedSessions, authenticationTransactionId, authenticatorDescriptor)

        then:
        result instanceof AuthenticationActionResult.SuccessAuthenticationActionResult
        1 * emailSender.sendEmail("michal@curity.io", _, _)
    }

    def "should get email from accountManager and send email"()
    {
        given:
        authenticationAttributes = AuthenticationAttributes.of("michal", ContextAttributes.empty())
        def request = getRequestData()
        def emailSender = Mock(EmailSender)
        def accountManager = Mock(AccountManager)
        accountManager.useUsernameAsEmail() >> false
        accountManager.getByUserName("michal") >> AccountAttributes.of("michal", "pass", "michal@curity.io")

        def configuration = new TestConfiguration(
                emailSender,
                Optional.of(accountManager)
        )

        def action = new SendEmailActionAuthenticationAction(configuration, request)

        when:
        def result = action.apply(authenticationAttributes, authenticatedSessions, authenticationTransactionId, authenticatorDescriptor)

        then:
        result instanceof AuthenticationActionResult.SuccessAuthenticationActionResult
        1 * emailSender.sendEmail("michal@curity.io", _, _)
    }

    def "should handle error from accountManager"()
    {
        given:
        authenticationAttributes = AuthenticationAttributes.of("michal", ContextAttributes.empty())
        def request = getRequestData()
        def emailSender = Mock(EmailSender)
        def accountManager = Mock(AccountManager)
        accountManager.useUsernameAsEmail() >> false
        accountManager.getByUserName("michal") >> { throw new RuntimeException("Something went wrong") }

        def configuration = new TestConfiguration(
                emailSender,
                Optional.of(accountManager)
        )

        def action = new SendEmailActionAuthenticationAction(configuration, request)

        when:
        def result = action.apply(authenticationAttributes, authenticatedSessions, authenticationTransactionId, authenticatorDescriptor)

        then:
        noExceptionThrown()
        result instanceof AuthenticationActionResult.SuccessAuthenticationActionResult
        0 * emailSender.sendEmail(_, _, _)
    }

    def "should handle error from emailSender"()
    {
        given:
        authenticationAttributes = AuthenticationAttributes.of("michal@curity.io", ContextAttributes.empty())
        def request = getRequestData()
        def emailSender = Mock(EmailSender)
        emailSender("michal@curity.io", _, _) >> { throw new RuntimeException("Couldn't send e-mail") }

        def configuration = new TestConfiguration(
                emailSender,
                Optional.empty()
        )

        def action = new SendEmailActionAuthenticationAction(configuration, request)

        when:
        def result = action.apply(authenticationAttributes, authenticatedSessions, authenticationTransactionId, authenticatorDescriptor)

        then:
        noExceptionThrown()
        result instanceof AuthenticationActionResult.SuccessAuthenticationActionResult
    }

    def "should send email when `always send` set to false and `shouldSendEmail` attribute present"()
    {
        given:
        authenticationAttributes = AuthenticationAttributes.of(
                SubjectAttributes.of(["subject": "michal@curity.io", "sendemailaction-should-send-email": true]),
                ContextAttributes.empty())
        def request = getRequestData()
        def emailSender = Mock(EmailSender)
        def configuration = new TestConfiguration(
                emailSender,
                Optional.empty(),
                false
        )

        def action = new SendEmailActionAuthenticationAction(configuration, request)

        when:
        def result = action.apply(authenticationAttributes, authenticatedSessions, authenticationTransactionId, authenticatorDescriptor)

        then:
        result instanceof AuthenticationActionResult.SuccessAuthenticationActionResult
        1 * emailSender.sendEmail("michal@curity.io", _, _)
    }

    def "should not include ip in e-mail when `doNotSendIpAddressInEmail` setting on"()
    {
        given:
        authenticationAttributes = AuthenticationAttributes.of("michal@curity.io", ContextAttributes.empty())
        def request = getRequestData()
        def emailSender = Mock(EmailSender)
        def configuration = new TestConfiguration(
                emailSender,
                Optional.empty(),
                true,
                true
        )

        def action = new SendEmailActionAuthenticationAction(configuration, request)

        when:
        def result = action.apply(authenticationAttributes, authenticatedSessions, authenticationTransactionId, authenticatorDescriptor)

        then:
        result instanceof AuthenticationActionResult.SuccessAuthenticationActionResult
        1 * emailSender.sendEmail("michal@curity.io", { it -> it.getModel().get("_client_ip") == null }, _)
    }

    private class TestConfiguration implements SendEmailActionAuthenticationActionConfig
    {
        def _emailSender
        def _accountManager
        def _shouldAlwaysSendEmail
        def _doNotSendIpAddress

        TestConfiguration(def emailSender, def accountManager, def shouldAlwaysSendEmail = true, def doNotSendIpAddress = false)
        {
            _emailSender = emailSender
            _accountManager = accountManager
            _shouldAlwaysSendEmail = shouldAlwaysSendEmail
            _doNotSendIpAddress = doNotSendIpAddress
        }

        @Override
        Boolean getAlwaysSendEmailNotification()
        {
            return _shouldAlwaysSendEmail
        }

        @Override
        EmailSender getEmailSender()
        {
            return _emailSender
        }

        @Override
        Optional<AccountManager> getAccountManager()
        {
            return _accountManager
        }

        @Override
        Boolean getDoNotSendIpAddressInEmail()
        {
            return _doNotSendIpAddress
        }

        @Override
        String id()
        {
            return "TestConfiguration"
        }
    }

    private def getRequestData()
    {
        def request = Stub(Request)
        request.getClientIpAddress() >> "127.0.0.2"
        request.getHeaders().firstValue("User-Agent") >> "(spock tests)"
        request
    }
}
