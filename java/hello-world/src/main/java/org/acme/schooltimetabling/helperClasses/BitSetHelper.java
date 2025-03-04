package org.acme.schooltimetabling.helperClasses;
import org.apache.poi.hssf.record.CFHeaderRecord;
import org.openxmlformats.schemas.officeDocument.x2006.sharedTypes.STUnsignedDecimalNumber;

import java.util.BitSet;
import java.time.LocalDateTime;
import java.time.DayOfWeek;

public class BitSetHelper {

    public static BitSet surveyBitset(String header) throws Exception{
        /*bitset for a day is broken into 30min blocks starting from
        * 7:00 AM - 10:00 PM for a total of 30 30-minute blocks per day*/
        BitSet bitset = new BitSet();
        /*check if the time is for TR or MWF; if it has a 2 then TR*/
        boolean onTR = header.contains("2");
        int mondayOffset = 0;
        int tuesdayOffset = 30;
        int wednesdayOffset = 60;
        int thursdayOffset = 90;
        int fridayOffset = 120;
        int dayOffset = 0;
        /*truncate the 2 if this is a TR time*/
        String time = onTR ? header.substring(0, header.length()-1) : header;

        dayOffset = switch (time) {
            case "7 AM" -> 0;
            case "8 AM" -> 2;
            case "9 AM" -> 4;
            case "10 AM" -> 6;
            case "11 AM" -> 8;
            case "12 PM" -> 10;
            case "1 PM" -> 12;
            case "2 PM" -> 14;
            case "3 PM" -> 16;
            case "4 PM" -> 18;
            case "5 PM" -> 20;
            case "6 PM" -> 22;
            case "7 PM" -> 24;
            case "8 PM" -> 26;
            case "9 PM" -> 28;
            default ->
                    throw new Exception(String.format("There was an error reading the time from survey with header '%s'", header));
        };
        
        /*check if the time is for TR or MWF*/
        /*Note that we set <dayOffWeek>Offset + dayOffset for first
        * 30 minutes of the hour then add one for the second 30 minutes
        * of the hour*/
        if(header.contains("2")){
            /*set Tuesday bits*/
            bitset.set(tuesdayOffset + dayOffset);
            bitset.set(tuesdayOffset + dayOffset + 1);
            /*set Thursday bits*/
            bitset.set(thursdayOffset + dayOffset);
            bitset.set(thursdayOffset + dayOffset + 1);
        }
        else{
            /*set Monday bits*/
            bitset.set(mondayOffset + dayOffset);
            bitset.set(mondayOffset + dayOffset + 1);
            /*set Wednesday bits*/
            bitset.set(wednesdayOffset + dayOffset);
            bitset.set(wednesdayOffset + dayOffset + 1);
            /*set Friday bits */
            bitset.set(fridayOffset + dayOffset);
            bitset.set(fridayOffset + dayOffset + 1);
        }

        return bitset;
    }
}
