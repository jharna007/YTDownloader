package com.example.ytdownloader

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.ytdownloader.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var ytDlpPath: String
    private lateinit var ffmpegPath: String
    private lateinit var downloadDir: File

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            updateStatus("Permissions granted")
        } else {
            updateStatus("Some permissions were denied. App may not function properly.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        initializeApp()
    }

    private fun setupUI() {
        binding.downloadMp4Button.setOnClickListener {
            val url = binding.urlEditText.text.toString().trim()
            if (validateUrl(url)) {
                downloadVideo(url, "mp4")
            } else {
                updateStatus(getString(R.string.invalid_url))
            }
        }

        binding.downloadMp3Button.setOnClickListener {
            val url = binding.urlEditText.text.toString().trim()
            if (validateUrl(url)) {
                downloadVideo(url, "mp3")
            } else {
                updateStatus(getString(R.string.invalid_url))
            }
        }
    }

    private fun initializeApp() {
        lifecycleScope.launch {
            try {
                updateStatus(getString(R.string.initializing))
                setupPermissions()
                setupDirectories()
                copyBinaries()
                updateStatus(getString(R.string.ready_to_download))
            } catch (e: Exception) {
                Log.e("MainActivity", "Initialization failed", e)
                updateStatus("Initialization failed: \${e.message}")
            }
        }
    }

    private fun setupPermissions() {
        val permissions = mutableListOf<String>()

        // Add permissions based on Android version
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
        }

        // Check if permissions need to be requested
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    private fun setupDirectories() {
        // Setup download directory based on Android version
        downloadDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ (Scoped Storage)
            File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "YTDownloader")
        } else {
            // Android 9 and below
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "YTDownloader")
        }

        if (!downloadDir.exists()) {
            downloadDir.mkdirs()
        }

        updateStatus("Download directory: \${downloadDir.absolutePath}")
    }

    private suspend fun copyBinaries() {
        withContext(Dispatchers.IO) {
            try {
                // Copy yt-dlp
                val ytDlpFile = File(filesDir, "yt-dlp")
                if (!ytDlpFile.exists()) {
                    assets.open("yt-dlp").use { input ->
                        FileOutputStream(ytDlpFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    ytDlpFile.setExecutable(true)
                }
                ytDlpPath = ytDlpFile.absolutePath

                // Copy ffmpeg
                val ffmpegFile = File(filesDir, "ffmpeg")
                if (!ffmpegFile.exists()) {
                    assets.open("ffmpeg").use { input ->
                        FileOutputStream(ffmpegFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    ffmpegFile.setExecutable(true)
                }
                ffmpegPath = ffmpegFile.absolutePath

                withContext(Dispatchers.Main) {
                    updateStatus("Binaries copied and ready")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    updateStatus("Failed to copy binaries: \${e.message}")
                }
                throw e
            }
        }
    }

    private fun validateUrl(url: String): Boolean {
        if (url.isEmpty()) return false

        val youtubePatterns = listOf(
            Pattern.compile(".*youtube\.com/watch\?v=.*"),
            Pattern.compile(".*youtu\.be/.*"),
            Pattern.compile(".*youtube\.com/shorts/.*"),
            Pattern.compile(".*m\.youtube\.com/watch\?v=.*")
        )

        return youtubePatterns.any { it.matcher(url).matches() }
    }

    private fun downloadVideo(url: String, format: String) {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                updateStatus("Starting download...")

                val success = withContext(Dispatchers.IO) {
                    when (format) {
                        "mp4" -> downloadMp4(url)
                        "mp3" -> downloadMp3(url)
                        else -> false
                    }
                }

                if (success) {
                    updateStatus(getString(R.string.download_complete))
                    Toast.makeText(this@MainActivity, "Download completed!", Toast.LENGTH_SHORT).show()
                } else {
                    updateStatus(getString(R.string.download_failed))
                    Toast.makeText(this@MainActivity, "Download failed!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Download error", e)
                updateStatus("Download error: \${e.message}")
                Toast.makeText(this@MainActivity, "Download error!", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun downloadMp4(url: String): Boolean {
        return executeCommand(listOf(
            ytDlpPath,
            "-f", "best[ext=mp4]/best",
            "-o", "\${downloadDir.absolutePath}/%(title)s.%(ext)s",
            url
        ))
    }

    private fun downloadMp3(url: String): Boolean {
        return executeCommand(listOf(
            ytDlpPath,
            "--extract-audio",
            "--audio-format", "mp3",
            "-o", "\${downloadDir.absolutePath}/%(title)s.%(ext)s",
            url
        ))
    }

    private fun executeCommand(command: List<String>): Boolean {
        return try {
            val processBuilder = ProcessBuilder(command)
            processBuilder.redirectErrorStream(true)

            val process = processBuilder.start()

            // Read output in a separate thread
            val outputReader = Thread {
                process.inputStream.bufferedReader().use { reader ->
                    reader.lineSequence().forEach { line ->
                        runOnUiThread {
                            updateStatus(line)
                        }
                    }
                }
            }
            outputReader.start()

            // Wait for process with timeout
            val finished = process.waitFor(300, TimeUnit.SECONDS)

            if (!finished) {
                process.destroyForcibly()
                runOnUiThread {
                    updateStatus("Download timeout - process terminated")
                }
                return false
            }

            val exitCode = process.exitValue()
            runOnUiThread {
                updateStatus("Process finished with exit code: \$exitCode")
            }

            exitCode == 0
        } catch (e: Exception) {
            runOnUiThread {
                updateStatus("Command execution failed: \${e.message}")
            }
            false
        }
    }

    private fun updateStatus(message: String) {
        runOnUiThread {
            val currentText = binding.statusTextView.text.toString()
            val newText = if (currentText == getString(R.string.ready_to_download)) {
                message
            } else {
                "\$currentText\n\$message"
            }
            binding.statusTextView.text = newText

            // Auto-scroll to bottom
            binding.statusScrollView.post {
                binding.statusScrollView.fullScroll(View.FOCUS_DOWN)
            }
        }
    }
}