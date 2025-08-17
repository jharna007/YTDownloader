package com.example.ytdownloader

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.ytdownloader.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val TAG = "YTDownloader"

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (granted) {
            updateStatus("Permissions granted. Ready to download.")
        } else {
            updateStatus("Storage permission required for downloads.")
            Toast.makeText(this, "Storage permission is required", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        checkAndRequestPermissions()
        initializeTools()
    }

    private fun setupUI() {
        binding.btnDownloadMp4.setOnClickListener {
            val url = binding.etUrl.text.toString().trim()
            if (isValidYouTubeUrl(url)) {
                downloadVideo(url, false)
            } else {
                updateStatus("Please enter a valid YouTube URL")
                Toast.makeText(this, "Invalid YouTube URL", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnDownloadMp3.setOnClickListener {
            val url = binding.etUrl.text.toString().trim()
            if (isValidYouTubeUrl(url)) {
                downloadVideo(url, true)
            } else {
                updateStatus("Please enter a valid YouTube URL")
                Toast.makeText(this, "Invalid YouTube URL", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isValidYouTubeUrl(url: String): Boolean {
        return url.contains("youtube.com/watch") || 
               url.contains("youtu.be/") || 
               url.contains("youtube.com/shorts/")
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) 
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) 
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6-12
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissions.isNotEmpty()) {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }
    }

    private fun initializeTools() {
        lifecycleScope.launch {
            updateStatus("Initializing download tools...")
            try {
                copyAssetToInternalStorage("yt-dlp", "yt-dlp")
                copyAssetToInternalStorage("ffmpeg", "ffmpeg")

                val ytDlpFile = File(filesDir, "yt-dlp")
                val ffmpegFile = File(filesDir, "ffmpeg")

                // Make files executable
                ytDlpFile.setExecutable(true)
                ffmpegFile.setExecutable(true)

                updateStatus("Tools initialized successfully. Ready to download.")
                Log.d(TAG, "yt-dlp and ffmpeg copied and made executable")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize tools", e)
                updateStatus("Failed to initialize download tools: $${e.message}")
            }
        }
    }

    private suspend fun copyAssetToInternalStorage(assetName: String, fileName: String) = withContext(Dispatchers.IO) {
        try {
            val inputStream = assets.open(assetName)
            val outputFile = File(filesDir, fileName)

            if (!outputFile.exists()) {
                val outputStream = FileOutputStream(outputFile)
                inputStream.copyTo(outputStream)
                inputStream.close()
                outputStream.close()
                Log.d(TAG, "Copied $$assetName to $$fileName")
            } else {
                Log.d(TAG, "$$fileName already exists")
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to copy $$assetName", e)
            throw e
        }
    }

    private fun downloadVideo(url: String, audioOnly: Boolean) {
        if (!hasStoragePermission()) {
            checkAndRequestPermissions()
            return
        }

        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                binding.btnDownloadMp4.isEnabled = false
                binding.btnDownloadMp3.isEnabled = false

                val format = if (audioOnly) "MP3" else "MP4"
                updateStatus("Starting $$format download...")

                val success = withContext(Dispatchers.IO) {
                    executeDownload(url, audioOnly)
                }

                if (success) {
                    updateStatus("$$format download completed successfully!")
                    Toast.makeText(this@MainActivity, "Download completed!", Toast.LENGTH_SHORT).show()
                } else {
                    updateStatus("Download failed. Please check the URL and try again.")
                    Toast.makeText(this@MainActivity, "Download failed", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Download error", e)
                updateStatus("Download error: $${e.message}")
                Toast.makeText(this@MainActivity, "Download error: $${e.localizedMessage}", Toast.LENGTH_LONG).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.btnDownloadMp4.isEnabled = true
                binding.btnDownloadMp3.isEnabled = true
            }
        }
    }

    private suspend fun executeDownload(url: String, audioOnly: Boolean): Boolean = withContext(Dispatchers.IO) {
        try {
            val ytDlpPath = File(filesDir, "yt-dlp").absolutePath
            val downloadsDir = getDownloadsDirectory()

            // Get video info first
            updateStatus("Fetching video information...")
            val titleCommand = listOf(
                ytDlpPath,
                "--get-title",
                url
            )

            val titleResult = executeCommand(titleCommand)
            val videoTitle = titleResult.output.trim().take(50).replace(Regex("[^a-zA-Z0-9\s_-]"), "")

            if (audioOnly) {
                // Download audio and convert to MP3
                updateStatus("Downloading audio...")
                val audioCommand = listOf(
                    ytDlpPath,
                    "-f", "bestaudio[ext=m4a]/bestaudio",
                    "--extract-audio",
                    "--audio-format", "mp3",
                    "--audio-quality", "192K",
                    "-o", "$${downloadsDir.absolutePath}/$$videoTitle.%(ext)s",
                    url
                )

                val audioResult = executeCommand(audioCommand)
                return@withContext audioResult.exitCode == 0
            } else {
                // Download video (MP4)
                updateStatus("Downloading video...")
                val videoCommand = listOf(
                    ytDlpPath,
                    "-f", "best[ext=mp4]/best",
                    "-o", "$${downloadsDir.absolutePath}/$$videoTitle.%(ext)s",
                    url
                )

                val videoResult = executeCommand(videoCommand)
                return@withContext videoResult.exitCode == 0
            }
        } catch (e: Exception) {
            Log.e(TAG, "Execute download error", e)
            return@withContext false
        }
    }

    private fun executeCommand(command: List<String>): CommandResult {
        return try {
            Log.d(TAG, "Executing command: $${command.joinToString(" ")}")

            val processBuilder = ProcessBuilder(command)
            processBuilder.redirectErrorStream(true)

            val process = processBuilder.start()
            val output = process.inputStream.bufferedReader().use { it.readText() }

            val exitCode = if (process.waitFor(120, TimeUnit.SECONDS)) {
                process.exitValue()
            } else {
                process.destroyForcibly()
                -1
            }

            Log.d(TAG, "Command output: $$output")
            Log.d(TAG, "Exit code: $$exitCode")

            CommandResult(exitCode, output)
        } catch (e: Exception) {
            Log.e(TAG, "Command execution failed", e)
            CommandResult(-1, e.message ?: "Unknown error")
        }
    }

    private fun getDownloadsDirectory(): File {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ - use scoped storage
            File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "YTDownloader")
        } else {
            // Android 9 and below
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "YTDownloader")
        }.apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    private fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun updateStatus(message: String) {
        runOnUiThread {
            binding.tvStatus.text = "$${binding.tvStatus.text}\n$$message"
            Log.d(TAG, message)
        }
    }

    data class CommandResult(
        val exitCode: Int,
        val output: String
    )
}
