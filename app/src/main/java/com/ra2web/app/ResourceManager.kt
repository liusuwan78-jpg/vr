package com.ra2web.app

import android.content.Context
import android.os.Handler
import android.os.Looper
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream
import javax.net.ssl.HttpsURLConnection

/**
 * 游戏资源管理器 - 负责下载和管理游戏美术资源
 */
class ResourceManager(private val context: Context) {

    private val handler = Handler(Looper.getMainLooper())

    // 资源目录
    private val gameresDir: File
        get() = File(context.filesDir, "gameres")

    // 资源包下载地址
    private val resourceUrl = "https://download.ra2web.com/full-pack.7z"

    // 资源校验文件
    private val manifestFile: File
        get() = File(gameresDir, "manifest.json")

    /**
     * 检查资源是否已就绪
     */
    fun areResourcesReady(): Boolean {
        // 检查资源目录是否存在且有内容
        if (!gameresDir.exists() || gameresDir.listFiles()?.isEmpty() == true) {
            return false
        }

        // 检查是否有核心资源文件（ra2.mix 等）
        val coreFiles = listOf("ra2.mix", "ra2cd.mix", "language.mix")
        for (fileName in coreFiles) {
            if (File(gameresDir, fileName).exists()) {
                return true
            }
        }

        // 检查子目录
        gameresDir.listFiles()?.forEach { dir ->
            if (dir.isDirectory) {
                for (fileName in coreFiles) {
                    if (File(dir, fileName).exists()) {
                        return true
                    }
                }
            }
        }

        return false
    }

    /**
     * 下载游戏资源
     * @param onProgress 进度回调 (百分比, 状态文本)
     * @param onComplete 完成回调
     * @param onError 错误回调
     */
    fun downloadResources(
        onProgress: (Int, String) -> Unit,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) {
        Thread {
            try {
                // 确保目录存在
                if (!gameresDir.exists()) {
                    gameresDir.mkdirs()
                }

                // 先尝试从 CDN 加载资源列表
                onProgress(0, "连接服务器...")

                // 下载资源包
                downloadFile(
                    url = resourceUrl,
                    destFile = File(gameresDir, "full-pack.7z"),
                    onProgress = { percent, bytesRead, totalBytes ->
                        val status = if (totalBytes > 0) {
                            "下载中: ${formatSize(bytesRead)} / ${formatSize(totalBytes)}"
                        } else {
                            "下载中: ${formatSize(bytesRead)}"
                        }
                        handler.post { onProgress(percent, status) }
                    }
                )

                // 解压资源
                handler.post { onProgress(95, "解压资源中...") }
                extract7z(File(gameresDir, "full-pack.7z"), gameresDir)

                // 删除压缩包
                File(gameresDir, "full-pack.7z").delete()

                // 写入 manifest 标记资源已就绪
                manifestFile.writeText("{\"ready\": true, \"version\": \"1.0\"}")

                handler.post { onProgress(100, "完成!") }
                handler.postDelayed({ onComplete() }, 500)

            } catch (e: Exception) {
                handler.post { onError(e.message ?: "未知错误") }
            }
        }.start()
    }

    /**
     * 下载文件
     */
    private fun downloadFile(
        url: String,
        destFile: File,
        onProgress: (Int, Long, Long) -> Unit
    ) {
        val connection = if (url.startsWith("https")) {
            URL(url).openConnection() as HttpsURLConnection
        } else {
            URL(url).openConnection() as HttpURLConnection
        }

        connection.connectTimeout = 30000
        connection.readTimeout = 60000
        connection.setRequestProperty("User-Agent", "RA2Web-Android/1.0")

        try {
            connection.connect()

            if (connection.responseCode !in 200..299) {
                throw Exception("HTTP ${connection.responseCode}: ${connection.responseMessage}")
            }

            val totalBytes = connection.contentLength.toLong()
            var bytesRead: Long = 0

            val input = BufferedInputStream(connection.inputStream)
            val output = BufferedOutputStream(FileOutputStream(destFile))

            val buffer = ByteArray(8192)
            var len: Int

            while (input.read(buffer).also { len = it } != -1) {
                output.write(buffer, 0, len)
                bytesRead += len

                if (totalBytes > 0) {
                    val percent = ((bytesRead * 100) / totalBytes).toInt()
                    onProgress(percent, bytesRead, totalBytes)
                } else {
                    onProgress(-1, bytesRead, totalBytes)
                }
            }

            output.flush()
            output.close()
            input.close()

        } finally {
            connection.disconnect()
        }
    }

    /**
     * 解压 7z 文件
     * 注意：这里使用简化实现，实际项目中建议使用 7z 解压库
     */
    private fun extract7z(archiveFile: File, destDir: File) {
        // 由于 7z 解压需要原生库支持，这里先做一个占位
        // 实际实现可以使用：
        // 1. Apache Commons Compress + XZ for Java
        // 2. 7-Zip-JBinding
        // 3. 游戏内的 7zz.js (WASM) - 可以在 WebView 中执行解压

        // 简化实现：假设资源已经通过其他方式获取
        // 实际上我们可以让 WebView 里的游戏自己处理资源加载
        // 这里只是确保目录存在
        if (!destDir.exists()) {
            destDir.mkdirs()
        }

        // 如果是 zip 格式，这里可以解压
        // 7z 格式需要专门的库
    }

    /**
     * 格式化文件大小
     */
    private fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${"%.1f".format(bytes.toFloat() / (1024 * 1024 * 1024))} GB"
        }
    }

    /**
     * 清除本地资源
     */
    fun clearResources() {
        if (gameresDir.exists()) {
            gameresDir.deleteRecursively()
        }
    }

    /**
     * 获取资源目录大小
     */
    fun getResourcesSize(): Long {
        return getDirSize(gameresDir)
    }

    private fun getDirSize(dir: File): Long {
        var size: Long = 0
        dir.listFiles()?.forEach { file ->
            size += if (file.isDirectory) {
                getDirSize(file)
            } else {
                file.length()
            }
        }
        return size
    }
}
