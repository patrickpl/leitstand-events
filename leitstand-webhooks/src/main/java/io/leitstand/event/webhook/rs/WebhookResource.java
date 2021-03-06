/*
 * (c) RtBrick, Inc - All rights reserved, 2015 - 2019
 */
package io.leitstand.event.webhook.rs;

import static io.leitstand.commons.model.ObjectUtil.isDifferent;
import static io.leitstand.commons.model.Patterns.UUID_PATTERN;
import static io.leitstand.commons.rs.ReasonCode.VAL0003E_IMMUTABLE_ATTRIBUTE;
import static io.leitstand.commons.rs.Responses.created;
import static io.leitstand.commons.rs.Responses.success;
import static io.leitstand.event.webhook.service.MessageFilter.newMessageFilter;
import static io.leitstand.event.webhook.service.MessageState.messageState;
import static io.leitstand.security.auth.Role.ADMINISTRATOR;
import static io.leitstand.security.auth.Role.SYSTEM;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import io.leitstand.commons.UnprocessableEntityException;
import io.leitstand.commons.messages.Messages;
import io.leitstand.event.queue.service.DomainEventId;
import io.leitstand.event.webhook.service.MessageFilter;
import io.leitstand.event.webhook.service.WebhookId;
import io.leitstand.event.webhook.service.WebhookMessage;
import io.leitstand.event.webhook.service.WebhookMessages;
import io.leitstand.event.webhook.service.WebhookName;
import io.leitstand.event.webhook.service.WebhookService;
import io.leitstand.event.webhook.service.WebhookSettings;
import io.leitstand.event.webhook.service.WebhookStatistics;
import io.leitstand.event.webhook.service.WebhookTemplate;

@RequestScoped
@Path("/webhooks")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
public class WebhookResource {

	@Inject
	private WebhookService service;
	
	@Inject
	private Messages messages;
	
	@GET
	@Path("/{hook_id:"+UUID_PATTERN+"}")
	@RolesAllowed({ADMINISTRATOR,SYSTEM})
	public WebhookSettings getWebhookSettings(@PathParam("hook_id") WebhookId hookId) {
		return service.getWebhook(hookId);
	}
	
	@GET
	@Path("/{hook_name}")
	@RolesAllowed({ADMINISTRATOR,SYSTEM})
	public WebhookSettings getWebhookSettings(@Valid @PathParam("hook_name") WebhookName hookName) {
		return service.getWebhook(hookName);
	}
	
	@PUT
	@Path("/{hook_id:"+UUID_PATTERN+"}")
	@RolesAllowed({ADMINISTRATOR,SYSTEM})
	public Response storeWebhookSettings(@Valid @PathParam("hook_id") WebhookId hookId,
										 @Valid WebhookSettings settings) {
		if(isDifferent(hookId, settings.getWebhookId())) {
			throw new UnprocessableEntityException(VAL0003E_IMMUTABLE_ATTRIBUTE, 
					   								"hook_id", 
					   								hookId, 
					   								settings.getWebhookId());
		}
		
		if(service.storeWebhook(settings)) {
			return created(messages, hookId);
		}
		return success(messages);
	}
	
	@POST
	@RolesAllowed({ADMINISTRATOR,SYSTEM})
	public Response storeWebhookSettings(@Valid WebhookSettings settings) {
		
		if(service.storeWebhook(settings)) {
			return created(messages,settings.getWebhookId());
		}
		return success(messages);	
	}
	
	@GET
	@Path("/{hook_name}/messages/{event:"+UUID_PATTERN+"}")
	@RolesAllowed({ADMINISTRATOR,SYSTEM})
	public WebhookMessage getMessage(@Valid @PathParam("hook_name") WebhookName hookName, 
									 @Valid @PathParam("event") DomainEventId eventId) {
		return service.getMessage(hookName, eventId);
	}
	
	@GET
	@Path("/{hook_id:"+UUID_PATTERN+"}/messages/{event:"+UUID_PATTERN+"}")
	@RolesAllowed({ADMINISTRATOR,SYSTEM})
	public WebhookMessage getMessage(@Valid @PathParam("hook_id") WebhookId hookId, 
									 @Valid @PathParam("event") DomainEventId eventId) {
		return service.getMessage(hookId,
								  eventId);
	}
	
