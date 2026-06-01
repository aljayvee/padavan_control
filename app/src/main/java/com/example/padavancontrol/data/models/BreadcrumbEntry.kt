package com.example.padavancontrol.data.models

import kotlinx.serialization.Serializable

/**
 * Represents a single segment in the navigation breadcrumb trail.
 *
 * Example trail: Settings › Advanced › Wireless 2.4GHz › General
 * Each segment maps to a serializable NavKey so the user can tap
 * any breadcrumb to jump directly to that level.
 */
@Serializable
data class BreadcrumbEntry(
    /** Human-readable label displayed in the breadcrumb chip. */
    val label: String,
    /** Optional section within the category (e.g., "General", "Guest"). */
    val section: String? = null,
    /** Serialized NavKey JSON string for back-navigation on tap. */
    val navKeyJson: String = ""
)
