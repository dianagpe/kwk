package controllers;

import models.IdentityUser;
import models.Movie;
import models.Search;
import models.User;
import play.data.Form;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import securesocial.core.java.SecureSocial;

import java.util.List;
import java.util.Map;

public class MovieController extends Controller{

    public static Result index(){

        return TODO;
    }

    //@SecureSocial.SecuredAction
    public static Result find(){
        Form<Search> searchForm = Form.form(Search.class).bindFromRequest();
        session("search",searchForm.get().q);
        return redirect(routes.MovieController.movies(session("set")));
    }

    //@SecureSocial.SecuredAction
    public static Result movies(String set){

        //IdentityUser user = (IdentityUser) ctx().args.get(SecureSocial.USER_KEY);
        IdentityUser user = new IdentityUser();
        user.id = 1;

        session("set", Movie.Set.getById(set).id);
        Form<Search> searchForm = Form.form(Search.class).bindFromRequest();
        searchForm = searchForm.fill(new Search(session("search")));

        return ok(views.html.movies2.render(new IdentityUser(), searchForm));
    }

    @BodyParser.Of(BodyParser.Json.class)
    //@SecureSocial.SecuredAction(ajaxCall = true)
    public static Result load(Integer offset){

        //IdentityUser user = (IdentityUser) ctx().args.get(SecureSocial.USER_KEY);
        IdentityUser user = new IdentityUser();
        user.id = 1;

        Movie.Set set = Movie.Set.getById(session("set"));
        return ok(Json.toJson(Movie.list(set, user.id, 0, 12, session("search"),  1)));
    }

    //@BodyParser.Of(BodyParser.Json.class)
    public static Result recommendations(){

        String userId = session("userId");
        Map<Long, User> users = null;
        List<Movie> movies = null;

        try{
            User user = new User();
            user.id = Long.parseLong(userId);
            users = User.similarity(user);
            movies = Movie.recommendations(user, users);

        }catch (Exception ex){

        }

        return ok(Json.toJson(movies));
    }

    @SecureSocial.SecuredAction(ajaxCall = true)
    public static Result rate(Integer movieId, Float rating){

        //IdentityUser user = (IdentityUser) ctx().args.get(SecureSocial.USER_KEY);
        IdentityUser user = new IdentityUser();
        user.id = 1;

        Movie.rate(user.id, movieId, rating);
        return ok("baby");
    }

//    public static Result newMovie(){
//        Form<Movie> filledForm = movieForm.bindFromRequest();
//        if(filledForm.hasErrors()){
//            return badRequest(views.html.movie.render(filledForm));
//        } else {
//            return redirect(routes.MovieController.movies());
//        }
//
//
//    }

}
