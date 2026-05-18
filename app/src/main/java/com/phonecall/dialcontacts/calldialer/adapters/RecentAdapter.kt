package com.phonecall.dialcontacts.calldialer.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.phonecall.dialcontacts.calldialer.R
import com.phonecall.dialcontacts.calldialer.databinding.DateHeaderDesignBinding
import com.phonecall.dialcontacts.calldialer.databinding.RecentsDesignBinding
import com.phonecall.dialcontacts.calldialer.models.CallHistoryListItems
import com.phonecall.dialcontacts.calldialer.models.CallLogEntry
import com.phonecall.dialcontacts.calldialer.utils.Common
import com.phonecall.dialcontacts.calldialer.utils.Common.isValidClick
import com.phonecall.dialcontacts.calldialer.utils.Constance
import com.phonecall.dialcontacts.calldialer.utils.SharedPreferenceManager

class RecentAdapter(
    private val onClickCall: (CallLogEntry, String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_CONTACT = 1
        private const val TYPE_LOADER = 2
    }

    private var expandedPosition = -1
    var isSelectionMode = false
    private val selectedEntries = mutableSetOf<CallLogEntry>()

    var onSelectionModeChanged: ((Boolean) -> Unit)? = null
    var onSelectionCountChanged: ((Int) -> Unit)? = null

    private val originalList = ArrayList<CallHistoryListItems>()
    private var filteredList = ArrayList<CallHistoryListItems>()

    override fun getItemViewType(position: Int): Int {
        return when (filteredList[position]) {
            is CallHistoryListItems.Header -> TYPE_HEADER
            is CallHistoryListItems.Contact -> TYPE_CONTACT
            is CallHistoryListItems.Loader -> TYPE_LOADER
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        return when (viewType) {

            TYPE_HEADER -> {
                val binding = DateHeaderDesignBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                HeaderViewHolder(binding)
            }

            TYPE_LOADER -> {
                val view = LayoutInflater.from(parent.context).inflate(
                    R.layout.loader_design, parent, false
                )
                LoaderViewHolder(view)
            }

            else -> {
                val binding = RecentsDesignBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                ContactViewHolder(binding)
            }
        }
    }

    override fun getItemCount(): Int = filteredList.size


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        when (val item = filteredList[position]) {

            is CallHistoryListItems.Header -> {
                (holder as HeaderViewHolder).bind(item)
            }

            is CallHistoryListItems.Contact -> {
                (holder as ContactViewHolder).bind(item, position)
            }

            is CallHistoryListItems.Loader -> {
                // No binding needed for loader
            }
        }
    }

    // 🔹 Submit list
    fun submitList(newList: List<CallHistoryListItems>) {
        filteredList.clear()
        filteredList.addAll(newList)
        originalList.clear()
        originalList.addAll(newList)
        notifyDataSetChanged()
    }

    fun showLoader() {
        if (filteredList.lastOrNull() !is CallHistoryListItems.Loader) {
            filteredList.add(CallHistoryListItems.Loader)
            notifyItemInserted(filteredList.size - 1)
        }
    }

    fun hideLoader() {
        if (filteredList.lastOrNull() is CallHistoryListItems.Loader) {
            val position = filteredList.size - 1
            filteredList.removeAt(position)
            notifyItemRemoved(position)
        }
    }


    fun filter(query: String) {

        val search = query.trim()

        if (search.isEmpty()) {
            filteredList = ArrayList(originalList)
        } else {

            val tempList = ArrayList<CallHistoryListItems>()
            var currentHeader: CallHistoryListItems.Header? = null

            for (item in originalList) {

                when (item) {

                    is CallHistoryListItems.Header -> {
                        currentHeader = item
                    }

                    is CallHistoryListItems.Contact -> {

                        val data = item.data

                        val name = data.stringCallName ?: ""
                        val number = data.stringNumber ?: ""

                        if (name.contains(search, true) || number.contains(search, true)) {

                            // ✅ HEADER ADD (ONLY ONCE)
                            currentHeader?.let { header ->
                                if (!tempList.contains(header)) {
                                    tempList.add(header)
                                }
                            }

                            // ✅ CONTACT ADD
                            tempList.add(item)
                        }
                    }

                    else -> {

                    }
                }
            }

            filteredList = tempList
        }

        notifyDataSetChanged()
    }

    fun getCurrentList(): List<CallHistoryListItems> {
        return filteredList
    }

    // 🧩 HEADER VIEW HOLDER
    class HeaderViewHolder(private val binding: DateHeaderDesignBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CallHistoryListItems.Header) {
            binding.tvDate.text = item.title
        }
    }

    // 📞 CONTACT VIEW HOLDER
    inner class ContactViewHolder(private val binding: RecentsDesignBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("UseCompatLoadingForDrawables")
        fun bind(item: CallHistoryListItems.Contact, position: Int) {
            val data = item.data

            val context = binding.root.context
            val isExpanded = position == expandedPosition

            binding.llCollapseView.visibility = if (isExpanded) View.GONE else View.VISIBLE
            binding.llExpandedView.visibility = if (isExpanded) View.VISIBLE else View.GONE

            val isSaved = !data.contactId.isNullOrEmpty()
            binding.llNotSavedContact.isVisible = !isSaved
            binding.ivCallInfo.isVisible = isSaved
            binding.ivCallHistory.isVisible = !isSaved

            // 🔥 Check neighbors (ignore headers)
            val isNextExpanded = position + 1 == expandedPosition
            val isPrevExpanded = position - 1 == expandedPosition

            val isFirst =
                position == 0 || filteredList[position - 1] is CallHistoryListItems.Header || isPrevExpanded
            val isLast = position == filteredList.size - 1 ||
                    filteredList.getOrNull(position + 1) is CallHistoryListItems.Header ||
                    filteredList.getOrNull(position + 1) is CallHistoryListItems.Loader ||
                    isNextExpanded

            val backgroundRes = when {
                isExpanded -> R.drawable.bg_all_rounded
                isFirst && isLast -> R.drawable.bg_all_rounded
                isFirst -> R.drawable.bg_top_rounded
                isLast -> R.drawable.bg_bottom_rounded
                else -> R.drawable.bg_middle
            }

            val isSelected = isSelectionMode && selectedEntries.contains(data)
            binding.llMainView.setBackgroundResource(backgroundRes)
            if (isSelected) {
                binding.llMainView.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.border_color_2))
            } else {
                binding.llMainView.backgroundTintList = null
            }

            val params = binding.root.layoutParams as RecyclerView.LayoutParams
            val vertical = context.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._10sdp)

            if (isExpanded) {
                binding.viewSep.isVisible = false
                params.setMargins(0, vertical, 0, vertical)
            } else {
                // Separator should be hidden if this is the last in its visual group (includes if next is expanded)
                binding.viewSep.isVisible = !isLast
                params.setMargins(0, 0, 0, 0)
            }

            binding.root.layoutParams = params

            binding.cbSelect.isVisible = isSelectionMode
            binding.cvCallType.visibility = if (!isSelectionMode) View.VISIBLE else View.INVISIBLE
            binding.cbSelect.isChecked = selectedEntries.contains(data)
            binding.cbSelect.isClickable = false
            binding.cbSelect.isFocusable = false

            binding.llMainView.setOnLongClickListener {
                if (!isSelectionMode) {
                    isSelectionMode = true
                    expandedPosition = -1
                    selectedEntries.add(data)
                    onSelectionModeChanged?.invoke(true)
                    onSelectionCountChanged?.invoke(selectedEntries.size)
                    notifyDataSetChanged()
                }
                true
            }

            binding.llMainView.setOnClickListener {
                if (isSelectionMode) {
                    if (selectedEntries.contains(data)) {
                        selectedEntries.remove(data)
                    } else {
                        selectedEntries.add(data)
                    }
                    onSelectionCountChanged?.invoke(selectedEntries.size)
                    notifyItemChanged(position)

                    if (selectedEntries.isEmpty()) {
                        isSelectionMode = false
                        onSelectionModeChanged?.invoke(false)
                        notifyDataSetChanged()
                    }
                } else {
                    val previousPosition = expandedPosition
                    expandedPosition = if (isExpanded) -1 else position

                    // Notify all affected items
                    val itemsToNotify = mutableSetOf<Int>()
                    if (previousPosition != -1) {
                        itemsToNotify.add(previousPosition)
                        itemsToNotify.add(previousPosition - 1)
                        itemsToNotify.add(previousPosition + 1)
                    }
                    itemsToNotify.add(position)
                    itemsToNotify.add(position - 1)
                    itemsToNotify.add(position + 1)

                    itemsToNotify.forEach { pos ->
                        if (pos in 0 until itemCount) {
                            notifyItemChanged(pos)
                        }
                    }
                }
            }

            binding.run {

                ivCall.setOnClickListener {
                    if (!isValidClick()) return@setOnClickListener
                    onClickCall(data, Constance.ACTION_CALL)
                }

                ivMessage.setOnClickListener {
                    if (!isValidClick()) return@setOnClickListener
                    onClickCall(data, Constance.ACTION_SEND_MESSAGE)
                }

                ivVideoCall.setOnClickListener {
                    if (!isValidClick()) return@setOnClickListener
                    onClickCall(data, Constance.ACTION_VIDEO_CALL)
                }

                ivCallInfo.setOnClickListener {
                    if (!isValidClick()) return@setOnClickListener
                    onClickCall(data, Constance.ACTION_INFO)
                }

                ivCallHistory.setOnClickListener {
                    if (!isValidClick()) return@setOnClickListener
                    onClickCall(data, Constance.ACTION_CALL_HISTORY)
                }

                cvAddToContact.setOnClickListener {
                    if (!isValidClick()) return@setOnClickListener
                    onClickCall(data, Constance.ACTION_ADD_TO_CONTACT)
                }

                cvAddTag.setOnClickListener {
                    if (!isValidClick()) return@setOnClickListener
                    onClickCall(data, Constance.ACTION_ADD_TAG)
                }

                llBlockContact.setOnClickListener {
                    if (!isValidClick()) return@setOnClickListener
                    onClickCall(data, Constance.ACTION_BLOCK_CONTACT)
                }


                val name = if (data.callCount > 1) {
                    "${data.stringCallName ?: data.stringNumber} (${data.callCount})"
                } else {
                    data.stringCallName ?: data.stringNumber
                }

                tvExpandedCallType.text = Common.getCallType(binding.root.context, data.intType)

                tvCollapseName.text = name
                tvExpandedName.text = name

                tvCollapseTime.text = Common.extractTimeFromDate(data.dateData.toString())
                tvExpandedTime.text = Common.extractTimeFromDate(data.dateData.toString())
                if (data.isBlocked) {
                    ivExpandedCallType.setImageDrawable(context.getDrawable(R.drawable.ic_block))
                    ivCollapseCallType.setImageDrawable(context.getDrawable(R.drawable.ic_block))
                } else {
                    ivExpandedCallType.setImageDrawable(
                        Common.getCallImageType(
                            data.intType,
                            root.context
                        )
                    )

                    ivCollapseCallType.setImageDrawable(
                        Common.getCallImageType(
                            data.intType,
                            root.context
                        )
                    )
                }

                val simLabel = Common.getSimLabel(context, data.simId)
                tvSimNumber.text = simLabel
                tvSimNumber.isVisible = simLabel.isNotEmpty()


                tvBlockText.text =
                    if (data.isBlocked) root.context.getString(R.string.unblock) else root.context.getString(
                        R.string.block_contacts
                    )

                // 🖼️ Profile Logic
                if (data.stringPhotoUri.isNullOrEmpty()) {

                    if (data.stringCallName.isNullOrEmpty()) {
                        tvExpandedContactName.isVisible = false
                        ivExpandedContactPhoto.isVisible = false
                        ivExpandedUser.isVisible = true

                        val color = Common.profileColors[position % Common.profileColors.size]
                        cvExpandedProfile.setCardBackgroundColor(
                            ContextCompat.getColor(root.context, color)
                        )

                    } else {
                        tvExpandedContactName.isVisible = true
                        ivExpandedContactPhoto.isVisible = false
                        ivExpandedUser.isVisible = false

                        val color = Common.profileColors[position % Common.profileColors.size]
                        cvExpandedProfile.setCardBackgroundColor(
                            ContextCompat.getColor(root.context, color)
                        )

                        val firstChar =
                            data.stringCallName?.firstOrNull()?.uppercase() ?: ""
                        tvExpandedContactName.text = firstChar
                    }

                } else {
                    tvExpandedContactName.isVisible = false
                    ivExpandedContactPhoto.isVisible = true
                    ivExpandedUser.isVisible = false

                    Glide.with(ivExpandedContactPhoto.context)
                        .load(data.stringPhotoUri)
                        .into(ivExpandedContactPhoto)
                }
            }
        }
    }

    // ⏳ LOADER VIEW HOLDER
    class LoaderViewHolder(view: View) : RecyclerView.ViewHolder(view)

    fun getItemTouchHelper(context: Context): ItemTouchHelper {
        val swipeCallback = object :
            ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                val isSwipeEnabled =
                    SharedPreferenceManager.getBoolean(context, Constance.SWIPE_ACTION, false)
                val position = viewHolder.bindingAdapterPosition
                val isExpanded = position == expandedPosition

                if (!isSwipeEnabled || viewHolder !is ContactViewHolder || isExpanded) {
                    return makeMovementFlags(0, 0)
                }
                return super.getMovementFlags(recyclerView, viewHolder)
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) return

                val item = filteredList.getOrNull(position)
                if (item is CallHistoryListItems.Contact) {
                    if (direction == ItemTouchHelper.RIGHT) {
                        onClickCall(item.data, Constance.ACTION_CALL)
                    } else if (direction == ItemTouchHelper.LEFT) {
                        onClickCall(item.data, Constance.ACTION_SEND_MESSAGE)
                    }
                }
                notifyItemChanged(position)
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    val itemView = viewHolder.itemView
                    val paint = Paint()
                    val cornerRadius =
                        context.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._10sdp)
                            .toFloat()
                    val iconSize =
                        context.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._32sdp)
                    val horizontalMargin =
                        context.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._10sdp)

                    if (dX > 0) { // Swiping Right (Call)
                        paint.color = context.getColor(R.color.action_call_color)
                        val background = RectF(
                            itemView.left.toFloat(),
                            itemView.top.toFloat(),
                            itemView.left.toFloat() + dX,
                            itemView.bottom.toFloat()
                        )
                        c.drawRoundRect(background, cornerRadius, cornerRadius, paint)

                        val icon = ContextCompat.getDrawable(context, R.drawable.ic_call)
                        icon?.let {
                            val verticalMargin = (itemView.height - iconSize) / 2
                            val top = itemView.top + verticalMargin
                            val left = itemView.left + horizontalMargin
                            it.setBounds(left, top, left + iconSize, top + iconSize)
                            it.draw(c)
                        }
                    } else if (dX < 0) { // Swiping Left (Message)
                        paint.color = context.getColor(R.color.action_message_color)
                        val background = RectF(
                            itemView.right.toFloat() + dX,
                            itemView.top.toFloat(),
                            itemView.right.toFloat(),
                            itemView.bottom.toFloat()
                        )
                        c.drawRoundRect(background, cornerRadius, cornerRadius, paint)

                        val icon = ContextCompat.getDrawable(context, R.drawable.ic_message)
                        icon?.let {
                            val verticalMargin = (itemView.height - iconSize) / 2
                            val top = itemView.top + verticalMargin
                            val right = itemView.right - horizontalMargin
                            it.setBounds(right - iconSize, top, right, top + iconSize)
                            it.draw(c)
                        }
                    }
                }
                super.onChildDraw(
                    c,
                    recyclerView,
                    viewHolder,
                    dX,
                    dY,
                    actionState,
                    isCurrentlyActive
                )
            }
        }
        return ItemTouchHelper(swipeCallback)
    }

    fun selectAll() {
        filteredList.forEach {
            if (it is CallHistoryListItems.Contact) {
                selectedEntries.add(it.data)
            }
        }
        onSelectionCountChanged?.invoke(selectedEntries.size)
        notifyDataSetChanged()
    }

    fun deselectAll() {
        selectedEntries.clear()
        onSelectionCountChanged?.invoke(0)
        notifyDataSetChanged()
    }

    fun clearSelection() {
        isSelectionMode = false
        selectedEntries.clear()
        onSelectionModeChanged?.invoke(false)
        notifyDataSetChanged()
    }

    fun getSelectedEntries(): List<CallLogEntry> {
        return selectedEntries.toList()
    }
}
