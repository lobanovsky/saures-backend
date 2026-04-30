package saures.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime

object ReadingsTable : Table("readings") {
    val id           = long("id").autoIncrement()
    val syncedAt     = datetime("synced_at").defaultExpression(CurrentDateTime)
    val objectLabel  = varchar("object_label",   255).default("")
    val objectAddress = varchar("object_address", 512).default("")
    val sensorSn     = varchar("sensor_sn",       64).default("")
    val meterSn      = varchar("meter_sn",        64).default("")
    val meterId      = integer("meter_id")
    val meterName    = varchar("meter_name",     255).default("")
    val meterType    = varchar("meter_type",     255).default("")
    val unit         = varchar("unit",            32).default("")
    val state        = varchar("state",          255).default("")
    val valuePrimary = double("value_primary")
    val valueExtra   = text("value_extra").default("")

    override val primaryKey = PrimaryKey(id)
}
