package com.example.contactmanager.activities.setRingtone

import android.content.ContentValues
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.contactmanager.R
import com.example.contactmanager.adapters.RingtonesAdapter
import com.example.contactmanager.databinding.ActivitySetRingtoneBinding
import com.example.contactmanager.models.RingtoneModel
import com.example.contactmanager.utils.OnClickHandler

class SetRingtoneActivity : AppCompatActivity(), OnClickHandler {
    private lateinit var binding: ActivitySetRingtoneBinding
    private lateinit var adapter: RingtonesAdapter
    private var ringtoneList = mutableListOf<RingtoneModel>()
    private var mediaPlayer: MediaPlayer? = null
    private var selectedRingtone: RingtoneModel? = null
    private var currentPlayingPosition = -1
    private var pendingRingtoneUri: Uri? = null

    private val pickRingtoneLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { contentUri ->
                // Take persistable permission so we can read this URI later
                try {
                    contentResolver.takePersistableUriPermission(
                        contentUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: SecurityException) {
                }

                val displayName = getDisplayNameFromUri(contentUri)
                    .takeIf { it.isNotBlank() } ?: getString(R.string.custom_ringtone)
                setCustomRingtone(contentUri, displayName)
            }
        }

    private val requestAudioPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                displayCurrentRingtone()
                loadSystemRingtones()
            } else {
                Toast.makeText(
                    this,
                    "Permission required to read ringtone details",
                    Toast.LENGTH_SHORT
                ).show()
                binding.tvRingtoneName.text = "Default"
            }
        }

    private fun checkAndRequestAudioPermission(): Boolean {
        val permission =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                android.Manifest.permission.READ_MEDIA_AUDIO
            } else {
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            }

        return if (androidx.core.content.ContextCompat.checkSelfPermission(
                this,
                permission
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            true
        } else {
            requestAudioPermissionLauncher.launch(permission)
            false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!Settings.System.canWrite(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
            intent.data = "package:$packageName".toUri()
            startActivity(intent)
            Toast.makeText(this, getString(R.string.please_allow_modify_system), Toast.LENGTH_LONG)
                .show()
            finish()
            return
        }

        enableEdgeToEdge()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_set_ringtone)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initView()
        loadSystemRingtones()
        displayCurrentRingtone()
    }

    private fun initView() {
        binding.onClickHandler = this

        adapter = RingtonesAdapter(ringtoneList, onRingtoneSelected = { model ->
            selectedRingtone = model
        }, { model, position ->
            handlePlayPause(model, position)
        })
        binding.rvRingtone.adapter = adapter
        binding.rvRingtone.layoutManager = LinearLayoutManager(this)
    }

    private fun loadSystemRingtones() {
        val manager = RingtoneManager(this)
        manager.setType(RingtoneManager.TYPE_RINGTONE)
        val cursor = try {
            manager.cursor
        } catch (e: SecurityException) {
            Log.e("TAG", "loadSystemRingtones: ${e.message}")
            null
        } ?: return
        val currentUri = try {
            RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE)
        } catch (e: SecurityException) {
            Log.e("TAG", "loadSystemRingtones: ${e.message}")
            null
        }
        ringtoneList.clear()
        while (cursor.moveToNext()) {
            val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
            val uri = manager.getRingtoneUri(cursor.position)
            val isSelected = uri.toString() == currentUri?.toString()
            val model = RingtoneModel(title, uri, isSelected = isSelected)
            if (isSelected) selectedRingtone = model
            ringtoneList.add(model)
        }
        adapter.updateList(ringtoneList)
    }

    /** Resolve the human-readable display name from any content URI */
    private fun getDisplayNameFromUri(uri: Uri): String {
        // 1. OpenableColumns.DISPLAY_NAME — works for most file pickers
        runCatching {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { c ->
                    if (c.moveToFirst()) {
                        val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) {
                            val raw = c.getString(idx) ?: ""
                            if (raw.isNotBlank()) return raw.substringBeforeLast(".")
                        }
                    }
                }
        }
        // 2. MediaStore.Audio.Media.TITLE — fallback for media store URIs
        runCatching {
            contentResolver.query(uri, arrayOf(MediaStore.Audio.Media.TITLE), null, null, null)
                ?.use { c ->
                    if (c.moveToFirst()) {
                        val idx = c.getColumnIndex(MediaStore.Audio.Media.TITLE)
                        if (idx >= 0) {
                            val title = c.getString(idx) ?: ""
                            if (title.isNotBlank()) return title
                        }
                    }
                }
        }
        // Final fallback — must never return null or blank
        return getString(R.string.custom_ringtone).takeIf { it.isNotBlank() } ?: "Custom Ringtone"
    }

    private fun displayCurrentRingtone() {
        val currentUri = try {
            RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE)
        } catch (e: SecurityException) {
            Log.e("TAG", "displayCurrentRingtone: ${e.message}")
            null
        }

        if (currentUri == null) {
            binding.tvRingtoneName.text = "Default"
            return
        }

        val hasUriPermission = checkUriPermission(
            currentUri,
            android.os.Process.myPid(),
            android.os.Process.myUid(),
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        var name: String? = null
        if (hasUriPermission || currentUri.authority != "media") {
            name = queryNameFromContentResolver(currentUri)
        } else {
            if (checkAndRequestAudioPermission()) {
                name = queryNameFromContentResolver(currentUri)
            } else {
                return // Permission requested, UI will update in callback
            }
        }

        // Fallback: RingtoneManager title
        if (name.isNullOrBlank()) {
            name = try {
                RingtoneManager.getRingtone(this, currentUri)?.getTitle(this)
            } catch (e: SecurityException) {
                Log.e("TAG", "displayCurrentRingtone: ${e.message}")
                null
            }
        }

        binding.tvRingtoneName.text = name?.takeIf { it.isNotBlank() } ?: "Default"
    }

    private fun queryNameFromContentResolver(uri: Uri): String? {
        return try {
            contentResolver.query(
                uri,
                arrayOf(MediaStore.Audio.Media.TITLE),
                null, null, null
            )?.use { c ->
                if (c.moveToFirst()) {
                    val idx = c.getColumnIndex(MediaStore.Audio.Media.TITLE)
                    if (idx >= 0) c.getString(idx) else null
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun setCustomRingtone(uri: Uri, displayName: String) {

        val safeName = displayName.trim().takeIf { it.isNotBlank() }
            ?: getString(R.string.custom_ringtone).takeIf { it.isNotBlank() }
            ?: "Custom Ringtone"

        val mimeType = contentResolver.getType(uri)
            ?.takeIf { it.isNotBlank() }
            ?: "audio/mpeg"

        var stagedUri: Uri? = null

        try {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, safeName)
                put(MediaStore.MediaColumns.TITLE, safeName) // ✅ FIX (important)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)

                put(MediaStore.Audio.Media.IS_RINGTONE, true)
                put(MediaStore.Audio.Media.IS_MUSIC, false)
                put(MediaStore.Audio.Media.IS_NOTIFICATION, false)
                put(MediaStore.Audio.Media.IS_ALARM, false)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_RINGTONES) // ✅ FIX
                    put(MediaStore.MediaColumns.IS_PENDING, 1) // ✅ FIX
                }
            }

            val insertedUri = contentResolver.insert(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values
            )

            if (insertedUri != null) {
                var bytesCopied = 0L

                try {
                    contentResolver.openOutputStream(insertedUri)?.use { out ->
                        contentResolver.openInputStream(uri)?.use { input ->
                            bytesCopied = input.copyTo(out)
                        }
                    }
                } catch (copyEx: Exception) {
                    Log.e("SetRingtone", "copy failed: ${copyEx.message}")
                }

                if (bytesCopied > 0L) {

                    // ✅ Mark file as complete (VERY IMPORTANT)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val updateValues = ContentValues().apply {
                            put(MediaStore.MediaColumns.IS_PENDING, 0)
                        }
                        contentResolver.update(insertedUri, updateValues, null, null)
                    }

                    stagedUri = insertedUri

                } else {
                    contentResolver.delete(insertedUri, null, null)
                    Log.e("SetRingtone", "0 bytes copied, deleted")
                }
            }

        } catch (e: Exception) {
            Log.e("SetRingtone", "MediaStore insert failed: ${e.message}")
        }

        if (stagedUri == null) {
            Toast.makeText(
                this,
                "Could not prepare the audio file. Please try a different file.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // ✅ Extra safety check
        val ringtone = RingtoneManager.getRingtone(this, stagedUri)
        if (ringtone == null) {
            Toast.makeText(this, "Invalid ringtone file", Toast.LENGTH_SHORT).show()
            return
        }

        if (!Settings.System.canWrite(this)) {
            pendingRingtoneUri = stagedUri
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
            intent.data = "package:$packageName".toUri()
            startActivity(intent)
            Toast.makeText(
                this,
                getString(R.string.please_allow_modify_system_settings_and_come_back),
                Toast.LENGTH_LONG
            ).show()
            return
        }

        try {
            RingtoneManager.setActualDefaultRingtoneUri(
                this,
                RingtoneManager.TYPE_RINGTONE,
                stagedUri
            )

            Toast.makeText(this, "Ringtone set: $safeName", Toast.LENGTH_SHORT).show()

            pendingRingtoneUri = null
            loadSystemRingtones()
            adapter.clearSelection()
            selectedRingtone = null
            displayCurrentRingtone()

        } catch (e: SecurityException) {
            Log.e("SetRingtone", "Permission error: ${e.message}")

            pendingRingtoneUri = stagedUri
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
            intent.data = "package:$packageName".toUri()
            startActivity(intent)

            Toast.makeText(
                this,
                getString(R.string.please_ensure_modify_system_settings_is_enabled),
                Toast.LENGTH_LONG
            ).show()

        } catch (e: Exception) {
            Log.e("SetRingtone", "Error: ${e.message}")
            Toast.makeText(this, "Failed to set ringtone: ${e.message}", Toast.LENGTH_SHORT).show()
            pendingRingtoneUri = null
        }
    }

    private fun handlePlayPause(model: RingtoneModel, position: Int) {
        if (currentPlayingPosition == position) {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
                model.isPlaying = false
                binding.ivPlay.setImageResource(R.drawable.ic_play)
            } else {
                try {
                    mediaPlayer?.start()
                    model.isPlaying = true
                } catch (e: Exception) {
                    Log.e("TAG", "handlePlayPause start: ${e.message}")
                }
            }
            adapter.notifyPlayStateChanged(position)
        } else {
            stopAllPlayback()
            mediaPlayer?.release()
            try {
                mediaPlayer = MediaPlayer.create(this, model.uri)
                if (mediaPlayer != null) {
                    mediaPlayer?.start()
                    model.isPlaying = true
                    currentPlayingPosition = position
                    adapter.notifyPlayStateChanged(position)

                    mediaPlayer?.setOnCompletionListener {
                        model.isPlaying = false
                        adapter.notifyPlayStateChanged(position)
                        currentPlayingPosition = -1
                        binding.ivPlay.setImageResource(R.drawable.ic_play)
                    }
                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.cannot_play_ringtone),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("TAG", "handlePlayPause: ${e.message}")
                Toast.makeText(this, getString(R.string.cannot_play_ringtone), Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun stopAllPlayback() {
        if (currentPlayingPosition >= 0) {
            ringtoneList[currentPlayingPosition].isPlaying = false
            adapter.notifyPlayStateChanged(currentPlayingPosition)
        }
        currentPlayingPosition = -1
        try {
            if (mediaPlayer?.isPlaying == true) mediaPlayer?.stop()
        } catch (_: Exception) {
        }
        binding.ivPlay.setImageResource(R.drawable.ic_play)
    }

    private fun handleTopPlayPause() {
        if (mediaPlayer?.isPlaying == true && currentPlayingPosition == -2) {
            // Pause current top playback
            mediaPlayer?.pause()
            binding.ivPlay.setImageResource(R.drawable.ic_play)
            return
        }

        stopAllPlayback()
        val currentUri = try {
            RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE)
        } catch (e: SecurityException) {
            Log.e("TAG", "handleTopPlayPause: ${e.message}")
            null
        } ?: run {
            Toast.makeText(
                this,
                getString(R.string.no_ringtone_found_or_permission_missing), Toast.LENGTH_SHORT
            )
                .show()
            return
        }

        try {
            mediaPlayer?.release()
            val mp = MediaPlayer()

            // Try FileDescriptor first — works for content:// URIs (custom files)
            val pfd =
                runCatching { contentResolver.openFileDescriptor(currentUri, "r") }.getOrNull()
            if (pfd != null) {
                pfd.use { mp.setDataSource(it.fileDescriptor) }
                mp.prepare()
            } else {
                // System ringtone URI — use setDataSource with context
                mp.setDataSource(this, currentUri)
                mp.prepare()
            }

            mp.start()
            mediaPlayer = mp
            currentPlayingPosition = -2
            binding.ivPlay.setImageResource(R.drawable.ic_pause)

            mp.setOnCompletionListener {
                binding.ivPlay.setImageResource(R.drawable.ic_play)
                currentPlayingPosition = -1
            }
        } catch (e: Exception) {
            Log.e("TAG", "handleTopPlayPause: ${e.message}")
            // Ultimate fallback
            try {
                mediaPlayer?.release()
                mediaPlayer = MediaPlayer.create(this, currentUri)
                mediaPlayer?.start()
                currentPlayingPosition = -2
                binding.ivPlay.setImageResource(R.drawable.ic_pause)
                mediaPlayer?.setOnCompletionListener {
                    binding.ivPlay.setImageResource(R.drawable.ic_play)
                    currentPlayingPosition = -1
                }
            } catch (ex: Exception) {
                Log.e("TAG", "handleTopPlayPause: ${ex.message}")
                Toast.makeText(this, getString(R.string.cannot_play_ringtone), Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun setSystemRingtone(uri: Uri) {
        if (!Settings.System.canWrite(this)) {
            pendingRingtoneUri = uri
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
            intent.data = "package:$packageName".toUri()
            startActivity(intent)
            Toast.makeText(
                this, getString(R.string.please_allow_modify_system_settings_and_come_back),
                Toast.LENGTH_LONG
            ).show()
            return
        }

        try {
            RingtoneManager.setActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE, uri)
            Toast.makeText(this, getString(R.string.ringtone_set_successfully), Toast.LENGTH_SHORT)
                .show()
            pendingRingtoneUri = null
            displayCurrentRingtone()
        } catch (e: SecurityException) {
            Log.e("TAG", "setSystemRingtone: ${e.message}")
            pendingRingtoneUri = uri
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
            intent.data = "package:$packageName".toUri()
            startActivity(intent)
            Toast.makeText(
                this, getString(R.string.please_ensure_modify_system_settings_is_enabled),
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Log.e("TAG", "setSystemRingtone:ggfg ${e.message}")
            Toast.makeText(this, "Failed to set ringtone: ${e.message}", Toast.LENGTH_SHORT).show()
            pendingRingtoneUri = null
        }
    }

    override fun onResume() {
        super.onResume()
        pendingRingtoneUri?.let { pending ->
            if (Settings.System.canWrite(this)) {
                // pendingRingtoneUri is always a stable MediaStore URI (authority == "media")
                // because setCustomRingtone now only saves stagedUri (never the raw picker URI).
                // So we can always call setSystemRingtone directly here.
                setSystemRingtone(pending)
            }
        }
    }

    override fun onClick(view: View) {
        when (view.id) {
            binding.cvDone.id -> {
                val selected = selectedRingtone
                if (selected == null) {
                    Toast.makeText(
                        this,
                        getString(R.string.please_select_a_ringtone), Toast.LENGTH_SHORT
                    ).show()
                    return
                }
                val currentDefault = try {
                    RingtoneManager.getActualDefaultRingtoneUri(
                        this, RingtoneManager.TYPE_RINGTONE
                    )
                } catch (e: SecurityException) {
                    Log.e("TAG", "onClick: ${e.message}")
                    null
                }
                if (selected.uri.toString() == currentDefault?.toString()) {
                    Toast.makeText(
                        this,
                        getString(R.string.this_ringtone_is_already_set), Toast.LENGTH_SHORT
                    ).show()
                    return
                }
                setSystemRingtone(selected.uri)
            }

            binding.llCustomRingtone.id -> {
                // Stop any playing ringtone and clear list selection before opening picker
                stopAllPlayback()
                adapter.clearSelection()
                selectedRingtone = null
                pickRingtoneLauncher.launch("audio/*")
            }

            binding.llRecentRingtone.id -> {
                handleTopPlayPause()
            }

            binding.ivPlay.id -> {
                handleTopPlayPause()
            }

            binding.ivBack.id -> {
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}