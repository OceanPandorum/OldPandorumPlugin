package pandorum;

public class HostData{
    public String ip = "pandorum.su";
    public int port;
    public int size = 3;

    public int teleportX;
    public int teleportY;

    public String title = "title";
    public int titleX;
    public int titleY;

    public int labelX;
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

    public boolean inDiapason(int x, int y){
        return x <= teleportX + size && x >= teleportX &&
               y >= teleportY && y <= teleportY + size;
    }
}
