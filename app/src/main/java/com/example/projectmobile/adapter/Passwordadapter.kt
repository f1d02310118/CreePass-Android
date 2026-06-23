package com.example.projectmobile.adapter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.projectmobile.R
import com.example.projectmobile.model.PasswordItem

class PasswordAdapter(
    private val context: Context,
    private var passwordList: MutableList<PasswordItem>,
    private val onItemClick: (PasswordItem) -> Unit,
    private val onEditClick: (PasswordItem) -> Unit,
    private val onDeleteClick: (PasswordItem) -> Unit
) : RecyclerView.Adapter<PasswordAdapter.PasswordViewHolder>() {

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

        // Default sembunyikan password
        holder.isPasswordVisible = false
        holder.tvPassword.transformationMethod = PasswordTransformationMethod.getInstance()
        holder.tvPassword.text = item.password
        holder.btnTogglePassword.setImageResource(R.drawable.ic_eye_off)

        // Toggle show/hide password
        holder.btnTogglePassword.setOnClickListener {
            holder.isPasswordVisible = !holder.isPasswordVisible
            if (holder.isPasswordVisible) {
                holder.tvPassword.transformationMethod = null
                holder.btnTogglePassword.setImageResource(R.drawable.ic_eye_on)
            } else {
                holder.tvPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                holder.btnTogglePassword.setImageResource(R.drawable.ic_eye_off)
            }
        }

        // Copy password ke clipboard
        holder.btnCopyPassword.setOnClickListener {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("password", item.password)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "Password \"${item.title}\" disalin!", Toast.LENGTH_SHORT).show()
        }

        // Klik item untuk lihat detail
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

    // Update list saat hasil pencarian berubah
    fun updateList(newList: MutableList<PasswordItem>) {
        passwordList = newList
        notifyDataSetChanged()
    }
}