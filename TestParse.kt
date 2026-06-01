import java.util.regex.Pattern

fun main() {
    val html = "var netdevs = { 'eth2':{rx:0x00,tx:0x00}, 'eth3':{rx:0x87d60f4e,tx:0x34cc1a08,rx_bytes:0x687d60f4e,tx_bytes:0x34cc1a08} };"
    val pattern = Pattern.compile("(?:var\\s+)?netdevs\\s*=\\s*(\\{.*?\\});", Pattern.DOTALL)
    val matcher = pattern.matcher(html)
    if (matcher.find()) {
        var jsonStr = matcher.group(1) ?: "{}"
        println("Found: " + jsonStr)
        jsonStr = jsonStr.replace(Regex(":\\s*(0x[0-9a-fA-F]+)"), ": "\"")
        println("Replaced: " + jsonStr)
    } else {
        println("Not found")
    }
}
