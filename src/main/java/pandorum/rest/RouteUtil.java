package pandorum.rest;

import java.util.regex.*;

public abstract class RouteUtil{

    private static final Pattern PARAMETER_PATTERN = Pattern.compile("\\{([\\w.]+)}");

    private RouteUtil(){}

    public static String escape(String str){
        return str.replaceAll("&", "&amp;")
                  .replaceAll("<", "&lt;")
                  .replaceAll(">", "&gt;")
                  .replaceAll("\"", "&quot;")
                  .replaceAll("'", "&#x27;")
                  .replaceAll("/", "&#x2F;");
    }

    public static String expand(String template, Object... variables){
        if(variables.length == 0){
            return template;
        }
        StringBuffer buf = new StringBuffer();
        Matcher matcher = PARAMETER_PATTERN.matcher(template);
        int index = 0;
        while(matcher.find()){
            matcher.appendReplacement(buf, escape(variables[index++].toString()));
        }
        matcher.appendTail(buf);
        return buf.toString();
    }
}
