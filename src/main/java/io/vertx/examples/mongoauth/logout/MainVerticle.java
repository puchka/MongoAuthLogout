package io.vertx.examples.mongoauth.logout;

import java.util.ArrayList;
import java.util.List;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.mongo.MongoAuth;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.CookieHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.UserSessionHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;

public class MainVerticle extends AbstractVerticle {
	
	private MongoClient mongo;
	private MongoAuth authProvider;

  @Override
  public void start(Future<Void> fut) {
  	JsonObject config = new JsonObject();
   
    config.put("host", "127.0.0.1");
    config.put("port", 27017);;
    config.put("db_name", "local");
    
    // Create a Mongo client
	  mongo = MongoClient.createShared(vertx, config);
	
	  JsonObject authProperties = new JsonObject();
	  authProvider = MongoAuth.create(mongo, authProperties);
	  
	  mongo.count("user", new JsonObject().put("username", "user"), count -> {
      if (count.succeeded()) {
        if (count.result() == 0) {
      	  List<String> roles = new ArrayList<String>();
      	  List<String> permissions = new ArrayList<String>();
				  roles.add("user");
				  permissions.add("user");
      	  authProvider.insertUser("user", "P@ssw0rd", roles, permissions, res -> {});
        }
      }
	  });
	  
	  Router router = Router.router(vertx);
	  
	  router.route("/").handler(ctx -> {
	  	
	  	HttpServerResponse response = ctx.response();
	  	
	  	User user = ctx.user();
	  	
    	if (user != null) {
    		
    		user.isAuthorised(MongoAuth.ROLE_PREFIX + "user", res -> {
    			if (res.succeeded()) {
	    				response
		  	        .putHeader("content-type", "text/html")
		  	        .end("<h1>You are logged in! 1</h1>");
    				} else {
    					response
	    	        .putHeader("content-type", "text/html")
	    	        .end("<h1>You are logged out! 2</h1>");
    			}
    		});
    	} else {
    		response
	        .putHeader("content-type", "text/html")
	        .end("<h1>You are logged out! 3</h1>");
    	}
    	
	  });
	  
	  router.route("/assets/*").handler(StaticHandler.create("assets"));
	  
	  router.route().handler(CookieHandler.create());
    router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));
    router.route().handler(UserSessionHandler.create(authProvider));
	  
	  router.post("/api/login").handler(new io.vertx.examples.mongoauth.logout.FormLoginHandler(authProvider));
	  
	  router.route("/logout").handler(context -> {
    	context.clearUser();
    	
    	context.response().putHeader("location", "/").setStatusCode(302).end();
    });
	  
	  vertx
	    .createHttpServer()
	    .requestHandler(router::accept)
	    .listen(8080, "127.0.0.1");
  }
  
  @Override
  public void stop() throws Exception {
    mongo.close();
  }
}
