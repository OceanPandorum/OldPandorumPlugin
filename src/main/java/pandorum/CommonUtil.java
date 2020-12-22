package pandorum;

import arc.files.Fi;
import arc.util.*;
import mindustry.Vars;
import mindustry.maps.Map;

import java.time.*;
import java.time.temporal.*;
import java.util.Objects;
import java.util.regex.*;

import static java.util.regex.Pattern.compile;
import static mindustry.Vars.*;

public abstract class CommonUtil{

    private CommonUtil(){}

    private static final Pattern timeUnitPattern = compile(
            "^" +
            "((\\d+)(y|year|years|г|год|года|лет))?" +
            "((\\d+)(m|mon|month|months|мес|месяц|месяца|месяцев))?" +
            "((\\d+)(w|week|weeks|н|нед|неделя|недели|недель|неделю))?" +
            "((\\d+)(d|day|days|д|день|дня|дней))?" +
            "((\\d+)(h|hour|hours|ч|час|часа|часов))?" +
            "((\\d+)(min|mins|minute|minutes|мин|минута|минуту|минуты|минут))?" +
            "((\\d+)(s|sec|secs|second|seconds|с|c|сек|секунда|секунду|секунды|секунд))?$"
    );

    public static Instant parseTime(String message){
        if(message == null) return null;
        Matcher matcher = timeUnitPattern.matcher(message.toLowerCase());
        if(!matcher.matches()) return null;
        LocalDateTime offsetDateTime = LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC);
        offsetDateTime = addUnit(offsetDateTime, ChronoUnit.YEARS, matcher.group(2));
        offsetDateTime = addUnit(offsetDateTime, ChronoUnit.MONTHS, matcher.group(5));
        offsetDateTime = addUnit(offsetDateTime, ChronoUnit.WEEKS, matcher.group(8));
        offsetDateTime = addUnit(offsetDateTime, ChronoUnit.DAYS, matcher.group(11));
        offsetDateTime = addUnit(offsetDateTime, ChronoUnit.HOURS, matcher.group(14));
        offsetDateTime = addUnit(offsetDateTime, ChronoUnit.MINUTES, matcher.group(17));
        offsetDateTime = addUnit(offsetDateTime, ChronoUnit.SECONDS, matcher.group(20));
        return Instant.now().plusSeconds(offsetDateTime.toEpochSecond(ZoneOffset.UTC));
    }

    private static <T extends Temporal> T addUnit(T instant, ChronoUnit unit, String amount){
        return Strings.canParseInt(amount) ? unit.addTo(instant, Long.parseLong(amount)) : instant;
    }

    public static Map findMap(String text){
        for(int i = 0; i < maps.all().size; i++){
            Map m = maps.all().get(i);
            if((Strings.canParseInt(text) && i == Strings.parseInt(text) - 1) || m.name().equals(text)){
                return m;
            }
        }
        return null;
    }

    public static Fi findSave(String text){
        for(int i = 0; i < saveDirectory.list().length; i++){
            Fi save = saveDirectory.list()[i];
            if((Strings.canParseInt(text) && i == Strings.parseInt(text) - 1) || Objects.equals(save.nameWithoutExtension(), text)){
                return save;
            }
        }
        return null;
    }
}