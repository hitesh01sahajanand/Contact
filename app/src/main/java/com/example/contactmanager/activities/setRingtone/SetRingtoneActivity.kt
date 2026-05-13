package com.example.contactmanager.activities.setRingtone

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Process
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.provider.Settings
import android.provider.ContactsContract
import android.util.Log
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.Toast
import java.io.File
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.contactmanager.R
import com.example.contactmanager.adapters.RingtonesAdapter
import com.example.contactmanager.databinding.ActivitySetRingtoneBinding
import com.example.contactmanager.models.RingtoneModel
import com.example.contactmanager.utils.Common.isValidClick
import com.example.contactmanager.utils.Constance
import com.example.contactmanager.utils.OnClickHandler

class SetRingtoneActivity : AppCompatActivity(), OnClickHandler {
    private lateinit var binding: ActivitySetRingtoneBinding
    private lateinit var adapter: RingtonesAdapter
    private var ringtoneList = mutableListOf<RingtoneModel>()
    private var mediaPlayer: MediaPlayer? = null
    private var selectedRingtone: RingtoneModel? = null
    private var currentPlayingPosition = -1
    private var pendingRingtoneUri: Uri? = null
    private var contactId: String? = null

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
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val allGranted = result.values.all { it }
            if (allGranted) {
                displayCurrentRingtone()
                loadSystemRingtones()
            } else {
                // If at least the READ permission is granted, we can still load ringtones
                val readGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    result[Manifest.permission.READ_MEDIA_AUDIO] == true
                } else {
                    result[Manifest.permission.READ_EXTERNAL_STORAGE] == true
                }

