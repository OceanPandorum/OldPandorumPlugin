package pandorum.components;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

import static pandorum.PandorumPlugin.config;

public class Bundle{

    private final ResourceBundle bundle;

    public Bundle(){
        bundle = ResourceBundle.getBundle("bundle", new Locale(config.locale));
    }

    public String get(String key){
        try{
            return bundle.getString(key);
        }catch(Throwable t){
            return "???" + key + "???";
        }
    }

    public String format(String key, Object... values){
        return MessageFormat.format(get(key), values);
    }
}
