package models;

import play.data.validation.Constraints;
import play.libs.F;

/**
 * Created with IntelliJ IDEA.
 * User: diana
 * Date: 14/08/13
 * Time: 13:01
 * To change this template use File | Settings | File Templates.
 */
public class MyValidator extends Constraints.Validator<String> {


    @Override
    public boolean isValid(String s) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public F.Tuple<String, Object[]> getErrorMessageKey() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
