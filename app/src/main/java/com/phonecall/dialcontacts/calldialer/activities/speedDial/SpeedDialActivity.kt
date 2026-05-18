package com.phonecall.dialcontacts.calldialer.activities.speedDial

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.phonecall.dialcontacts.calldialer.Advertisement.ADSBannerSmall
import com.phonecall.dialcontacts.calldialer.Advertisement.ADSMainClass
import com.phonecall.dialcontacts.calldialer.Advertisement.ADSNativeDisplay
import com.phonecall.dialcontacts.calldialer.R
import com.phonecall.dialcontacts.calldialer.activities.speedDialSelect.SpeedDialSelectActivity
import com.phonecall.dialcontacts.calldialer.databinding.ActivitySpeedDialBinding
import com.phonecall.dialcontacts.calldialer.models.SpeedDialModel
import com.phonecall.dialcontacts.calldialer.utils.Common
import com.phonecall.dialcontacts.calldialer.utils.Common.isValidClick
import com.phonecall.dialcontacts.calldialer.utils.Constance
import com.phonecall.dialcontacts.calldialer.utils.OnClickHandler
import com.phonecall.dialcontacts.calldialer.viewmodels.SpeedDialViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SpeedDialActivity : AppCompatActivity(), OnClickHandler {
    private lateinit var binding: ActivitySpeedDialBinding
    private val viewModel: SpeedDialViewModel by viewModels()
    private var currentSlot: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_speed_dial)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        Common.hideSystemUI(this)
        initView()
        loadAds()
    }

    private val resultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val number = data?.getStringExtra(Constance.DATA_FETCH)
            val name = data?.getStringExtra("NAME") ?: getString(R.string.unknown)
            val photoUri = data?.getStringExtra("PHOTO_URI")

            if (number != null && currentSlot != -1) {
                viewModel.insertSpeedDial(currentSlot, name, number, photoUri)
            }
        }
    }

    private fun loadAds() {
        if (ADSMainClass.getOtherAdsShow()) {
            if (ADSMainClass.getOtherAdsType().equals("native")) {
                ADSNativeDisplay.loadAdmobNativeAdBig(
                    ADSMainClass.getStringValue(ADSMainClass.OTHER_SCREEN_NATIVE),
                    findViewById(R.id.flNativeSmallPlaceholder),
                    findViewById(R.id.shimmer_container_banner),
                    "small",
                    this
                )
            } else {
                ADSBannerSmall.loadAdMobBanner(
                    ADSMainClass.getStringValue(ADSMainClass.OTHER_SCREEN_BANNER),
                    findViewById(R.id.flBannerSmallPlaceholder),
                    findViewById(R.id.shimmer_container_banner),
                    this
                )
            }
        } else {
            findViewById<View>(R.id.shimmer_container_banner).visibility = View.GONE
            findViewById<View>(R.id.flNativeSmallPlaceholder).visibility = View.GONE
            findViewById<View>(R.id.flBannerSmallPlaceholder).visibility = View.GONE
        }
    }

    private fun initView() {
        binding.onClickHandler = this

        lifecycleScope.launch {
            viewModel.speedDialList.collectLatest { speedDials ->
                updateSpeedDialUI(speedDials)
            }
        }
    }

    private fun updateSpeedDialUI(speedDials: List<SpeedDialModel>) {
        val unselectViews = listOf(
            binding.ll1Unselect, binding.ll2Unselect, binding.ll3Unselect,
            binding.ll4Unselect, binding.ll5Unselect, binding.ll6Unselect,
            binding.ll7Unselect, binding.ll8Unselect, binding.ll9Unselect
        )
        val selectViews = listOf(
            binding.ll1Select, binding.ll2Select, binding.ll3Select,
            binding.ll4Select, binding.ll5Select, binding.ll6Select,
            binding.ll7Select, binding.ll8Select, binding.ll9Select
        )
        val nameTextViews = listOf(
            binding.tv1Name, binding.tv2Name, binding.tv3Name,
            binding.tv4Name, binding.tv5Name, binding.tv6Name,
            binding.tv7Name, binding.tv8Name, binding.tv9Name
        )

        val setBackgroundView = listOf(
            binding.ll1View, binding.ll2View, binding.ll3View,
            binding.ll4View, binding.ll5View, binding.ll6View,
            binding.ll7View, binding.ll8View, binding.ll9View
        )


        for (i in 0 until 9) {
            val slot = i + 1
            val speedDial = speedDials.find { it.slot == slot }
            if (speedDial != null) {
                unselectViews[i].visibility = View.GONE
                selectViews[i].visibility = View.VISIBLE
                nameTextViews[i].text = speedDial.contactName
                setBackgroundView[i].setBackgroundResource(R.drawable.speed_dial_selected_bg)

            } else {
                unselectViews[i].visibility = View.VISIBLE
                selectViews[i].visibility = View.GONE
                setBackgroundView[i].setBackgroundResource(R.drawable.speed_dial_unselected_bg)
            }
        }
    }

    override fun onClick(view: View) {
        if (!isValidClick()) return
        when (view.id) {
            binding.ll1Unselect.id -> openSpeedDialSelect(1)
            binding.ll2Unselect.id -> openSpeedDialSelect(2)
            binding.ll3Unselect.id -> openSpeedDialSelect(3)
            binding.ll4Unselect.id -> openSpeedDialSelect(4)
            binding.ll5Unselect.id -> openSpeedDialSelect(5)
            binding.ll6Unselect.id -> openSpeedDialSelect(6)
            binding.ll7Unselect.id -> openSpeedDialSelect(7)
            binding.ll8Unselect.id -> openSpeedDialSelect(8)
            binding.ll9Unselect.id -> openSpeedDialSelect(9)

            binding.iv1Close.id -> viewModel.deleteSpeedDial(1)
            binding.iv2Close.id -> viewModel.deleteSpeedDial(2)
            binding.iv3Close.id -> viewModel.deleteSpeedDial(3)
            binding.iv4Close.id -> viewModel.deleteSpeedDial(4)
            binding.iv5Close.id -> viewModel.deleteSpeedDial(5)
            binding.iv6Close.id -> viewModel.deleteSpeedDial(6)
            binding.iv7Close.id -> viewModel.deleteSpeedDial(7)
            binding.iv8Close.id -> viewModel.deleteSpeedDial(8)
            binding.iv9Close.id -> viewModel.deleteSpeedDial(9)

            binding.ivBack.id -> onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun openSpeedDialSelect(slot: Int) {
        currentSlot = slot
        val intent = Intent(this, SpeedDialSelectActivity::class.java)
        resultLauncher.launch(intent)
    }
}