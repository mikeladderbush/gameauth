package com.gamingroom.gameauth;

import io.dropwizard.Application;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.basic.BasicCredentialAuthFilter;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import javax.ws.rs.client.Client;

import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gamingroom.gameauth.auth.GameAuthenticator;
import com.gamingroom.gameauth.auth.GameAuthorizer;
import com.gamingroom.gameauth.auth.GameUser;

import com.gamingroom.gameauth.controller.GameUserRESTController;
import com.gamingroom.gameauth.controller.RESTClientController;
import com.gamingroom.gameauth.healthcheck.AppHealthCheck;
import com.gamingroom.gameauth.healthcheck.HealthCheckController;

public class GameAuthApplication extends Application<GameAuthConfiguration> {

	private static final Logger LOGGER = LoggerFactory.getLogger(GameAuthApplication.class);

	@Override
	public void initialize(Bootstrap<GameAuthConfiguration> b) {
	}

	@Override
	public void run(GameAuthConfiguration c, Environment e) throws Exception {

		LOGGER.info("Registering Jersey Client");
		final Client client = new JerseyClientBuilder(e)
				.using(c.getJerseyClientConfiguration())
				.build(getName());
		e.jersey().register(new RESTClientController(client));

		LOGGER.info("Registering REST resources");
		e.jersey().register(new GameUserRESTController(e.getValidator()));

		// Application health check
		e.healthChecks().register("APIHealthCheck", new AppHealthCheck(client));

		// Run multiple health checks
		e.jersey().register(new HealthCheckController(e.healthChecks()));

		// Setup Basic Security
		e.jersey().register(new AuthDynamicFeature(new BasicCredentialAuthFilter.Builder<GameUser>()
				.setAuthenticator(new GameAuthenticator())
				.setAuthorizer(new GameAuthorizer())
				.setRealm("BASIC-AUTH-REALM")
				.buildAuthFilter()));
		e.jersey().register(RolesAllowedDynamicFeature.class);
		e.jersey().register(new AuthValueFactoryProvider.Binder<>(GameUser.class));
	}

	public static void main(String[] args) throws Exception {
		new GameAuthApplication().run(args);
	}
}