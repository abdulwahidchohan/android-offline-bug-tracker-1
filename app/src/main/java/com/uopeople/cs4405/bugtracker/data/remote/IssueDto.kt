package com.uopeople.cs4405.bugtracker.data.remote

import com.google.gson.annotations.SerializedName

/**
 * Remote Data Transfer Object representing an issue on the remote REST API.
 */
data class IssueDto(
    @SerializedName("id")
    val id: String,

    @SerializedName("title")
    val title: String,

    @SerializedName("description")
    val description: String? = null,

    @SerializedName("priority")
    val priority: String,

    @SerializedName("status")
    val status: String,

    @SerializedName("createdAt")
    val createdAt: Long,

    @SerializedName("updatedAt")
    val updatedAt: Long,

    @SerializedName("isDeleted")
    val isDeleted: Boolean = false,

    @SerializedName("serverVersion")
    val serverVersion: Long = 1L
)
