package pandorum;

import com.google.gson.annotations.SerializedName;

import java.time.Instant;
import java.util.Objects;

public class AdminAction{
    private String id;

    @SerializedName("admin_id")
    private String adminId;

    @SerializedName("target_id")
    private String targetId;

    private AdminActionType type;

    private String reason;

    private Instant timestamp;

    @SerializedName("end_timestamp")
    private Instant endTimestamp;

    public String id(){
        return id;
    }

    public void id(String id){
        this.id = id;
    }

    public String adminId(){
        return adminId;
    }

    public void adminId(String adminId){
        this.adminId = adminId;
    }

    public String targetId(){
        return targetId;
    }

    public void targetId(String targetId){
        this.targetId = targetId;
    }

    public AdminActionType type(){
        return type;
    }

    public void type(AdminActionType type){
        this.type = type;
    }

    public String reason(){
        return reason;
    }

    public void reason(String reason){
        this.reason = reason;
    }

    public Instant timestamp(){
        return timestamp;
    }

    public void timestamp(Instant timestamp){
        this.timestamp = timestamp;
    }

    public Instant endTimestamp(){
        return endTimestamp;
    }

    public void endTimestamp(Instant endTimestamp){
        this.endTimestamp = endTimestamp;
    }

    @Override
    public boolean equals(Object o){
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        AdminAction that = (AdminAction)o;
        return Objects.equals(id, that.id) &&
               Objects.equals(adminId, that.adminId) &&
               Objects.equals(targetId, that.targetId) &&
               type == that.type &&
               Objects.equals(reason, that.reason) &&
               Objects.equals(timestamp, that.timestamp) &&
               Objects.equals(endTimestamp, that.endTimestamp);
    }

    @Override
    public int hashCode(){
        return Objects.hash(id, adminId, targetId, type, reason, timestamp, endTimestamp);
    }
}
