package components;

import mindustry.gen.Call;

public class Broadcast {
    public static void bc(String[] arg) {
        String[] args = arg[0].split(" \n ");

        StringBuilder newStr = new StringBuilder();
        for(String data: args){
            newStr.append(data).append(" ");
        }

        Call.onInfoMessage("\uE805"+(Bundle.get("bc.txt"))+"\uE805"+ "\n" + "\n" + newStr + "\n");
    }

}
