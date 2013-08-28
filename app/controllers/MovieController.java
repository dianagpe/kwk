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
import views.html.movieEdit;

import java.util.List;
import java.util.Map;

public class MovieController extends Controller{

    public static Result index(){

        return TODO;
    }

    static final int ITEMS_LIMIT_DEFAULT = 30;

    @SecureSocial.SecuredAction
    public static Result search(){

        Form<Search> searchForm = Form.form(Search.class).bindFromRequest();
        Search s = searchForm.get();

        IdentityUser user = (IdentityUser) ctx().args.get(SecureSocial.USER_KEY);
        List<Movie> movies =  Movie.list(Movie.Set.ALL, user.id, 0, ITEMS_LIMIT_DEFAULT, s.q, 1);

//        return ok(search.render(user, searchForm, movies));
        return ok(views.html.temporal.render(user, searchForm, movies));
    }

    @SecureSocial.SecuredAction
    public static Result movies(){

        IdentityUser user = (IdentityUser) ctx().args.get(SecureSocial.USER_KEY);
        return ok(views.html.movies.render(new IdentityUser(), Form.form(Search.class).bindFromRequest(), Movie.topRated(), Movie.topRated(), Movie.inTeathers()));
    }

    @BodyParser.Of(BodyParser.Json.class)
    @SecureSocial.SecuredAction(ajaxCall = true)
    public static Result load(String search, Integer offset){

        IdentityUser user = (IdentityUser) ctx().args.get(SecureSocial.USER_KEY);
//        IdentityUser user = new IdentityUser();
//        user.id = 1;
//        Movie.Set set = Movie.Set.getById(session("set"));
        return ok(Json.toJson(Movie.list(Movie.Set.ALL, user.id, offset, ITEMS_LIMIT_DEFAULT, search,  1)));
    }

//    public static Result temporal(String search){
//
//        IdentityUser user = new IdentityUser();
//        user.id = 1;
//
//        Movie.Set set = Movie.Set.getById(session("set"));
//        return ok(temporal.render(Movie.list(set, user.id, 0, 100, search, 1)));
//    }

  //  @BodyParser.Of(BodyParser.Json.class)
   // @SecureSocial.SecuredAction(ajaxCall = true)
  @SecureSocial.SecuredAction
  public static Result recommendations(){

        IdentityUser user = (IdentityUser) ctx().args.get(SecureSocial.USER_KEY);
        Map<Integer, IdentityUser> users = null;
        List<Movie> movies = null;

        try{

            users = User.similarity(user);
            movies = Movie.recommendations(user, users);

        }catch (Exception ex){

        }
        return ok(views.html.temporal.render(user, Form.form(Search.class).bindFromRequest(), movies));

        // return ok(Json.toJson(movies));
    }

    @SecureSocial.SecuredAction(ajaxCall = true)
    public static Result rate(Integer movieId, Float rating){

        //IdentityUser user = (IdentityUser) ctx().args.get(SecureSocial.USER_KEY);
        IdentityUser user = new IdentityUser();
        user.id = 1;

        Movie.rate(user.id, movieId, rating);
        return ok("baby");
    }

    public static Result editMovie(Integer id){

        Form<Movie> movieForm = Form.form(Movie.class).bindFromRequest();
        Movie movie = Movie.get(id);
        movieForm = movieForm.fill(movie);

        return ok(movieEdit.render(movieForm));
    }

    public static Result setMovie(){
        try{
            Form<Movie> movieForm = Form.form(Movie.class).bindFromRequest();
            Movie movie = movieForm.get();
            Movie.save(movie);
        }catch (Exception ex){
            ex.printStackTrace();
        }

        return redirect(routes.MovieController.movies());
//        return redirect(routes.MovieController.temporal("google"));

    }

}
