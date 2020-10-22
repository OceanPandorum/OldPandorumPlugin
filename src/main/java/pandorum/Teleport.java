package pandorum;

public class Teleport{
    public final int x, y;

    public Teleport(int x, int y){
        this.x = x;
        this.y = y;
    }

    public boolean valid(int x, int y){
        return x <= this.x + 3 && x >= this.x && y >= this.y && y <= this.y + 3;
    }
}
