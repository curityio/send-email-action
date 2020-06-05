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

import se.curity.identityserver.sdk.attribute.Attribute;
import se.curity.identityserver.sdk.authenticationaction.completions.ActionCompletionRequestHandler;
import se.curity.identityserver.sdk.authenticationaction.completions.ActionCompletionResult;
import se.curity.identityserver.sdk.service.SessionManager;
import se.curity.identityserver.sdk.web.Request;
import se.curity.identityserver.sdk.web.Response;

import java.util.Optional;

import static io.curity.identityserver.plugin.SendEmailAction.SendEmailActionAuthenticationAction.CLIENT_IP_ATTRIBUTE;
import static io.curity.identityserver.plugin.SendEmailAction.SendEmailActionAuthenticationAction.REQUEST_DATA_IN_SESSION;
import static io.curity.identityserver.plugin.SendEmailAction.SendEmailActionAuthenticationAction.USER_AGENT_ATTRIBUTE;

public class SendEmailActionAuthenticationActionHandler implements ActionCompletionRequestHandler<Request>
{
    private final SessionManager _sessionManager;

    public SendEmailActionAuthenticationActionHandler(SendEmailActionAuthenticationActionConfig configuration)
    {
        _sessionManager = configuration.getSessionManager();
    }

    @Override
    public Optional<ActionCompletionResult> get(Request request, Response response)
    {
        _sessionManager.put(Attribute.of(CLIENT_IP_ATTRIBUTE, request.getClientIpAddress()));
        _sessionManager.put(Attribute.of(USER_AGENT_ATTRIBUTE, request.getHeaders().firstValue("User-Agent")));
        _sessionManager.put(Attribute.ofFlag(REQUEST_DATA_IN_SESSION));

        return Optional.of(ActionCompletionResult.complete());
    }

    @Override
    public Optional<ActionCompletionResult> post(Request request, Response response)
    {
        return Optional.empty();
    }

    @Override
    public Request preProcess(Request request, Response response)
    {
        return request;
    }
}
