package org.acme.schooltimetabling.DefaultTimes;

import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

public class DefaultTimeRegistry {
    private static final List<Supplier<? extends DefaultTime>> defaultTimes = List.of(
            DefaultTimeAll::getInstance
//            , DefaultTimeMorning::getInstance
//            , DefaultTimeEvening::getInstance
    );
    private static final Random RANDOM = new Random();

    /**
     * get a random default time. Then you can get the preferences, acceptable, and conflict times for this
     * default time schedule
     * @return Default time schedule allowing you to copy bitsets
     */
    public static DefaultTime getRandomDefault(){
        int size = defaultTimes.size();
        int choice = RANDOM.nextInt(size);
        return defaultTimes.get(choice).get();
    }
}
