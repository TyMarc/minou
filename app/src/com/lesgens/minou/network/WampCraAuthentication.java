package com.lesgens.minou.network;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.SignatureException;

import ws.wamp.jawampa.WampError;
import ws.wamp.jawampa.WampMessages.AuthenticateMessage;
import ws.wamp.jawampa.WampMessages.ChallengeMessage;
import ws.wamp.jawampa.auth.client.ClientSideAuthentication;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lesgens.minou.controllers.Controller;
import com.lesgens.minou.utils.Utils;

public class WampCraAuthentication implements ClientSideAuthentication {

	@Override
	public AuthenticateMessage handleChallenge(ChallengeMessage message,
			ObjectMapper objectMapper) {
		try {
			JsonNode challenge = message.toObjectArray(objectMapper);
			JsonNode actualObj = objectMapper.readTree(challenge.get(2).get("challenge").textValue());
			ObjectNode extra = objectMapper.valueToTree(actualObj);
			return new AuthenticateMessage(Utils.authSignature(challenge.get(2).get("challenge").textValue(), Controller.getInstance().getSecret()), extra);
		} catch (WampError e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SignatureException e) {
			e.printStackTrace();
		}

		return null;
	}

	@Override
	public String getAuthMethod() {
		return "wampcra";
	}
}