	@GET
	@Path("/{hook_name}/messages")
	@RolesAllowed({ADMINISTRATOR,SYSTEM})
	public WebhookMessages findMessages(@Valid @PathParam("hook_name") WebhookName hookName,
										@QueryParam("state") String state,
										@QueryParam("correlationId") String correlationId,
										@QueryParam("offset") int offset,
										@QueryParam("size") int size){

		MessageFilter filter = newMessageFilter()
							   .withMessageState(messageState(state))
							   .withCorrelationId(correlationId)
							   .withOffset(offset)
							   .withLimit(size)
							   .build();
		
		return service.findMessages(hookName,filter);
	}
	
	@GET
	@Path("/{hook_id:"+UUID_PATTERN+"}/messages")
	@RolesAllowed({ADMINISTRATOR,SYSTEM})
	public WebhookMessages findMessages(@Valid @PathParam("hook_id") WebhookId hookId,
										@QueryParam("correlationId") String correlationId,
										@QueryParam("state") String state,
									   	@QueryParam("offset") int offset,
									   	@QueryParam("size") int size){
		
		MessageFilter filter = newMessageFilter()
							   .withMessageState(messageState(state))
							   .withCorrelationId(correlationId)
							   .withOffset(offset)
							   .withLimit(size)
							   .build();

		return service.findMessages(hookId,filter);
	}
	
	@POST
	@Path("/{hook_name}/messages/{event:"+UUID_PATTERN+"}/_retry")
	@RolesAllowed({ADMINISTRATOR,SYSTEM})
	public Response retryMessage(@Valid @PathParam("hook_name") WebhookName hookName, 
								 @Valid @PathParam("event") DomainEventId eventId) {
		service.getMessage(hookName, eventId);
		return success(messages);
	}
	
	@POST
	@Path("/{hook_id:"+UUID_PATTERN+"}/messages/{event:"+UUID_PATTERN+"}/_retry")
	@RolesAllowed({ADMINISTRATOR,SYSTEM})
	public Response retryMessage(@Valid @PathParam("hook_id") WebhookId hookId, 
								 @Valid @PathParam("event") DomainEventId eventId) {
		service.getMessage(hookId, eventId);
		return success(messages);
	}
	
	@POST
	@Path("/{hook_name}/_reset")
	@RolesAllowed({ADMINISTRATOR,SYSTEM})
	public Response resetWebhook(@Valid @PathParam("hook_name") WebhookName hookName, 
								 @Valid @QueryParam("event_id") DomainEventId eventId) {
		service.resetWebhook(hookName,
							 eventId);
		return success(messages);
	}
	
	@POST
	@Path("/{hook_id:"+UUID_PATTERN+"}/_reset")
	@RolesAllowed({ADMINISTRATOR,SYSTEM})
	public Response resetWebhook(@Valid @PathParam("hook_id") WebhookId hookId, 
		    					 @Valid @QueryParam("event_id") DomainEventId eventId) {
		service.resetWebhook(hookId,
						   	 eventId);
		return success(messages);
	}
	
	@POST
	@Path("/{hook_name}/_retry")
	@RolesAllowed({ADMINISTRATOR,SYSTEM})
	public Response retryFailedCalls(@Valid @PathParam("hook_name") WebhookName hookName) {
		service.retryWebhook(hookName);
		return success(messages);
	}
	
	@POST
	@Path("/{hook_id:"+UUID_PATTERN+"}/_retry")
	@RolesAllowed({ADMINISTRATOR,SYSTEM})
	public Response retryFailedCalls(@Valid @PathParam("hook_id") WebhookId hookId) {
		service.retryWebhook(hookId);
		return success(messages);
	}
	
	
	@POST
	@Path("/{hook_name}/_disable")
	@RolesAllowed({ADMINISTRATOR,SYSTEM})
	public Response disableWebhook(@Valid @PathParam("hook_name") WebhookName hookName) {
		service.disableWebhook(hookName);
		return success(messages);
	}
	
