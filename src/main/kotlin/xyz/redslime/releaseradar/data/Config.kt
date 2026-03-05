package xyz.redslime.releaseradar.data

data class Config(val discordToken: String, val logChannel: Long?, val adminGuild: Long?, val spotifyClientId: String,
      val spotifySecret: String, val dbDriverName: String, val dbHost: String, val dbUser: String,
      val dbPassword: String, val redirectUrl: String, val serverPort: Int, val spotifyFallbacks: Map<String, String>,
      val saveEmojiGuildId: Long?, val saveEmojiId: Long?)
