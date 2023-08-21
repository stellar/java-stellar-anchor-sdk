package org.stellar.reference.dao

import org.jetbrains.exposed.sql.Table

object Quotes : Table() {
  val id = varchar("id", 255).entityId()
  val price = varchar("price", 255).nullable()
  val totalPrice = varchar("total_price", 255).nullable()
  val expiresAt = long("expires_at").nullable()
  val createdAt = long("created_at").nullable()
  val sellAsset = varchar("sell_asset", 255).nullable()
  val sellAmount = varchar("sell_amount", 255).nullable()
  val sellDeliveryMethod = varchar("sell_delivery_method", 255).nullable()
  val buyAsset = varchar("buy_asset", 255).nullable()
  val buyAmount = varchar("buy_amount", 255).nullable()
  val buyDeliveryMethod = varchar("buy_delivery_method", 255).nullable()
  val countryCode = varchar("country_code", 255).nullable()
  val clientId = varchar("client_id", 255).nullable()
  val transactionId = varchar("transaction_id", 255).nullable()
  val fee = varchar("fee", 255).nullable()
}
