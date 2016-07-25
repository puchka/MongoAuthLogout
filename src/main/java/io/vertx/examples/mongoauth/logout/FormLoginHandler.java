package io.vertx.examples.mongoauth.logout;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.mongo.MongoAuth;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;

public class FormLoginHandler implements Handler<RoutingContext> {
	private MongoAuth authProvider;
	
	public FormLoginHandler(MongoAuth authProvider) {
		this.authProvider = authProvider;
	}
	
	@Override
	public void handle(RoutingContext routingContext) {
	    Session session = routingContext.session();

	    routingContext.request().bodyHandler(new Handler<Buffer>() {
	        public void handle(Buffer buf)
	        {
	        	String authInfoText = buf.getString(0, buf.length());
	        	
	        	JsonObject authInfo = new JsonObject();
		    	authInfo.put("username", authInfoText.split("&")[0].split("=")[1]);
		    	try {
					authInfo.put("password", URLDecoder.decode(authInfoText.split("&")[1].split("=")[1], "UTF-8"));
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	            
			    authProvider.authenticate(authInfo, res -> {
			      if (res.succeeded()) {
			      	User user = res.result();
			      	System.out.println("User " + user.principal() + " is now authenticated");
			      	routingContext.setUser(user);
				    	
			        routingContext.setSession(session);
			        routingContext.response().putHeader("location", "/").setStatusCode(302).end();
			      } else {
			      	routingContext.response().putHeader("location", "/assets/403.html").setStatusCode(302).end();
			      }
		    	});
	        };
	    });
	}
}
