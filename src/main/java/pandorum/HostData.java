package pandorum;

import com.google.gson.annotations.SerializedName;

public class HostData{
    public String ip = "pandorum.su";
    public int port;

    @SerializedName("teleport-x")
    public int teleportX;
    @SerializedName("teleport-y")
    public int teleportY;

    public String title = "title";
    @SerializedName("title-x")
    public int titleX;
    @SerializedName("title-y")
    public int titleY;

    @SerializedName("label-x")
    public int labelX;
    @SerializedName("label-y")
    public int labelY;

    public HostData(int port, int teleportX, int teleportY, int titleX, int titleY, int labelX, int labelY){
        this.port = port;
        this.teleportX = teleportX;
        this.teleportY = teleportY;
        this.titleX = titleX;
        this.titleY = titleY;
        this.labelX = labelX;
        this.labelY = labelY;
    }

    public boolean teleport(int x, int y){
        return x <= this.teleportX + 3 && x >= this.teleportX &&
               y >= this.teleportY && y <= this.teleportY + 3;
    }
}
