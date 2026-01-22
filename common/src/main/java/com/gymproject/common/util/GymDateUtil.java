package com.gymproject.common.util;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

import static com.gymproject.common.constant.GymTimePolicy.SERVICE_ZONE;

public class GymDateUtil {

    // 1.  LocalDate + LocalTime -> 호주 시간 OFfsetDateTime
    public static OffsetDateTime convert(LocalDate date, LocalTime time) {
        if(date == null || time == null) {
            return null;
        }

        return date.atTime(time)
                .atZone(SERVICE_ZONE)
                .toOffsetDateTime();
    }

    // 2. LocalDate -> 호주시간(시간은 00:00:00)
    public static OffsetDateTime convertStartOfDay(LocalDate date){
        if(date == null) {return null;}
        return date.atStartOfDay(SERVICE_ZONE)
                .toOffsetDateTime();
    }

    // 3. LocalDate -> 호주 시간(23:59:59)
    public static OffsetDateTime convertEndOfDay(LocalDate date){
        if(date == null) {return null;}
        return date.atTime(LocalTime.MAX)
                .atZone(SERVICE_ZONE)
                .toOffsetDateTime();
    }

    // 4. 현재 호주 시간
    public static OffsetDateTime now(){
        return OffsetDateTime.now(SERVICE_ZONE);
    }

}


