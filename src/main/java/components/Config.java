package components;

import arc.util.Log;

import java.io.*;
import java.util.Properties;

public class Config {

    public static void main() {

        // Создаём папку если не существует
        final File dir1 = new File("config/mods/Pandorum");
        if (!dir1.exists()) {
            dir1.mkdir();
        }


        File file1 = new File("config/mods/Pandorum/config.properties");
        if (!file1.exists()) {
            Log.warn("The config file was successfully generated.");

            try (InputStream in = Config.class
                    .getClassLoader()
                    .getResourceAsStream("config.properties");
                 OutputStream out = new FileOutputStream("config/mods/Pandorum/config.properties")) {
                int data;
                while ((data = in.read()) != -1) {
                    out.write(data);
                }
            } catch (IOException exc) {
                exc.printStackTrace();
            }
        }

        final File dir2 = new File("config/mods/Pandorum/bundles");
        if (!dir2.exists()) {
            dir2.mkdir();
        }

        File file2 = new File("config/mods/Pandorum/bundles/bundle_RU.properties");
        if(!file2.exists()) {
            try (InputStream in = Config.class
                    .getClassLoader()
                    .getResourceAsStream("bundles/bundle_RU.properties");
                 OutputStream out = new FileOutputStream("config/mods/Pandorum/bundles/bundle_RU.properties")) {
                int data;
                while ((data = in.read()) != -1) {
                    out.write(data);
                }
            } catch (IOException exc) {
                exc.printStackTrace();
            }
        }
    }

    public static String get(String nameStr) {
        String out = "CONFIG_ERROR";
        FileInputStream fileInputStream;
        Properties prop = new Properties();
        try {
            fileInputStream = new FileInputStream("config/mods/Pandorum/config.properties");
            prop.load(fileInputStream);
            out = prop.getProperty(nameStr);
            out = new String(out.getBytes("ISO-8859-1"), "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return out;
    }
}
