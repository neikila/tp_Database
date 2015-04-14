package helper;

import org.json.simple.JSONObject;

/**
 * Created by neikila on 13.04.15.
 */
public class Validator {
    public static boolean userValidation(JSONObject user) {
        if ((String)user.get("email") == null) {
            return false;
        }
        return true;
    }
}
