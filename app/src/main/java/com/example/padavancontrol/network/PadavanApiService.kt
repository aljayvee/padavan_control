package com.example.padavancontrol.network

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.http.Path

interface PadavanApiService {
    @GET("index.asp")
    suspend fun checkLogin(): Response<String>

    @GET("system_status_data.asp")
    suspend fun getSystemStatusData(): Response<String>

    @GET("status_internet.asp")
    suspend fun getStatusInternet(): Response<String>

    @GET("status_wanlink.asp")
    suspend fun getWanLinkStatus(): Response<String>

    @GET("device-map/clients.asp")
    suspend fun getLanClients(): Response<String>

    @GET("update.cgi")
    suspend fun getTrafficStats(@Query("output") output: String = "netdev"): Response<String>

    @GET("status_lanlink.asp")
    suspend fun getLanLinkStatus(): Response<String>

    @GET("status_eth_mib.asp")
    suspend fun getEthMibStatus(): Response<String>

    @GET("Main_WStatus2g_Content.asp")
    suspend fun getWirelessStatus2g(): Response<String>

    @GET("Main_WStatus_Content.asp")
    suspend fun getWirelessStatus5g(): Response<String>

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
        @Field("nvram_action") nvramAction: String
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
    suspend fun getPageContent(
        @retrofit2.http.Path("page") page: String,
        @retrofit2.http.Header("Custom-Read-Timeout") readTimeout: String? = null
    ): Response<String>

    @FormUrlEncoded
    @POST("start_apply.htm")
    suspend fun applySettings(@retrofit2.http.FieldMap fields: Map<String, String>): Response<String>

    @FormUrlEncoded
    @POST("{path}")
    suspend fun postGeneric(
        @retrofit2.http.Path("path") path: String,
        @retrofit2.http.FieldMap fields: Map<String, String>
    ): Response<String>

    @GET("Settings_{model}.CFG")
    suspend fun downloadSettings(@Path("model") model: String): Response<ResponseBody>

    @GET("Storage_{model}.TBZ")
    suspend fun downloadStorage(@Path("model") model: String): Response<ResponseBody>

    @Multipart
    @POST("restore_nv.cgi")
    suspend fun uploadSettingsBackup(@Part file: MultipartBody.Part): Response<String>

    @Multipart
    @POST("restore_st.cgi")
    suspend fun uploadStorageBackup(@Part file: MultipartBody.Part): Response<String>

    @Multipart
    @POST("upgrade.cgi")
    suspend fun uploadFirmwareUpgrade(@Part file: MultipartBody.Part): Response<String>
}
