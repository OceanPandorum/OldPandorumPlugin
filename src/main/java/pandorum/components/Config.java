package pandorum.components;

import arc.files.Fi;
import org.hjson.*;

import static pandorum.Main.dir;

public class Config{
    public JsonObject object;

    public Config(){
        Fi fi = dir.child("config.hjson");
        try{
            object = JsonValue.readHjson(fi.readString()).asObject();
        }catch(Exception e){
            object = new JsonObject();
            object.add("hub-ip", "pandorum.su");
            object.add("hub-port", 8000);
            object.add("language", "ru_RU");
            fi.writeString(object.toString(Stringify.HJSON));
        }
    }
}
