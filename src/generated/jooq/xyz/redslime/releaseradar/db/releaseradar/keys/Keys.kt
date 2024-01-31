/*
 * This file is generated by jOOQ.
 */
package xyz.redslime.releaseradar.db.releaseradar.keys


import org.jooq.ForeignKey
import org.jooq.UniqueKey
import org.jooq.impl.DSL
import org.jooq.impl.Internal

import xyz.redslime.releaseradar.db.releaseradar.tables.Artist
import xyz.redslime.releaseradar.db.releaseradar.tables.ArtistRadar
import xyz.redslime.releaseradar.db.releaseradar.tables.ConfigChannel
import xyz.redslime.releaseradar.db.releaseradar.tables.Info
import xyz.redslime.releaseradar.db.releaseradar.tables.PostLater
import xyz.redslime.releaseradar.db.releaseradar.tables.RadarChannel
import xyz.redslime.releaseradar.db.releaseradar.tables.Token
import xyz.redslime.releaseradar.db.releaseradar.tables.User
import xyz.redslime.releaseradar.db.releaseradar.tables.UserStat
import xyz.redslime.releaseradar.db.releaseradar.tables.records.ArtistRadarRecord
import xyz.redslime.releaseradar.db.releaseradar.tables.records.ArtistRecord
import xyz.redslime.releaseradar.db.releaseradar.tables.records.ConfigChannelRecord
import xyz.redslime.releaseradar.db.releaseradar.tables.records.InfoRecord
import xyz.redslime.releaseradar.db.releaseradar.tables.records.PostLaterRecord
import xyz.redslime.releaseradar.db.releaseradar.tables.records.RadarChannelRecord
import xyz.redslime.releaseradar.db.releaseradar.tables.records.TokenRecord
import xyz.redslime.releaseradar.db.releaseradar.tables.records.UserRecord
import xyz.redslime.releaseradar.db.releaseradar.tables.records.UserStatRecord



// -------------------------------------------------------------------------
// UNIQUE and PRIMARY KEY definitions
// -------------------------------------------------------------------------

