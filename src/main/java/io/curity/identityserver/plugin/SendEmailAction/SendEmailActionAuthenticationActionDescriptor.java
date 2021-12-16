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

import se.curity.identityserver.sdk.authenticationaction.AuthenticationAction;
import se.curity.identityserver.sdk.plugin.ManagedObject;
import se.curity.identityserver.sdk.plugin.descriptor.AuthenticationActionPluginDescriptor;

import java.util.Optional;

public final class SendEmailActionAuthenticationActionDescriptor implements AuthenticationActionPluginDescriptor<SendEmailActionAuthenticationActionConfig>
{
    @Override
    public Class<? extends AuthenticationAction> getAuthenticationAction()
    {
        return SendEmailActionAuthenticationAction.class;
    }

    @Override
    public String getPluginImplementationType()
    {
        return "sendemailaction";
    }

    @Override
    public Class<? extends SendEmailActionAuthenticationActionConfig> getConfigurationType()
    {
        return SendEmailActionAuthenticationActionConfig.class;
    }

    @Override
    public Optional<? extends ManagedObject<SendEmailActionAuthenticationActionConfig>> createManagedObject(SendEmailActionAuthenticationActionConfig configuration)
    {
        return Optional.empty();
    }
}
