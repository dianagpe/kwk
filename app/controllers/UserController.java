package controllers;

import models.User;
import play.data.Form;
import play.mvc.Controller;

public class UserController extends Controller {

    static Form<User> userForm = Form.form(User.class);

//    public static Result index() {
//        return ok(index.render(userForm)
//        );
//    }

//    public static Result login() {
//        Form<User> filledForm = userForm.bindFromRequest();
//        if(filledForm.hasErrors()) {
//            return badRequest(index.render(userForm));
//        } else {
//            User user = userForm.get();
//            return ok("Got user " + user);
//        }
//    }


}