val KEY_ARTIST_ARTIST_ID_UINDEX: UniqueKey<ArtistRecord> = Internal.createUniqueKey(Artist.ARTIST, DSL.name("KEY_artist_artist_id_uindex"), arrayOf(Artist.ARTIST.ID), true)
val KEY_ARTIST_PRIMARY: UniqueKey<ArtistRecord> = Internal.createUniqueKey(Artist.ARTIST, DSL.name("KEY_artist_PRIMARY"), arrayOf(Artist.ARTIST.ID), true)
val KEY_ARTIST_RADAR_ARTIST_RADAR_ARTIST_ID_RADAR_ID_UINDEX: UniqueKey<ArtistRadarRecord> = Internal.createUniqueKey(ArtistRadar.ARTIST_RADAR, DSL.name("KEY_artist_radar_artist_radar_artist_id_radar_id_uindex"), arrayOf(ArtistRadar.ARTIST_RADAR.ARTIST_ID, ArtistRadar.ARTIST_RADAR.RADAR_ID), true)
val KEY_ARTIST_RADAR_PRIMARY: UniqueKey<ArtistRadarRecord> = Internal.createUniqueKey(ArtistRadar.ARTIST_RADAR, DSL.name("KEY_artist_radar_PRIMARY"), arrayOf(ArtistRadar.ARTIST_RADAR.ARTIST_ID, ArtistRadar.ARTIST_RADAR.RADAR_ID), true)
val KEY_CONFIG_CHANNEL_CONFIG_CHANNEL_SERVER_ID_UINDEX: UniqueKey<ConfigChannelRecord> = Internal.createUniqueKey(ConfigChannel.CONFIG_CHANNEL, DSL.name("KEY_config_channel_config_channel_server_id_uindex"), arrayOf(ConfigChannel.CONFIG_CHANNEL.SERVER_ID), true)
val KEY_CONFIG_CHANNEL_PRIMARY: UniqueKey<ConfigChannelRecord> = Internal.createUniqueKey(ConfigChannel.CONFIG_CHANNEL, DSL.name("KEY_config_channel_PRIMARY"), arrayOf(ConfigChannel.CONFIG_CHANNEL.SERVER_ID), true)
val KEY_INFO_INFO_KEY_UINDEX: UniqueKey<InfoRecord> = Internal.createUniqueKey(Info.INFO, DSL.name("KEY_info_info_key_uindex"), arrayOf(Info.INFO.KEY), true)
val KEY_INFO_PRIMARY: UniqueKey<InfoRecord> = Internal.createUniqueKey(Info.INFO, DSL.name("KEY_info_PRIMARY"), arrayOf(Info.INFO.KEY), true)
val KEY_POST_LATER_POST_LATER_ID_UINDEX: UniqueKey<PostLaterRecord> = Internal.createUniqueKey(PostLater.POST_LATER, DSL.name("KEY_post_later_post_later_id_uindex"), arrayOf(PostLater.POST_LATER.ID), true)
val KEY_POST_LATER_PRIMARY: UniqueKey<PostLaterRecord> = Internal.createUniqueKey(PostLater.POST_LATER, DSL.name("KEY_post_later_PRIMARY"), arrayOf(PostLater.POST_LATER.ID), true)
val KEY_RADAR_CHANNEL_PRIMARY: UniqueKey<RadarChannelRecord> = Internal.createUniqueKey(RadarChannel.RADAR_CHANNEL, DSL.name("KEY_radar_channel_PRIMARY"), arrayOf(RadarChannel.RADAR_CHANNEL.ID), true)
val KEY_RADAR_CHANNEL_RADAR_CHANNEL_ID_UINDEX: UniqueKey<RadarChannelRecord> = Internal.createUniqueKey(RadarChannel.RADAR_CHANNEL, DSL.name("KEY_radar_channel_radar_channel_id_uindex"), arrayOf(RadarChannel.RADAR_CHANNEL.ID), true)
val KEY_TOKEN_PRIMARY: UniqueKey<TokenRecord> = Internal.createUniqueKey(Token.TOKEN, DSL.name("KEY_token_PRIMARY"), arrayOf(Token.TOKEN.ID), true)
val KEY_TOKEN_TOKEN_ID_UINDEX: UniqueKey<TokenRecord> = Internal.createUniqueKey(Token.TOKEN, DSL.name("KEY_token_token_id_uindex"), arrayOf(Token.TOKEN.ID), true)
val KEY_USER_PRIMARY: UniqueKey<UserRecord> = Internal.createUniqueKey(User.USER, DSL.name("KEY_user_PRIMARY"), arrayOf(User.USER.ID), true)
val KEY_USER_USER_ID_UINDEX: UniqueKey<UserRecord> = Internal.createUniqueKey(User.USER, DSL.name("KEY_user_user_id_uindex"), arrayOf(User.USER.ID), true)
val KEY_USER_STAT_PRIMARY: UniqueKey<UserStatRecord> = Internal.createUniqueKey(UserStat.USER_STAT, DSL.name("KEY_user_stat_PRIMARY"), arrayOf(UserStat.USER_STAT.USER_ID, UserStat.USER_STAT.SERVER_ID, UserStat.USER_STAT.ALBUM_ID), true)

// -------------------------------------------------------------------------
// FOREIGN KEY definitions
// -------------------------------------------------------------------------

val ARTIST_RADAR_ARTIST_ID_FK: ForeignKey<ArtistRadarRecord, ArtistRecord> = Internal.createForeignKey(ArtistRadar.ARTIST_RADAR, DSL.name("artist_radar_artist_id_fk"), arrayOf(ArtistRadar.ARTIST_RADAR.ARTIST_ID), xyz.redslime.releaseradar.db.releaseradar.keys.KEY_ARTIST_PRIMARY, arrayOf(Artist.ARTIST.ID), true)
val ARTIST_RADAR_RADAR_CHANNEL_ID_FK: ForeignKey<ArtistRadarRecord, RadarChannelRecord> = Internal.createForeignKey(ArtistRadar.ARTIST_RADAR, DSL.name("artist_radar_radar_channel_id_fk"), arrayOf(ArtistRadar.ARTIST_RADAR.RADAR_ID), xyz.redslime.releaseradar.db.releaseradar.keys.KEY_RADAR_CHANNEL_PRIMARY, arrayOf(RadarChannel.RADAR_CHANNEL.ID), true)
