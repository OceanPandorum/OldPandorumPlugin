package components;

import arc.struct.Array;
import mindustry.entities.type.Player;

public class Nick  implements Runnable{
    private static int update = 1000;

    private static int colorOffset = 0;

    private static String[] colors = new String[11];

    static {
        colors[0] = "[#ff0000]";
        colors[1] = "[#ff7f00]";
        colors[2] = "[#ffff00]";
        colors[3] = "[#7fff00]";
        colors[4] = "[#00ff00]";
        colors[5] = "[#00ff7f]";
        colors[6] = "[#00ffff]";
        colors[7] = "[#007fff]";
        colors[8] = "[#0000ff]";
        colors[9] = "[#8000ff]";
        colors[10] = "[#ff00ff]";
    }

    public Array<Player> targets = new Array();

    public Nick() {
        (new Thread(this)).start();
    }

    public void run() {
        Thread.currentThread().setName("Animated nickname thread");
        while (!Thread.currentThread().isInterrupted()) {
            for (Player player : this.targets) {
                if (player.con.hasConnected)
                    animated(player);
            }
            try {
                Thread.sleep(update);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void animated(Player player) {
        String name = player.name.replaceAll("\\[(.*?)]", "");
        StringBuilder stringBuilder = new StringBuilder();
        String[] newNick = new String[name.length()];
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            int colorIndex = (i + colorOffset) % colors.length;
            if (colorIndex < 0)
                colorIndex += colors.length;
            String newText = colors[colorIndex] + c;
            newNick[i] = newText;
        }
        colorOffset--;
        for (String s : newNick)
            stringBuilder.append(s);
        player.name = stringBuilder.toString();
    }
}
