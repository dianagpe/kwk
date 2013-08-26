package controllers;


import models.IdentityUser;
import models.User;
import play.Routes;
import play.cache.Cache;
import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.index;
import securesocial.core.Identity;
import securesocial.core.java.SecureSocial;
import views.html.prueba;

public class Application extends Controller {

    @SecureSocial.SecuredAction
    public static Result index() {
        IdentityUser user = (IdentityUser) ctx().args.get(SecureSocial.USER_KEY);
        ///return ok(index.render(user));
        return redirect(routes.MovieController.movies());

    }

    @SecureSocial.UserAwareAction
    public static Result userAware() {
        Identity user = (Identity) ctx().args.get(SecureSocial.USER_KEY);
        final String userName = user != null ? user.fullName() : "guest";
        return ok("Hello " + userName + ", you are seeing a public page");
    }

    @SecureSocial.SecuredAction( authorization = WithProvider.class, params = {"twitter"})
    public static Result onlyTwitter() {
        return ok("You are seeing this because you logged in using Twitter");
    }

//    public static Result index() {
//        return ok(index.render(Form.form(User.class))
//        );
//    }


//    public static Result authenticate() {
//        Form<User> loginform = Form.form(User.class).bindFromRequest();
//        if(loginform.hasErrors()) {
//            return badRequest(index.render(loginform));
//        } else {
//
//            session().clear();
//            User user = User.authenticate(loginform.get().email, loginform.get().password);
//
//
//            String uuid= java.util.UUID.randomUUID().toString();
//            Cache.set(uuid, user);
//            //store uuid in session for extracting the proper user from cache later
//            session("uuid",uuid);
//            session("userId", String.valueOf(user.id));
//
//            System.out.println("llegue hasta aca "+ Cache.get(uuid));
//           // session().clear();
//           // session("userId", String.valueOf(user.id));
//            //return ok(movies2.render(user));
//            return redirect(routes.MovieController.movies("unseen"));
//
//        }
//    }
//
    public static Result logout() {
        session().clear();
        return redirect("/logout");
    }


    public static Result javascriptRoutes() {
        response().setContentType("text/javascript");
        return ok(
                Routes.javascriptRouter("jsRoutes",
                        controllers.routes.javascript.MovieController.load(),
                        controllers.routes.javascript.MovieController.recommendations(),
                        controllers.routes.javascript.MovieController.rate())

        );
    }

    public static Result prueba() {
        //Identity user = (Identity) ctx().args.get(SecureSocial.USER_KEY);
        //return ok(index.render(user));
        return ok(prueba.render());

    }
}
