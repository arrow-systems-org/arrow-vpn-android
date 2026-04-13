package org.arrowx.vpn.domain.model

data class ServerNode(
    val id: String,
    val name: String,
    val countryCode: String,
    val vlessConfig: String
)
