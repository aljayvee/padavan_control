import java.net.URL
import java.net.HttpURLConnection
import java.util.Base64
import java.io.BufferedReader
import java.io.InputStreamReader

fun main() {
    try {
        val url = URL("http://192.168.2.2/apcli_scan.asp")
        val conn = url.openConnection() as HttpURLConnection
        val auth = "Jayvee:admin1234"
        val encodedAuth = Base64.getEncoder().encodeToString(auth.toByteArray())
        conn.setRequestProperty("Authorization", "Basic $encodedAuth")
        conn.requestMethod = "GET"
        
        val reader = BufferedReader(InputStreamReader(conn.inputStream))
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            if (line!!.contains("site_survey") || line!!.contains("apcli_scan") || line!!.contains("networks")) {
                println(line)
            }
        }
        reader.close()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
