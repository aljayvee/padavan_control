package com.example.padavancontrol.network

import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface PadavanApiService {
    @GET("index.asp")
    suspend fun checkLogin(): Response<String>

    @GET("system_status_data.asp")
    suspend fun getSystemStatusData(): Response<String>

    @GET("status_internet.asp")
    suspend fun getStatusInternet(): Response<String>

    @GET("status_wanlink.asp")
    suspend fun getWanLinkStatus(): Response<String>

    @GET("lan_clients.asp")
    suspend fun getLanClients(): Response<String>

    @GET("update.cgi")
    suspend fun getTrafficStats(@Query("output") output: String = "netdev"): Response<String>

    @GET("status_lanlink.asp")
    suspend fun getLanLinkStatus(): Response<String>

    @GET("log_content.asp")
    suspend fun getSystemLogs(): Response<String>

    @FormUrlEncoded
    @POST("apply.cgi")
    suspend fun executeCommand(
        @Field("action_mode") actionMode: String = " SystemCmd ",
        @Field("SystemCmd") systemCmd: String
    ): Response<String>

    @GET("console_response.asp")
    suspend fun getConsoleResponse(): Response<String>

    @FormUrlEncoded
    @POST("apply.cgi")
    suspend fun commitFlash(
        @Field("action_mode") actionMode: String = " CommitFlash ",
        @Field("nvram_action") nvramAction: String = "commit_nvram"
    ): Response<String>

    @FormUrlEncoded
    @POST("apply.cgi")
    suspend fun clearLogs(
        @Field("action_mode") actionMode: String = " ClearLog "
    ): Response<String>

    @FormUrlEncoded
    @POST("apply.cgi")
    suspend fun performAction(
        @Field("action_mode") actionMode: String,
        @Field("nvram_action") nvramAction: String? = null,
        @Field("SystemCmd") systemCmd: String? = null,
        @Field("rt_radio_x") rtRadioX: String? = null,
        @Field("wl_radio_x") wlRadioX: String? = null
    ): Response<String>

    @GET("{page}")
    suspend fun getPageContent(@retrofit2.http.Path("page") page: String): Response<String>

    @FormUrlEncoded
    @POST("apply.cgi")
    suspend fun applySettings(@retrofit2.http.FieldMap fields: Map<String, String>): Response<String>
}
