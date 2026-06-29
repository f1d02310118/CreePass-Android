package com.example.projectmobile.adapter

import android.content.Context
import android.text.method.PasswordTransformationMethod
import android.util.SparseBooleanArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.projectmobile.R
import com.example.projectmobile.model.PasswordItem
import com.example.projectmobile.util.copyToClipboardWithTimeout
import com.example.projectmobile.util.togglePasswordVisibility

class PasswordAdapter(
    private val context: Context,
    private var passwordList: MutableList<PasswordItem>,
    private val onItemClick: (PasswordItem) -> Unit,
    private val onEditClick: (PasswordItem) -> Unit,
    private val onDeleteClick: (PasswordItem) -> Unit
) : RecyclerView.Adapter<PasswordAdapter.PasswordViewHolder>() {

    private val visibilityStates = SparseBooleanArray()

    inner class PasswordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvUsername: TextView = itemView.findViewById(R.id.tvUsername)
        val tvPassword: TextView = itemView.findViewById(R.id.tvPassword)
        val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        val btnMore: ImageButton = itemView.findViewById(R.id.btnMore)
        val layoutAction: View = itemView.findViewById(R.id.layoutAction)
        val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
        val btnTogglePassword: ImageButton = itemView.findViewById(R.id.btnTogglePassword)
        val btnCopyPassword: ImageButton = itemView.findViewById(R.id.btnCopyPassword)
        var isPasswordVisible: Boolean = false
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PasswordViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_password, parent, false)
        return PasswordViewHolder(view)
    }

    override fun onBindViewHolder(holder: PasswordViewHolder, position: Int) {
        val item = passwordList[position]

        holder.tvTitle.text = item.title
        holder.tvUsername.text = item.username
        holder.tvCategory.text = item.category
        holder.layoutAction.visibility = View.GONE

        holder.isPasswordVisible = visibilityStates.get(position, false)
        holder.tvPassword.text = item.password
        if (holder.isPasswordVisible) {
            holder.tvPassword.transformationMethod = null
            holder.btnTogglePassword.setImageResource(R.drawable.ic_eye_on)
        } else {
            holder.tvPassword.transformationMethod = PasswordTransformationMethod.getInstance()
            holder.btnTogglePassword.setImageResource(R.drawable.ic_eye_off)
        }

        holder.btnTogglePassword.setOnClickListener {
            holder.isPasswordVisible = togglePasswordVisibility(
                holder.isPasswordVisible,
                null,
                holder.btnTogglePassword,
                R.drawable.ic_eye_on,
                R.drawable.ic_eye_off
            )
            if (holder.isPasswordVisible) {
                holder.tvPassword.transformationMethod = null
            } else {
                holder.tvPassword.transformationMethod = PasswordTransformationMethod.getInstance()
            }
            visibilityStates.put(position, holder.isPasswordVisible)
        }

        holder.btnCopyPassword.setOnClickListener {
            copyToClipboardWithTimeout(context, "password", item.password)
            Toast.makeText(context, context.getString(R.string.item_password_copied, item.title), Toast.LENGTH_SHORT).show()
        }

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }

        holder.btnMore.setOnClickListener {
            if (holder.layoutAction.visibility == View.VISIBLE) {
                holder.layoutAction.visibility = View.GONE
            } else {
                holder.layoutAction.visibility = View.VISIBLE
            }
        }

        holder.btnEdit.setOnClickListener {
            onEditClick(item)
        }

        holder.btnDelete.setOnClickListener {
            onDeleteClick(item)
        }
    }

    override fun getItemCount(): Int = passwordList.size

    fun updateList(newList: MutableList<PasswordItem>) {
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = passwordList.size
            override fun getNewListSize(): Int = newList.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return passwordList[oldItemPosition].id == newList[newItemPosition].id
            }
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return passwordList[oldItemPosition] == newList[newItemPosition]
            }
        }
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        passwordList = newList
        visibilityStates.clear()
        diffResult.dispatchUpdatesTo(this)
    }
}
