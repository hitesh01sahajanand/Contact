package com.phonecall.dialcontacts.calldialer.activities.quickResponse

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.phonecall.dialcontacts.calldialer.Advertisement.ADSBannerSmall
import com.phonecall.dialcontacts.calldialer.Advertisement.ADSInterDisplayClick
import com.phonecall.dialcontacts.calldialer.Advertisement.ADSMainClass
import com.phonecall.dialcontacts.calldialer.Advertisement.ADSNativeDisplay
import com.phonecall.dialcontacts.calldialer.Advertisement.ADSUtilitis
import com.phonecall.dialcontacts.calldialer.R
import com.phonecall.dialcontacts.calldialer.adapters.QuickResponseAdapter
import com.phonecall.dialcontacts.calldialer.databinding.ActivityQuickResponseBinding
import com.phonecall.dialcontacts.calldialer.utils.Common
import com.phonecall.dialcontacts.calldialer.utils.Common.isValidClick
import com.phonecall.dialcontacts.calldialer.utils.Constance
import com.phonecall.dialcontacts.calldialer.utils.OnClickHandler
import com.phonecall.dialcontacts.calldialer.viewmodels.QuickResponseViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class QuickResponseActivity : AppCompatActivity(), OnClickHandler {
    private lateinit var binding: ActivityQuickResponseBinding
    private lateinit var adapter: QuickResponseAdapter
    private val viewModel: QuickResponseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_quick_response)
        Common.setStableStatusBarInsets(findViewById(R.id.main))
        Common.hideSystemUI(this)

        viewModel.refreshForLocaleChange()
        initView()
        observeData()
        loadAds()
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

    fun loadInterAd() {
        ADSInterDisplayClick.ADSBackDisplayInterstitial(
            this@QuickResponseActivity,
            ADSMainClass.getStringValue(ADSMainClass.INTER_SECOND_TIME),
            { _ ->
                finish()
            })
    }

    private fun observeData() {
        lifecycleScope.launch {
            viewModel.messages.collectLatest { list ->
                adapter.updateData(list)
            }
        }
    }

    private fun initView() {
        binding.onClickHandler = this

        adapter = QuickResponseAdapter(onItemClick = { model, action ->
            if (action == Constance.ACTION_DELETE) {
                viewModel.deleteMessageById(model.id)
            }
        })

        binding.rvQuickResponse.adapter = adapter
        binding.rvQuickResponse.layoutManager = LinearLayoutManager(this)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                loadInterAd()
            }
        })
    }

    override fun onClick(view: View) {
        if (!isValidClick()) return
        when (view.id) {
            binding.ivBack.id -> {
                onBackPressedDispatcher.onBackPressed()
            }

            binding.llQuickResponse.id -> {
                Common.editQuickMessageDialog(this, onItemClick = { message ->
                    if (message.isNotBlank()) {
                        viewModel.insertMessage(message.trim())
                    }
                })
            }
        }
    }
}