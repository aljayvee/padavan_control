package com.example.padavancontrol.ui

import com.example.padavancontrol.data.AppLocale
import com.example.padavancontrol.data.t
import com.example.padavancontrol.data.models.AdminConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class LanguageIntegrationTest {

    @Test
    fun testAppLocaleTranslation() {
        // Force language to English
        AppLocale.currentLanguage = "EN"
        assertEquals("Admin - System Settings", t("Admin - System Settings"))
        assertEquals("Device Name", t("Device Name"))
        
        // Force language to Chinese (Simplified)
        AppLocale.currentLanguage = "CN"
        assertEquals("系统管理 - 系统设置", t("Admin - System Settings"))
        assertEquals("设备名称", t("Device Name"))
        
        // Non-existent translations should return the original key
        assertEquals("Unknown Key Test", t("Unknown Key Test"))
        
        // Reset to EN
        AppLocale.currentLanguage = "EN"
    }

    @Test
    fun testLanguageConfigState() {
        val config = AdminConfig(selectLang = "CN")
        assertEquals("CN", config.selectLang)
    }
}
