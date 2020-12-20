package pandorum.components;

import arc.util.*;

import java.net.InetAddress;
import java.util.Objects;

public class IpInfo{
    private final int mask;
    private final InetAddress required;

    public IpInfo(String ip){
        String address = ip;
        if(address.indexOf('/') > 0){
            String[] addressAndMask = address.split("/");
            address = addressAndMask[0];
            mask = Strings.parseInt(addressAndMask[1]);
        }else{
            mask = -1;
        }

        required = parseAddress(address);
        if(!(required.getAddress().length * 8 >= mask)){
            throw new IllegalArgumentException(String.format("IP address %s is too short for bitmask of length %d", address, mask));
        }
    }

    public boolean matchIp(String ip){
        InetAddress remoteAddress = parseAddress(ip);

        if(required.getClass() != remoteAddress.getClass()){
            return false;
        }

        if(mask < 0){
            return Objects.equals(remoteAddress, required);
        }

        byte[] rem = remoteAddress.getAddress();
        byte[] req = required.getAddress();
        int fullMask = mask / 8;
        byte b = (byte)(0xFF00 >> (mask & 0x07));
        for(int i = 0; i < fullMask; i++){
            if(rem[i] != req[i]) return false;
        }

        return b == 0 || (rem[fullMask] & b) == (req[fullMask] & b);
    }

    private InetAddress parseAddress(String ip){
        try{
            return InetAddress.getByName(ip);
        }catch(Throwable t){
            throw new ArcRuntimeException(t);
        }
    }
}
