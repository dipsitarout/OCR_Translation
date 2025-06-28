package com.example.imagetotext.network



import android.net.Uri
import okhttp3.*
import org.json.JSONArray
import java.io.IOException
import android.util.Log
import java.net.URLEncoder

object TranslateApi {
    private val client = OkHttpClient()

    fun translateText(
        text: String,
        targetLanguage: String,
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val url = "https://translate.googleapis.com/translate_a/single" +
                "?client=gtx&sl=auto&tl=$targetLanguage&dt=t&q=${URLEncoder.encode(text, "UTF-8")}"

        val request = Request.Builder().url(url).build()
        val client = OkHttpClient()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onError("Network error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    onError("API error: ${response.code}")
                    return
                }

                try {
                    val responseBody = response.body?.string()
                    val jsonArray = JSONArray(responseBody)
                    val translatedArray = jsonArray.getJSONArray(0)

                    val resultBuilder = StringBuilder()
                    for (i in 0 until translatedArray.length()) {
                        val segment = translatedArray.getJSONArray(i)
                        resultBuilder.append(segment.getString(0))
                    }

                    onResult(resultBuilder.toString().trim())
                } catch (e: Exception) {
                    onError("Parsing error: ${e.message}")
                }
            }
        })
    }
}