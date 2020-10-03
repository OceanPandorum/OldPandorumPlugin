package components;

import arc.util.Strings;

import java.util.Locale;
import java.util.ResourceBundle;

import static pandorum.Main.config;

public class Bundle{
    private final ResourceBundle bundle;

    public Bundle(){
        bundle = ResourceBundle.getBundle("bundle", new Locale(config.object.getString("language", "ru_RU")));
    }

    public String get(String key) {
        try{
            return bundle.getString(key);
        }catch(Exception e){
            return "???" + key + "???";
        }
    }

    public String format(String key, Object... values){
        return Strings.format(get(key), values);
    }
}
