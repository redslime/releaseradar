package xyz.redslime.releaseradar.util

import java.time.ZoneId

/**
 * @author redslime
 * @version 2023-05-26
 */
enum class Timezone(val friendly: String, val zone: ZoneId) {

    ASAP("As soon as possible", ZoneId.of("Etc/GMT-13")),
    MINUS11("Pacific/Samoa/GMT-11", ZoneId.of("US/Samoa")),
    MINUS10("HST/Pacific/Honolulu/Hawaii/GMT-10", ZoneId.of("Pacific/Honolulu")),
    MINUS9("AST/Pacific/Alaska/GMT-9", ZoneId.of("US/Alaska")),
    MINUS8("PST/Pacific/Los Angeles/GMT-8", ZoneId.of("America/Los_Angeles")),
    MINUS7("MST/PNT/Mountain/Phoenix/GMT-7", ZoneId.of("America/Phoenix")),
    MINUS6("CST/Central/Chicago/GMT-6", ZoneId.of("America/Chicago")),
    MINUS5("EST/Eastern/New York/Cuba/GMT-5", ZoneId.of("America/New_York")),
    MINUS4("PRT/Brazil West/Puerto Rico/GMT-4", ZoneId.of("America/Puerto_Rico")),
    MINUS3("AGT/BET/Buenos Aires/GMT-3", ZoneId.of("America/Buenos_Aires")),
    MINUS2("Atlantic/Noronha/South Georgia/GMT-2", ZoneId.of("America/Noronha")),
    MINUS1("Atlantic/Azores/Cape Verde/GMT-1", ZoneId.of("Atlantic/Azores")),
    PLUS0("UTC/GMT/London/GMT+0", ZoneId.of("Europe/London")),
    PLUS1("CET/MET/Amsterdam/Berlin/Paris/Stockholm/GMT+1", ZoneId.of("Europe/Berlin")),
    PLUS2("ART/CAT/EET/Istanbul/Sofia/Helsinki/Tallinn/GMT+2", ZoneId.of("Europe/Istanbul")),
    PLUS3("EAT/Djibouti/Nairobi/Kuwait/GMT+3", ZoneId.of("Africa/Djibouti")),
    PLUS4("NET/Moscow/Mauritius/Dubai/GMT+4", ZoneId.of("Europe/Moscow")),
    PLUS5("PLT/Oral/Maldives/GMT+5", ZoneId.of("Asia/Oral")),
    PLUS6("BST/Dhaka/Chagos/GMT+6", ZoneId.of("Asia/Dhaka")),
    PLUS7("VST/Jakarta/Bangkok/Saigon/GMT+7", ZoneId.of("Asia/Jakarta")),
    PLUS8("CTT/PRC/Hongkong/Singapore/Shanghai/Perth/GMT+8", ZoneId.of("Asia/Hong_Kong")),
    PLUS9("JST/ROK/Japan/Seoul/GMT+9", ZoneId.of("Asia/Seoul")),
    PLUS10("AET/Syndney/Guam/GMT+10", ZoneId.of("Australia/Sydney")),
    PLUS11("SST/Pacific/Noumea/Efate/GMT+11", ZoneId.of("Pacific/Noumea")),
    PLUS12("NST/NZ/Auckland/Fiji/GMT+12", ZoneId.of("Pacific/Auckland"))

}