                if (readGranted) {
                    displayCurrentRingtone()
                    loadSystemRingtones()
                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.permission_required_to_read_ringtone_details),
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.tvRingtoneName.text = "Default"
                }
            }
        }

    private fun checkAndRequestAudioPermission(): Boolean {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            // For Android 9 and below, we also need WRITE to set custom ringtones
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) !=
                    PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        return if (missingPermissions.isEmpty()) {
            true
        } else {
            requestAudioPermissionLauncher.launch(missingPermissions)
            false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        contactId = intent.getStringExtra(Constance.CONTACT_ID)

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
        updateHeaderTitle()
    }

    private fun updateHeaderTitle() {
        if (contactId != null) {
            val name = getContactNameById(contactId!!)
            if (!name.isNullOrEmpty()) {
                binding.tvHeaderTitle.text = "${getString(R.string.set_ringtone)} for $name"
            }
        }
    }

    private fun getContactNameById(id: String): String? {
        val projection = arrayOf(ContactsContract.Contacts.DISPLAY_NAME)
        val selection = "${ContactsContract.Contacts._ID} = ?"
        val selectionArgs = arrayOf(id)

        return try {
            contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(0)
                } else null
            }
        } catch (_: Exception) {
            null
        }
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
        val currentUri = if (contactId != null) {
            getContactRingtoneUri(contactId!!) ?: try {
                RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE)
            } catch (_: SecurityException) {
                null
            }
        } else {
            try {
                RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE)
            } catch (e: SecurityException) {
                Log.e("TAG", "loadSystemRingtones: ${e.message}")
                null
            }
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
        return getString(R.string.custom_ringtone).takeIf { it.isNotBlank() }
            ?: getString(R.string.custom_ringtone)
    }

    private fun displayCurrentRingtone() {
        val currentUri = if (contactId != null) {
            getContactRingtoneUri(contactId!!) ?: try {
                RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE)
            } catch (_: SecurityException) {
                null
            }
        } else {
            try {
                RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE)
            } catch (e: SecurityException) {
                Log.e("TAG", "displayCurrentRingtone: ${e.message}")
                null
            }
        }

        if (currentUri == null) {
            binding.tvRingtoneName.text = getString(R.string.default_)
            return
        }

        val hasUriPermission = checkUriPermission(
            currentUri,
            Process.myPid(),
            Process.myUid(),
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED

        var name: String?
        name = if (hasUriPermission || currentUri.authority != "media") {
            queryNameFromContentResolver(currentUri)
        } else {
            if (checkAndRequestAudioPermission()) {
                queryNameFromContentResolver(currentUri)
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
        } catch (_: Exception) {
            null
        }
    }

    private fun getContactRingtoneUri(id: String): Uri? {
        val projection = arrayOf(ContactsContract.Contacts.CUSTOM_RINGTONE)
        val selection = "${ContactsContract.Contacts._ID} = ?"
        val selectionArgs = arrayOf(id)

        return try {
            contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val ringtoneStr = cursor.getString(0)
                    if (!ringtoneStr.isNullOrEmpty()) Uri.parse(ringtoneStr) else null
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun setCustomRingtone(uri: Uri, displayName: String) {

        val safeName = displayName.trim().replace("[\\\\/:*?\"<>|]".toRegex(), "_")
            .takeIf { it.isNotBlank() }
            ?: getString(R.string.custom_ringtone).takeIf { it.isNotBlank() }
            ?: "Custom Ringtone"

        val mimeType = contentResolver.getType(uri)
            ?.takeIf { it.isNotBlank() }
            ?: "audio/mpeg"

        var stagedUri: Uri? = null

        try {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, safeName)
                put(MediaStore.MediaColumns.TITLE, safeName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)

                put(MediaStore.Audio.Media.IS_RINGTONE, true)
                put(MediaStore.Audio.Media.IS_MUSIC, false)
                put(MediaStore.Audio.Media.IS_NOTIFICATION, false)
                put(MediaStore.Audio.Media.IS_ALARM, false)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_RINGTONES)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                } else {
                    // For Android 9 and below, we need to provide a file path via the DATA column
                    @Suppress("DEPRECATION")
                    val ringtoneDir =
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RINGTONES)
                    if (!ringtoneDir.exists()) ringtoneDir.mkdirs()

                    val extension =
                        MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "mp3"
                    var file = File(ringtoneDir, "$safeName.$extension")

                    // Avoid overwriting if possible or just use a unique name
                    if (file.exists()) {
                        file = File(
                            ringtoneDir,
                            "${safeName}_${System.currentTimeMillis()}.$extension"
                        )
                    }
                    put(MediaStore.MediaColumns.DATA, file.absolutePath)
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
                getString(R.string.could_not_prepare_the_audio_file_please_try_a_different_file),
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // ✅ Extra safety check
        val ringtone = RingtoneManager.getRingtone(this, stagedUri)
        if (ringtone == null) {
            Toast.makeText(this, getString(R.string.invalid_ringtone_file), Toast.LENGTH_SHORT)
                .show()
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
            setSystemRingtone(stagedUri)
        } catch (e: Exception) {
            Log.e("SetRingtone", "Error: ${e.message}")
            Toast.makeText(
                this,
                getString(R.string.failed_to_set_ringtone, e.message), Toast.LENGTH_SHORT
            ).show()
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
        val currentUri = if (contactId != null) {
            getContactRingtoneUri(contactId!!) ?: try {
                RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE)
            } catch (_: SecurityException) {
                null
            }
        } else {
            try {
                RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE)
            } catch (e: SecurityException) {
                Log.e("TAG", "handleTopPlayPause: ${e.message}")
                null
            }
        }

        if (currentUri == null) {
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
        if (contactId == null && !Settings.System.canWrite(this)) {
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
            if (contactId != null) {
                val values = ContentValues()
                values.put(ContactsContract.Contacts.CUSTOM_RINGTONE, uri.toString())
                contentResolver.update(
                    ContactsContract.Contacts.CONTENT_URI,
                    values,
                    "${ContactsContract.Contacts._ID}=?",
                    arrayOf(contactId)
                )
                Toast.makeText(
                    this,
                    getString(R.string.ringtone_set_successfully),
                    Toast.LENGTH_SHORT
                )
                    .show()
            } else {
                RingtoneManager.setActualDefaultRingtoneUri(
                    this,
                    RingtoneManager.TYPE_RINGTONE,
                    uri
                )
                Toast.makeText(
                    this,
                    getString(R.string.ringtone_set_successfully),
                    Toast.LENGTH_SHORT
                )
                    .show()
            }
            pendingRingtoneUri = null
            displayCurrentRingtone()
            loadSystemRingtones()
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
            Log.e("TAG", "setSystemRingtone: ${e.message}")
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
        if (!isValidClick()) return
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
                val currentDefault = if (contactId != null) {
                    getContactRingtoneUri(contactId!!) ?: try {
                        RingtoneManager.getActualDefaultRingtoneUri(
                            this,
                            RingtoneManager.TYPE_RINGTONE
                        )
                    } catch (_: SecurityException) {
                        null
                    }
                } else {
                    try {
                        RingtoneManager.getActualDefaultRingtoneUri(
                            this, RingtoneManager.TYPE_RINGTONE
                        )
                    } catch (e: SecurityException) {
                        Log.e("TAG", "onClick: ${e.message}")
                        null
                    }
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
                if (checkAndRequestAudioPermission()) {
                    pickRingtoneLauncher.launch("audio/*")
                }
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