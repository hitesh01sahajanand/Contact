package com.example.contactmanager.activities.setRingtone

import android.content.ContentValues
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
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

    private val pickRingtoneLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { contentUri ->
            // Take persistable permission so we can read this URI later
            try {
                contentResolver.takePersistableUriPermission(
                    contentUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) { }

            val displayName = getDisplayNameFromUri(contentUri)
            setCustomRingtone(contentUri, displayName)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (!Settings.System.canWrite(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
            Toast.makeText(this, "Please allow 'Modify system settings' to use this feature", Toast.LENGTH_LONG).show()
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
            null
        } ?: return
        val currentUri = try {
            RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE)
        } catch (e: SecurityException) {
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
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
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
            contentResolver.query(uri, arrayOf(MediaStore.Audio.Media.TITLE), null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val idx = c.getColumnIndex(MediaStore.Audio.Media.TITLE)
                    if (idx >= 0) {
                        val title = c.getString(idx) ?: ""
                        if (title.isNotBlank()) return title
                    }
                }
            }
        }
        return "Custom Ringtone"
    }

    private fun displayCurrentRingtone() {
        val currentUri = try {
            RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE)
        } catch (e: SecurityException) {
            null
        }
        val name = if (currentUri != null) {
            // Try ContentResolver (correct for MediaStore-inserted URIs and custom files)
            val fromCr = runCatching {
                contentResolver.query(
                    currentUri,
                    arrayOf(MediaStore.Audio.Media.TITLE),
                    null, null, null
                )?.use { c ->
                    if (c.moveToFirst()) {
                        val idx = c.getColumnIndex(MediaStore.Audio.Media.TITLE)
                        if (idx >= 0) c.getString(idx) else null
                    } else null
                }
            }.getOrNull()
            // Fallback: RingtoneManager title (works for built-in system ringtones)
            fromCr?.takeIf { it.isNotBlank() }
                ?: try {
                    RingtoneManager.getRingtone(this, currentUri)?.getTitle(this)
                } catch (e: SecurityException) {
                    null
                }
                ?: "Default"
        } else "Default"
        binding.tvRingtoneName.text = name
    }

    /**
     * Copy the user-picked audio file into MediaStore so it gets a stable
     * media:// URI and shows correct metadata, then set as default ringtone.
     */
    private fun setCustomRingtone(uri: Uri, displayName: String) {
        // 1. Try to stage the file to MediaStore first.
        // This creates a persistent URI that we can use even after returning from the settings screen,
        // as raw picker URIs often lose permission when navigating to Settings.
        var stagedUri: Uri? = null
        try {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, "audio/mpeg")
                put(MediaStore.Audio.Media.IS_RINGTONE, true)
                put(MediaStore.Audio.Media.IS_MUSIC, false)
            }
            stagedUri = contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
            if (stagedUri != null) {
                contentResolver.openOutputStream(stagedUri)?.use { out ->
                    contentResolver.openInputStream(uri)?.use { input -> input.copyTo(out) }
                }
            }
        } catch (e: Exception) {
            // If staging fails, we'll attempt to use the original URI as a fallback
        }

        val ringtoneUri = stagedUri ?: uri

        if (!Settings.System.canWrite(this)) {
            pendingRingtoneUri = ringtoneUri
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
            Toast.makeText(this, "Please allow 'Modify system settings' and come back", Toast.LENGTH_LONG).show()
            return
        }

        try {
            RingtoneManager.setActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE, ringtoneUri)
            Toast.makeText(this, "Ringtone set: $displayName", Toast.LENGTH_SHORT).show()
            pendingRingtoneUri = null
            // Reload list — custom ringtone won't match any system ringtone, so selection clears
            loadSystemRingtones()
            adapter.clearSelection()
            selectedRingtone = null
            displayCurrentRingtone()
        } catch (e: SecurityException) {
            // Even if canWrite() returned true, we might hit this on some devices
            pendingRingtoneUri = ringtoneUri
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
            Toast.makeText(this, "Please ensure 'Modify system settings' is enabled", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
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
                mediaPlayer?.start()
                model.isPlaying = true
            }
            adapter.notifyPlayStateChanged(position)
        } else {
            stopAllPlayback()
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(this, model.uri)
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
        } catch (_: Exception) { }
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
            null
        } ?: run {
                Toast.makeText(this, "No ringtone found or permission missing", Toast.LENGTH_SHORT).show()
                return
            }

        try {
            mediaPlayer?.release()
            val mp = MediaPlayer()

            // Try FileDescriptor first — works for content:// URIs (custom files)
            val pfd = runCatching { contentResolver.openFileDescriptor(currentUri, "r") }.getOrNull()
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
            binding.ivPlay.setImageResource(R.drawable.ic_fav)

            mp.setOnCompletionListener {
                binding.ivPlay.setImageResource(R.drawable.ic_play)
                currentPlayingPosition = -1
            }
        } catch (e: Exception) {
            // Ultimate fallback
            try {
                mediaPlayer?.release()
                mediaPlayer = MediaPlayer.create(this, currentUri)
                mediaPlayer?.start()
                currentPlayingPosition = -2
                binding.ivPlay.setImageResource(R.drawable.ic_fav)
                mediaPlayer?.setOnCompletionListener {
                    binding.ivPlay.setImageResource(R.drawable.ic_play)
                    currentPlayingPosition = -1
                }
            } catch (ex: Exception) {
                Toast.makeText(this, "Cannot play ringtone", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setSystemRingtone(uri: Uri) {
        if (!Settings.System.canWrite(this)) {
            pendingRingtoneUri = uri
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
            Toast.makeText(this, "Please allow 'Modify system settings' and come back", Toast.LENGTH_LONG).show()
            return
        }

        try {
            RingtoneManager.setActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE, uri)
            Toast.makeText(this, "Ringtone set successfully", Toast.LENGTH_SHORT).show()
            pendingRingtoneUri = null
            displayCurrentRingtone()
        } catch (e: SecurityException) {
            pendingRingtoneUri = uri
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
            Toast.makeText(this, "Please ensure 'Modify system settings' is enabled", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to set ringtone: ${e.message}", Toast.LENGTH_SHORT).show()
            pendingRingtoneUri = null
        }
    }

    override fun onResume() {
        super.onResume()
        pendingRingtoneUri?.let { pending ->
            if (Settings.System.canWrite(this)) {
                // If the URI is already from MediaStore (system or staged), set it directly.
                // Otherwise, treat it as a custom file that might need staging.
                val isStagedOrSystem = pending.authority == "media"
                if (isStagedOrSystem) {
                    setSystemRingtone(pending)
                } else {
                    val name = getDisplayNameFromUri(pending)
                    setCustomRingtone(pending, name)
                }
            }
        }
    }

    override fun onClick(view: View) {
        when (view.id) {
            binding.cvDone.id -> {
                val selected = selectedRingtone
                if (selected == null) {
                    Toast.makeText(this, "Please select a ringtone", Toast.LENGTH_SHORT).show()
                    return
                }
                val currentDefault = try {
                    RingtoneManager.getActualDefaultRingtoneUri(
                        this, RingtoneManager.TYPE_RINGTONE
                    )
                } catch (e: SecurityException) {
                    null
                }
                if (selected.uri.toString() == currentDefault?.toString()) {
                    Toast.makeText(this, "This ringtone is already set", Toast.LENGTH_SHORT).show()
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