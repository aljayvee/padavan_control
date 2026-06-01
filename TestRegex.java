import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class TestRegex {
    public static void main(String[] args) {
        String html = "<!DOCTYPE html>\r\n<html>\r\n<head>\r\n<script>\r\nvar http_username = \"custom_admin\";\r\nvar http_passwd2 = \"secure_password\";\r\nvar time_zone = \"GMT-5\";\r\n</script>\r\n</head>\r\n</html>";
        String name = "http_passwd2";
        System.out.println("Result: " + extractValue(html, name));
    }

    public static String extractValue(String html, String name) {
        if (name.isEmpty()) return "";
        int searchIndex = 0;
        while (true) {
            int idx = html.toLowerCase().indexOf(name.toLowerCase(), searchIndex);
            if (idx == -1) break;
            
            System.out.println("Found " + name + " at index " + idx);
            
            // Check JS var assignment: var varname = '123'
            int startIdx = Math.max(0, idx - 10);
            String jsVarPrefix = html.substring(startIdx, idx);
            System.out.println("jsVarPrefix: [" + jsVarPrefix + "]");
            
            // Match using pattern similar to Regex("(?s).*var\\s+$", RegexOption.IGNORE_CASE)
            Pattern p = Pattern.compile("(?is).*var\\s+");
            Matcher m = p.matcher(jsVarPrefix);
            if (m.matches()) {
                System.out.println("jsVarPrefix matched var pattern!");
                int lineEnd = html.indexOf("\n", idx);
                if (lineEnd == -1) lineEnd = html.length();
                String lineStr = html.substring(idx, lineEnd);
                System.out.println("lineStr: [" + lineStr + "]");
                
                Pattern jsMatchPattern = Pattern.compile("=\\s*['\"](.*?)['\"]", Pattern.CASE_INSENSITIVE);
                Matcher jsMatch = jsMatchPattern.matcher(lineStr);
                if (jsMatch.find()) {
                    return jsMatch.group(1);
                } else {
                    System.out.println("jsMatch pattern did not match lineStr!");
                }
            } else {
                System.out.println("jsVarPrefix did not match var pattern!");
            }
            searchIndex = Math.max(searchIndex + 1, idx + name.length());
        }
        return "";
    }
}
