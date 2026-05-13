package com.example.contactmanager.adapters

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.transition.ChangeBounds
import android.transition.Fade
import android.transition.TransitionManager
import android.transition.TransitionSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.contactmanager.R
import com.example.contactmanager.databinding.AllContactDesignBinding
import com.example.contactmanager.databinding.HeaderItemDesignBinding
import com.example.contactmanager.models.ContactListItem
import com.example.contactmanager.models.ContactModel
import com.example.contactmanager.utils.Common
import com.example.contactmanager.utils.Common.isValidClick
import com.example.contactmanager.utils.Constance
import com.example.contactmanager.utils.SharedPreferenceManager

class AllContactsAdapter(
    private val isAllContact: Boolean = false,
    private val onClick: (ContactModel, String) -> Unit
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val contactList = ArrayList<ContactListItem>()
    private var filteredList: MutableList<ContactListItem> = mutableListOf()
    private val initialFavoriteStatus = HashMap<String, Int>()
    private val pendingChanges = HashMap<String, Int>()

    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_CONTACT = 1
    }

    private var expandedPosition = -1
    private var isMergeDuplicate: Boolean = false
    private var currentQuery: String = ""

    fun setMergeDuplicate(merge: Boolean) {
        if (isMergeDuplicate != merge) {
            isMergeDuplicate = merge
            applyFilter()
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (filteredList[position]) {
            is ContactListItem.Header -> TYPE_HEADER
            is ContactListItem.Contact -> TYPE_CONTACT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        return if (viewType == TYPE_HEADER) {
            val binding = HeaderItemDesignBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            HeaderViewHolder(binding)
        } else {
            val binding = AllContactDesignBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            ContactViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        when (val item = filteredList[position]) {

            is ContactListItem.Header -> {
                (holder as HeaderViewHolder).bind(item)
            }

            is ContactListItem.Contact -> {
                (holder as ContactViewHolder).bind(item, position)
            }
        }


    }

    override fun getItemCount(): Int = filteredList.size

    fun addAll(newList: List<ContactListItem>) {
        contactList.clear()
        contactList.addAll(newList)

        // Store initial favorite status for all contacts (unique by ID)
        // and re-apply pending changes
        newList.filterIsInstance<ContactListItem.Contact>().forEach { contactItem ->
            val id = contactItem.data.contactId
            if (id != null) {
                if (!initialFavoriteStatus.containsKey(id)) {
                    initialFavoriteStatus[id] = contactItem.data.isFavourite
                }

                // Re-apply pending change if it exists
                if (pendingChanges.containsKey(id)) {
                    contactItem.data.isFavourite = pendingChanges[id]!!
                }
            }
        }

        applyFilter()
    }
    fun filter(query: String) {
        currentQuery = query
        applyFilter()
    }

    private fun applyFilter() {
        val searchText = currentQuery.trim()
        val tempList = mutableListOf<ContactListItem>()

        var lastHeader: ContactListItem.Header? = null
        val seenNames = mutableSetOf<String>()

        contactList.forEach { item ->
            when (item) {
                is ContactListItem.Header -> {
                    lastHeader = item
                }

                is ContactListItem.Contact -> {
                    val displayName = item.data.displayName ?: ""

                    val matchQuery = searchText.isEmpty() ||
                            displayName.contains(searchText, true) ||
                            item.data.number?.contains(searchText, true) == true

                    if (matchQuery) {
                        val isDuplicate = if (isMergeDuplicate && displayName.isNotEmpty()) {
                            !seenNames.add(displayName.lowercase())
                        } else {
                            false
                        }

                        if (!isDuplicate) {
                            if (lastHeader != null && !tempList.contains(lastHeader)) {
                                tempList.add(lastHeader)
                            }
                            tempList.add(item)
                        }
                    }
                }
            }
        }

        filteredList = tempList
        notifyDataSetChanged()
    }

    fun getCurrentList(): List<ContactListItem> {
        return filteredList
    }

    fun getChangedContacts(): List<ContactModel> {
        return contactList
            .filterIsInstance<ContactListItem.Contact>()
            .map { it.data }
            .distinctBy { it.contactId }
            .filter { contact ->
                val id = contact.contactId
                id != null && contact.isFavourite != initialFavoriteStatus[id]
            }
    }

    class HeaderViewHolder(private val binding: HeaderItemDesignBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ContactListItem.Header) {
            binding.tvHeaderTitle.text = item.title
        }
    }

    inner class ContactViewHolder(private val binding: AllContactDesignBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ContactListItem.Contact, position: Int) {

            val data = item.data

            val context = binding.root.context
            val isExpanded = position == expandedPosition
            binding.llCollapseView.visibility = View.VISIBLE


            // 🔥 Check neighbors (ignore headers)
            val isNextExpanded = position + 1 == expandedPosition
            val isPrevExpanded = position - 1 == expandedPosition

            val isFirst =
                position == 0 || (filteredList.getOrNull(position - 1) is ContactListItem.Header) || isPrevExpanded
            val isLast = position == filteredList.size - 1 ||
                    (filteredList.getOrNull(position + 1) is ContactListItem.Header) || isNextExpanded

            val backgroundRes = when {
                isExpanded -> R.drawable.bg_all_rounded
                isFirst && isLast -> R.drawable.bg_all_rounded
                isFirst -> R.drawable.bg_top_rounded
                isLast -> R.drawable.bg_bottom_rounded
                else -> R.drawable.bg_middle
            }

            binding.llMainView.setBackgroundResource(backgroundRes)

            val params = binding.root.layoutParams as RecyclerView.LayoutParams
            val vertical =
                context.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._10sdp)

            if (isExpanded) {
                binding.viewSep.isVisible = false
                params.setMargins(0, vertical, 0, vertical)
            } else {
                // Separator should be hidden if this is the last in its visual group (includes if next is expanded)
                binding.viewSep.isVisible = !isLast
                params.setMargins(0, 0, 0, 0)
            }

            binding.root.layoutParams = params

            if (!isAllContact) {

                binding.llExpandedView.visibility = if (isExpanded) View.VISIBLE else View.GONE

                binding.llMainView.setOnClickListener {

                    val previousPosition = expandedPosition
                    expandedPosition = if (isExpanded) -1 else position

                    val transition = TransitionSet()
                        .addTransition(Fade())
                        .addTransition(ChangeBounds())
                        .setDuration(250)

                    TransitionManager.beginDelayedTransition(binding.llMainView, transition)

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
                binding.ivFav.visibility = View.GONE

            } else {
                binding.llCollapseView.visibility = View.VISIBLE
                binding.llExpandedView.visibility = View.GONE

                binding.ivFav.visibility = View.VISIBLE
                val isFav = data.isFavourite == 1

                binding.ivFav.setImageResource(
                    if (isFav) R.drawable.ic_fav else R.drawable.ic_un_fav
                )

                binding.llCollapseView.setOnClickListener {
                    val contactId = data.contactId ?: return@setOnClickListener

                    // Toggle in filtered list (UI)
                    val newFavStatus = if (data.isFavourite == 1) 0 else 1

                    // Track this change
                    pendingChanges[contactId] = newFavStatus

                    // 🔥 Update ALL instances in the master list and collect their positions for UI refresh
                    val positionsToRefresh = mutableListOf<Int>()

                    // First, find all positions in the filtered list that need refreshing
                    filteredList.forEachIndexed { index, item ->
                        if (item is ContactListItem.Contact && item.data.contactId == contactId) {
                            item.data.isFavourite = newFavStatus
                            positionsToRefresh.add(index)
                        }
                    }

                    // Also update the master list to ensure consistency if filters change
                    contactList.forEach { item ->
                        if (item is ContactListItem.Contact && item.data.contactId == contactId) {
                            item.data.isFavourite = newFavStatus
                        }
                    }

                    // Refresh all affected items in the UI
                    positionsToRefresh.forEach { pos ->
                        notifyItemChanged(pos)
                    }
                }
            }



            binding.run {

                if (!isAllContact) {
                    ivCall.setOnClickListener {
                        if (!isValidClick()) return@setOnClickListener
                        onClick(data, Constance.ACTION_CALL)
                    }

                    ivMessage.setOnClickListener {
                        if (!isValidClick()) return@setOnClickListener
                        onClick(data, Constance.ACTION_SEND_MESSAGE)
                    }

                    ivVideoCall.setOnClickListener {
                        if (!isValidClick()) return@setOnClickListener
                        onClick(data, Constance.ACTION_VIDEO_CALL)
                    }

                    ivCallInfo.setOnClickListener {
                        if (!isValidClick()) return@setOnClickListener
                        onClick(data, Constance.ACTION_INFO)
                    }
                }




                tvCollapseName.text = data.displayName
                if (!data.number.isNullOrEmpty()) {
                    tvExpandedContactNumber.visibility = View.VISIBLE
                    tvExpandedContactNumber.text = "Mobile +${data.number}"
                } else {
                    tvExpandedContactNumber.visibility = View.GONE
                }

                if (data.userThumbnail.isNullOrEmpty()) {
                    binding.tvCollapseContactName.isVisible = true
                    binding.ivCollapseContactPhoto.isVisible = false
                    val color = Common.profileColors[position % Common.profileColors.size]
                    binding.cvCollapseProfile.setCardBackgroundColor(
                        ContextCompat.getColor(binding.root.context, color)
                    )
                    val firstChar = data.displayName?.firstOrNull()?.uppercase() ?: ""
                    tvCollapseContactName.text = firstChar

                } else {
                    binding.tvCollapseContactName.isVisible = false
                    binding.ivCollapseContactPhoto.isVisible = true
                    Glide.with(ivCollapseContactPhoto.context).load(data.userThumbnail)
                        .into(ivCollapseContactPhoto)
                }

            }

        }
    }
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
                if (item is ContactListItem.Contact) {
                    if (direction == ItemTouchHelper.RIGHT) {
                        onClick(item.data, Constance.ACTION_CALL)
                    } else if (direction == ItemTouchHelper.LEFT) {
                        onClick(item.data, Constance.ACTION_SEND_MESSAGE)
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
}
