package com.banno.twittersampleendpoint.domain

final case class AppConfig(server: ServerConfig, twitterCredentials: TwitterCredentials)

final case class TwitterCredentials(
                               consumerKey: String,
                               consumerSecret: String,
                               userKey: String,
                               userSecret: String
                             )

final case class ServerConfig(ip: String, port: Int)