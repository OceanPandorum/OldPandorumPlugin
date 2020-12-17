package pandorum.components;

import com.google.gson.annotations.SerializedName;

public class Config{
    @SerializedName("alert-distance")
    public int alertDistance = 300;

    @SerializedName("hub-port")
    public int hubPort = 8000;

    @SerializedName("hub-id")
    public String hubIp = "pandorum.su";

    public String locale = "ru_RU";
}
