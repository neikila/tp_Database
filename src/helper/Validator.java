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
        if ((String)user.get("name") == null) {
            user.remove("name");
            user.put("name", "");
        }
        if ((String)user.get("about") == null) {
            user.remove("about");
            user.put("about", "");
        }
        if ((String)user.get("username") == null) {
            user.remove("username");
            user.put("username", "");
        }
        return true;
    }
}
