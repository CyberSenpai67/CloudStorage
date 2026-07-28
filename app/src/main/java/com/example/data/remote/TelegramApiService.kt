package com.example.data.remote

import android.content.ContentResolver
import android.net.Uri
import com.example.data.model.FileCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.Buffer
import okio.BufferedSink
import okio.ForwardingSink
import okio.Sink
import okio.buffer
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.util.concurrent.TimeUnit

data class TelegramBotResult(
    val isValid: Boolean,
    val botId: Long = 0,
    val botName: String = "",
    val username: String = "",
    val errorMessage: String? = null
)

data class TelegramUploadResult(
    val success: Boolean,
    val messageId: Long = 0,
    val fileId: String = "",
    val uniqueFileId: String = "",
    val fileName: String = "",
    val mimeType: String = "",
    val sizeBytes: Long = 0,
    val filePathOnTelegram: String? = null,
    val errorMessage: String? = null
)

class TelegramApiService {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    suspend fun validateBotToken(token: String): TelegramBotResult = withContext(Dispatchers.IO) {
        val cleanToken = token.trim()
        if (cleanToken.isEmpty()) {
            return@withContext TelegramBotResult(isValid = false, errorMessage = "Token cannot be empty")
        }

        val url = "https://api.telegram.org/bot$cleanToken/getMe"
        try {
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                if (!response.isSuccessful || bodyString.isEmpty()) {
                    return@withContext TelegramBotResult(
                        isValid = false,
                        errorMessage = "Failed connection (HTTP ${response.code})"
                    )
                }

                val json = JSONObject(bodyString)
                val ok = json.optBoolean("ok", false)
                if (ok && json.has("result")) {
                    val result = json.getJSONObject("result")
                    val id = result.optLong("id", 0)
                    val firstName = result.optString("first_name", "Telegram Bot")
                    val username = result.optString("username", "")
                    TelegramBotResult(
                        isValid = true,
                        botId = id,
                        botName = firstName,
                        username = username
                    )
                } else {
                    val description = json.optString("description", "Invalid Bot Token")
                    TelegramBotResult(isValid = false, errorMessage = description)
                }
            }
        } catch (e: Exception) {
            TelegramBotResult(isValid = false, errorMessage = e.localizedMessage ?: "Network error")
        }
    }

    suspend fun fetchLatestChatId(token: String): String? = withContext(Dispatchers.IO) {
        val cleanToken = token.trim()
        val url = "https://api.telegram.org/bot$cleanToken/getUpdates?limit=10"
        try {
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: return@withContext null
                val json = JSONObject(bodyString)
                if (json.optBoolean("ok", false) && json.has("result")) {
                    val updates = json.getJSONArray("result")
                    for (i in updates.length() - 1 downTo 0) {
                        val update = updates.getJSONObject(i)
                        val message = when {
                            update.has("message") -> update.getJSONObject("message")
                            update.has("channel_post") -> update.getJSONObject("channel_post")
                            update.has("my_chat_member") -> update.getJSONObject("my_chat_member")
                            else -> null
                        }
                        if (message != null && message.has("chat")) {
                            val chat = message.getJSONObject("chat")
                            return@withContext chat.optLong("id").toString()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    suspend fun uploadFile(
        token: String,
        chatId: String,
        fileUri: Uri,
        fileName: String,
        mimeType: String,
        category: FileCategory,
        contentResolver: ContentResolver,
        onProgress: (Float) -> Unit
    ): TelegramUploadResult = withContext(Dispatchers.IO) {
        val cleanToken = token.trim()
        val cleanChatId = chatId.trim()

        // Choose endpoint based on category
        val endpoint = when (category) {
            FileCategory.IMAGE -> "sendPhoto"
            FileCategory.VIDEO -> "sendVideo"
            FileCategory.AUDIO -> "sendAudio"
            else -> "sendDocument"
        }

        val paramName = when (category) {
            FileCategory.IMAGE -> "photo"
            FileCategory.VIDEO -> "video"
            FileCategory.AUDIO -> "audio"
            else -> "document"
        }

        val url = "https://api.telegram.org/bot$cleanToken/$endpoint"

        try {
            val inputStream = contentResolver.openInputStream(fileUri)
                ?: return@withContext TelegramUploadResult(success = false, errorMessage = "Could not open file stream")

            val fileBytes = inputStream.use { it.readBytes() }
            val mediaType = mimeType.ifEmpty { "application/octet-stream" }.toMediaTypeOrNull()

            val rawRequestBody = RequestBody.create(mediaType, fileBytes)
            val countingRequestBody = CountingRequestBody(rawRequestBody) { bytesWritten, contentLength ->
                if (contentLength > 0) {
                    onProgress(bytesWritten.toFloat() / contentLength.toFloat())
                }
            }

            val multipartBuilder = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", cleanChatId)
                .addFormDataPart("caption", "☁️ Uploaded via Cloudhub: $fileName")
                .addFormDataPart(paramName, fileName, countingRequestBody)

            val request = Request.Builder()
                .url(url)
                .post(multipartBuilder.build())
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                if (!response.isSuccessful || bodyString.isEmpty()) {
                    return@withContext TelegramUploadResult(
                        success = false,
                        errorMessage = "Upload failed HTTP ${response.code}: $bodyString"
                    )
                }

                val json = JSONObject(bodyString)
                if (!json.optBoolean("ok", false)) {
                    val desc = json.optString("description", "Telegram API upload rejected")
                    return@withContext TelegramUploadResult(success = false, errorMessage = desc)
                }

                val result = json.getJSONObject("result")
                val messageId = result.optLong("message_id", 0)

                var fileId = ""
                var uniqueFileId = ""
                var resFileName = fileName
                var resMimeType = mimeType
                var resSizeBytes = fileBytes.size.toLong()

                if (result.has("document")) {
                    val doc = result.getJSONObject("document")
                    fileId = doc.optString("file_id", "")
                    uniqueFileId = doc.optString("file_unique_id", "")
                    resFileName = doc.optString("file_name", fileName)
                    resMimeType = doc.optString("mime_type", mimeType)
                    resSizeBytes = doc.optLong("file_size", resSizeBytes)
                } else if (result.has("photo")) {
                    val photos = result.getJSONArray("photo")
                    val largestPhoto = photos.getJSONObject(photos.length() - 1)
                    fileId = largestPhoto.optString("file_id", "")
                    uniqueFileId = largestPhoto.optString("file_unique_id", "")
                    resSizeBytes = largestPhoto.optLong("file_size", resSizeBytes)
                } else if (result.has("video")) {
                    val vid = result.getJSONObject("video")
                    fileId = vid.optString("file_id", "")
                    uniqueFileId = vid.optString("file_unique_id", "")
                    resMimeType = vid.optString("mime_type", mimeType)
                    resSizeBytes = vid.optLong("file_size", resSizeBytes)
                } else if (result.has("audio")) {
                    val aud = result.getJSONObject("audio")
                    fileId = aud.optString("file_id", "")
                    uniqueFileId = aud.optString("file_unique_id", "")
                    resFileName = aud.optString("file_name", fileName)
                    resMimeType = aud.optString("mime_type", mimeType)
                    resSizeBytes = aud.optLong("file_size", resSizeBytes)
                }

                val telegramFilePath = getTelegramFilePath(cleanToken, fileId)

                TelegramUploadResult(
                    success = true,
                    messageId = messageId,
                    fileId = fileId,
                    uniqueFileId = uniqueFileId,
                    fileName = resFileName,
                    mimeType = resMimeType,
                    sizeBytes = resSizeBytes,
                    filePathOnTelegram = telegramFilePath
                )
            }
        } catch (e: Exception) {
            TelegramUploadResult(success = false, errorMessage = e.localizedMessage ?: "Upload error")
        }
    }

    suspend fun getTelegramFilePath(token: String, fileId: String): String? = withContext(Dispatchers.IO) {
        if (fileId.isEmpty()) return@withContext null
        val cleanToken = token.trim()
        val url = "https://api.telegram.org/bot$cleanToken/getFile?file_id=$fileId"
        try {
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: return@withContext null
                val json = JSONObject(bodyString)
                if (json.optBoolean("ok", false) && json.has("result")) {
                    val result = json.getJSONObject("result")
                    return@withContext result.optString("file_path", null)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    fun buildDirectDownloadUrl(token: String, filePath: String): String {
        return "https://api.telegram.org/file/bot${token.trim()}/$filePath"
    }

    suspend fun deleteMessage(token: String, chatId: String, messageId: Long): Boolean = withContext(Dispatchers.IO) {
        if (messageId <= 0) return@withContext true
        val cleanToken = token.trim()
        val cleanChatId = chatId.trim()
        val url = "https://api.telegram.org/bot$cleanToken/deleteMessage"

        try {
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", cleanChatId)
                .addFormDataPart("message_id", messageId.toString())
                .build()

            val request = Request.Builder().url(url).post(body).build()
            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                val json = JSONObject(bodyString)
                json.optBoolean("ok", false)
            }
        } catch (e: Exception) {
            false
        }
    }
}

class CountingRequestBody(
    private val delegate: RequestBody,
    private val onProgress: (bytesWritten: Long, contentLength: Long) -> Unit
) : RequestBody() {

    override fun contentType() = delegate.contentType()

    override fun contentLength() = try {
        delegate.contentLength()
    } catch (e: Exception) {
        -1L
    }

    override fun writeTo(sink: BufferedSink) {
        val countingSink = CountingSink(sink, contentLength(), onProgress)
        val bufferedSink = countingSink.buffer()
        delegate.writeTo(bufferedSink)
        bufferedSink.flush()
    }
}

class CountingSink(
    delegate: Sink,
    private val totalBytes: Long,
    private val onProgress: (bytesWritten: Long, contentLength: Long) -> Unit
) : ForwardingSink(delegate) {

    private var bytesWritten = 0L

    override fun write(source: Buffer, byteCount: Long) {
        super.write(source, byteCount)
        bytesWritten += byteCount
        onProgress(bytesWritten, totalBytes)
    }
}
