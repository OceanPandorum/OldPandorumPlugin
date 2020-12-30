package pandorum;

import com.google.gson.reflect.TypeToken;
import pandorum.rest.*;

import java.lang.reflect.Type;
import java.util.*;

public class ActionService{

    public static final Type array = new TypeToken<List<AdminAction>>(){}.getType();

    public static final Type single = new TypeToken<AdminAction>(){}.getType();

    private final Router router;

    public ActionService(Router router){
        this.router = router;
    }

    public AdminAction getActions(AdminActionType type, String targetId){
        return Routes.ACTION_GET.newRequest(type, targetId)
                .exchange(router)
                .bodyTo(single);
    }

    public List<AdminAction> getAllActions(AdminActionType type){
        return Routes.ACTIONS_GET.newRequest(type)
                .exchange(router)
                .defaultValue(Collections.emptyList())
                .bodyTo(array);
    }

    public void delete(AdminActionType type, String targetId){
        Routes.ACTION_DELETE.newRequest(type, targetId)
                .exchange(router);
    }

    public AdminAction save(AdminAction adminAction){
        return Routes.ACTION_POST.newRequest()
                .header("Content-type", "application/json")
                .body(adminAction)
                .exchange(router)
                .bodyTo(single);
    }
}
