package com.phonecall.dialcontacts.calldialer.activities.blockNumbers

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import com.phonecall.dialcontacts.calldialer.adapters.BlockNumberAdapter
import com.phonecall.dialcontacts.calldialer.databinding.ActivityBlockNumbersBinding
import com.phonecall.dialcontacts.calldialer.models.BlockModel
import com.phonecall.dialcontacts.calldialer.utils.Common
import com.phonecall.dialcontacts.calldialer.utils.Common.isValidClick
import com.phonecall.dialcontacts.calldialer.utils.OnClickHandler
import com.phonecall.dialcontacts.calldialer.utils.PermissionManager
import com.phonecall.dialcontacts.calldialer.viewmodels.BlockViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BlockNumbersActivity : AppCompatActivity(), OnClickHandler {
    private lateinit var binding: ActivityBlockNumbersBinding
    private lateinit var adapter: BlockNumberAdapter
    private val viewModel: BlockViewModel by viewModels()

    private val defaultDialerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            checkDefaultDialer()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_block_numbers)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        Common.hideSystemUI(this)
        initView()
        observeData()
        loadAds()
        checkDefaultDialer()
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

    private fun checkDefaultDialer() {
        if (!PermissionManager.isDefaultDialer(this)) {
            Common.ensureDefaultDialer(this, onProceed = {
                val intent = PermissionManager.getDefaultDialerIntent(this)
                if (intent != null) {
                    defaultDialerLauncher.launch(intent)
                }
            })
        } else {
            viewModel.refresh()
        }
    }


    private fun initView() {
        binding.onClickHandler = this

        adapter = BlockNumberAdapter { blockModel ->
            showUnblockDialog(blockModel)
        }
        binding.rvBlockNumbers.adapter = adapter
        binding.rvBlockNumbers.layoutManager = LinearLayoutManager(this)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                loadInterAd()
            }
        })
    }

    private fun observeData() {
        lifecycleScope.launch {
            viewModel.allBlockedNumbers.collect { list ->
                val isEmpty = list.isEmpty()
                adapter.setBlockList(list)

                binding.llBlockSpaceHolder.visibility = if (isEmpty) View.VISIBLE else View.GONE
                binding.cvBlockNumbers.visibility = if (isEmpty) View.GONE else View.VISIBLE
            }
        }
    }

    fun loadInterAd() {
        ADSInterDisplayClick.ADSBackDisplayInterstitial(
            this@BlockNumbersActivity,
            ADSMainClass.getStringValue(ADSMainClass.INTER_SECOND_TIME),
            { _ ->
                finish()
            })
    }

    private fun showUnblockDialog(blockModel: BlockModel) {
        if (!PermissionManager.isDefaultDialer(this)) {
            Common.ensureDefaultDialer(this, onProceed = {
                val intent = PermissionManager.getDefaultDialerIntent(this)
                if (intent != null) {
                    defaultDialerLauncher.launch(intent)
                }
            })
            return
        }

        Common.alertDialog(
            context = this,
            title = getString(R.string.unblock_contact),
            description = getString(R.string.you_will_be_able_to_receive_call),
            btnOkay = getString(R.string.unblock),
            isImageVisible = true,
            onItemClick = {
                viewModel.unblockNumber(blockModel.phoneNumber)
            })
    }

    override fun onClick(view: View) {
        if (!isValidClick()) return
        when (view.id) {
            binding.ivBack.id -> {
                onBackPressedDispatcher.onBackPressed()
            }

            binding.cvAdd.id -> {
                Common.ensureDefaultDialer(this, onProceed = {
                    Common.setBlockNumbersDialog(this, onItemClick = { number ->
                        viewModel.blockNumber(number)
                    })
                })
            }
        }
    }
}