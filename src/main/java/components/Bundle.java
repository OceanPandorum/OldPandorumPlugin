package components;

import java.io.*;
import java.util.Properties;

public class Bundle {

    public static String get(String nameStr) {
        String out = "TRANSLATE_ERROR("+nameStr+")";
        FileInputStream fileInputStream;
        Properties prop = new Properties();
        try {
            fileInputStream = new FileInputStream("config/mods/Pandorum/bundles/"+ Config.get("language")+".properties");
            prop.load(fileInputStream);
            if (prop.getProperty(nameStr) != null) {
                out = prop.getProperty(nameStr);
                out = new String(out.getBytes("ISO-8859-1"), "UTF-8");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return out;
    }
}