	@POST
	@Path("/{hook_id:"+UUID_PATTERN+"}/_disable")
	@RolesAllowed({ADMINISTRATOR,SYSTEM})
	public Response disableWebhook(@Valid @PathParam("hook_id") WebhookId hookId) {
		service.disableWebhook(hookId);
		return success(messages);
	}
	
	@POST
	@Path("/{hook_name}/_enable")
	@RolesAllowed({ADMINISTRATOR,SYSTEM})
	public Response enableWebhook(@Valid @PathParam("hook_name") WebhookName hookName) {
		service.enableWebhook(hookName);
		return success(messages);
	}
	
	@POST
	@Path("/{hook_id:"+UUID_PATTERN+"}/_enable")
	@RolesAllowed({ADMINISTRATOR,SYSTEM})
	public Response enableWebhook(@Valid @PathParam("hook_id") WebhookId hookId) {
		service.enableWebhook(hookId);
		return success(messages);
	}
	
	@DELETE
	@Path("/{hook_id:"+UUID_PATTERN+"}")
	@RolesAllowed({ADMINISTRATOR,SYSTEM})
	public Response removeWebhook(@Valid @PathParam("hook_id") WebhookId hookId) {
		service.removeWebhook(hookId);
		return success(messages);
	}
	
	@DELETE
	@Path("/{hook_name}")
	@RolesAllowed({ADMINISTRATOR,SYSTEM})
	public Response removeWebhook(@Valid @PathParam("hook_name") WebhookName hookName) {
		service.removeWebhook(hookName);
		return success(messages);
	}
	
	
	@PUT
	@Path("/{hook_id:"+UUID_PATTERN+"}/template")
	@RolesAllowed({ADMINISTRATOR,SYSTEM})
	public Response storeWebhookTemplate(@Valid @PathParam("hook_id") WebhookId hookId,
										 @Valid WebhookTemplate settings) {
		if(isDifferent(hookId, settings.getWebhookId())) {
			throw new UnprocessableEntityException(VAL0003E_IMMUTABLE_ATTRIBUTE, 
					   								"hook_id", 
					   								hookId, 
					   								settings.getWebhookId());
		}
		
		service.storeWebhookTemplate(settings);
		return success(messages);
	}
	
	@PUT
	@Path("/{hook_name}/template")
	@RolesAllowed({ADMINISTRATOR,SYSTEM})
	public Response storeWebhookTemplate(@Valid @PathParam("hook_name") WebhookName hookName,
										 @Valid WebhookTemplate settings) {
		
		if(isDifferent(hookName, settings.getWebhookName())) {
			throw new UnprocessableEntityException(VAL0003E_IMMUTABLE_ATTRIBUTE, 
					   								"hook_name", 
					   								hookName, 
					   								settings.getWebhookName());
		}
		
		service.storeWebhookTemplate(settings);
		return success(messages);
		
	}
	
	@GET
	@Path("/{hook_id:"+UUID_PATTERN+"}/template")
	@RolesAllowed({ADMINISTRATOR,SYSTEM})
	public WebhookTemplate getWebhookTemplate(@PathParam("hook_id") WebhookId hookId) {
		return service.getWebhookTemplate(hookId);
	}
	
	@GET
	@Path("/{hook_name}/template")
	@RolesAllowed({ADMINISTRATOR,SYSTEM})
	public WebhookTemplate getWebhookTemplate(@Valid @PathParam("hook_name") WebhookName hookName) {
		return service.getWebhookTemplate(hookName);
	}
	
	@GET
	@Path("/{hook_id:"+UUID_PATTERN+"}/statistics")
	@RolesAllowed({ADMINISTRATOR,SYSTEM})
	public WebhookStatistics getWebhookStatistics(@PathParam("hook_id") WebhookId hookId) {
		return service.getWebhookStatistics(hookId);
	}
	
	@GET
	@Path("/{hook_name}/statistics")
	@RolesAllowed({ADMINISTRATOR,SYSTEM})
	public WebhookStatistics getWebhookStatistics(@Valid @PathParam("hook_name") WebhookName hookName) {
		return service.getWebhookStatistics(hookName);
	}
}
