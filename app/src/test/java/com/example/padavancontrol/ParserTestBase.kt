package com.example.padavancontrol

open class ParserTestBase {
    /**
     * Helper to wrap a raw JS variable script segment inside standard HTML tags
     * mimicking real router ASP served responses.
     */
    fun getMockHtml(javascriptContent: String): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
            <meta http-equiv="Content-Type" content="text/html; charset=utf-8">
            <script>
            $javascriptContent
            </script>
            </head>
            <body>
            <div id="content"></div>
            </body>
            </html>
        """.trimIndent()
    }
}
