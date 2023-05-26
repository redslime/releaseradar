package xyz.redslime.releaseradar.util

import java.time.ZoneId

/**
 * @author redslime
 * @version 2023-05-26
 */
enum class Timezone(val friendly: String, val zone: ZoneId) {

    ASAP("As soon as possible", ZoneId.of("Etc/GMT-13")),
    MINUS11("Pacific/Samoa/GMT-11", ZoneId.of("Etc/GMT+11")),
    MINUS10("HST/Pacific/Honolulu/Hawaii/GMT-10", ZoneId.of("Etc/GMT+10")),
    MINUS9("AST/Pacific/Alaska/GMT-9", ZoneId.of("Etc/GMT+9")),
    MINUS8("PST/Pacific/Los Angeles/GMT-8", ZoneId.of("Etc/GMT+8")),
    MINUS7("MST/PNT/Mountain/Phoenix/GMT-7", ZoneId.of("Etc/GMT+7")),
    MINUS6("CST/Central/Chicago/GMT-6", ZoneId.of("Etc/GMT+6")),
    MINUS5("EST/Eastern/New York/Cuba/GMT-5", ZoneId.of("Etc/GMT+5")),
    MINUS4("PRT/Brazil West/Puerto Rico/GMT-4", ZoneId.of("Etc/GMT+4")),
    MINUS3("AGT/BET/Buenos Aires/GMT-3", ZoneId.of("Etc/GMT+3")),
    MINUS2("Atlantic/Noronha/South Georgia/GMT-2", ZoneId.of("Etc/GMT+2")),
    MINUS1("Atlantic/Azores/Cape Verde/GMT-1", ZoneId.of("Etc/GMT+1")),
    PLUS0("UTC/GMT/London/GMT+0", ZoneId.of("Etc/GMT+0")),
    PLUS1("CET/MET/Amsterdam/Berlin/Paris/Stockholm/GMT+1", ZoneId.of("Etc/GMT-1")),
    PLUS2("ART/CAT/EET/Istanbul/Sofia/Helsinki/Tallinn/GMT+2", ZoneId.of("Etc/GMT-2")),
    PLUS3("EAT/Djibouti/Nairobi/Kuwait/GMT+3", ZoneId.of("Etc/GMT-3")),
    PLUS4("NET/Moscow/Mauritius/Dubai/GMT+4", ZoneId.of("Etc/GMT-4")),
    PLUS5("PLT/Oral/Maldives/GMT+5", ZoneId.of("Etc/GMT-5")),
    PLUS6("BST/Dhaka/Chagos/GMT+6", ZoneId.of("Etc/GMT-6")),
    PLUS7("VST/Jakarta/Bangkok/Saigon/GMT+7", ZoneId.of("Etc/GMT-7")),
    PLUS8("CTT/PRC/Hongkong/Singapore/Shanghai/Perth/GMT+8", ZoneId.of("Etc/GMT-8")),
    PLUS9("JST/ROK/Japan/Seoul/GMT+9", ZoneId.of("Etc/GMT-9")),
    PLUS10("AET/Syndney/Guam/GMT+10", ZoneId.of("Etc/GMT-10")),
    PLUS11("SST/Pacific/Noumea/Efate/GMT+11", ZoneId.of("Etc/GMT-11")),
    PLUS12("NST/NZ/Auckland/Fiji/GMT+12", ZoneId.of("Etc/GMT-12"))

}