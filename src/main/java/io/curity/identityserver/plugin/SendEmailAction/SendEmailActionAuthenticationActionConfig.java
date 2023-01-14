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

import se.curity.identityserver.sdk.config.Configuration;
import se.curity.identityserver.sdk.config.annotation.DefaultBoolean;
import se.curity.identityserver.sdk.config.annotation.Description;
import se.curity.identityserver.sdk.service.AccountManager;
import se.curity.identityserver.sdk.service.EmailSender;

import java.util.Optional;

public interface SendEmailActionAuthenticationActionConfig extends Configuration
{
    @Description("When enabled, a notification email will always be sent, regardless of the " +
            "`sendemailaction-should-send-email` parameter value")
    @DefaultBoolean(false)
    Boolean getAlwaysSendEmailNotification();

    @Description("When enabled, the user's IP address will not be sent in the notification e-mail")
    @DefaultBoolean(false)
    Boolean getDoNotSendIpAddressInEmail();

    EmailSender getEmailSender();

    Optional<AccountManager> getAccountManager();
}
