
package com.example.medicine.ui

import android.view.*
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.medicine.R
import com.example.medicine.model.Medicine
import java.text.SimpleDateFormat
import java.util.*

class MedicineAdapter(private var list: List<Medicine>) :
    RecyclerView.Adapter<MedicineAdapter.ViewHolder>() {

    fun updateList(newList: List<Medicine>) {
        list = newList
        notifyDataSetChanged()
    }

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.name)
        val shopName: TextView = v.findViewById(R.id.shopName)
        val price: TextView = v.findViewById(R.id.price)
        val expiry: TextView = v.findViewById(R.id.expiry)
        val alert: TextView = v.findViewById(R.id.alert)
    }

    override fun onCreateViewHolder(p: ViewGroup, v: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(p.context)
            .inflate(R.layout.item_medicine, p, false))
    }

    override fun onBindViewHolder(h: ViewHolder, i: Int) {
        val item = list[i]
        h.name.text = item.name
        h.shopName.text = "Medical: ${item.shopName}"
        h.price.text = "Price: ₹${item.price}"
        h.expiry.text = "Exp: ${item.expiryDate}"

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        try {
            val expDate = if (item.expiryDate.isNotEmpty()) sdf.parse(item.expiryDate) else null
            if (expDate != null) {
                val diffInMillis = expDate.time - Date().time
                val diffInDays = diffInMillis / (1000 * 60 * 60 * 24)

                when {
                    diffInDays < 0 -> {
                        // Expired
                        h.itemView.setBackgroundColor(android.graphics.Color.parseColor("#FFCDD2")) // Light Red
                        h.alert.visibility = View.VISIBLE
                        h.alert.text = "EXPIRED"
                        h.alert.setTextColor(android.graphics.Color.RED)
                    }
                    diffInDays < 90 -> {
                        // Expiring in less than 3 months
                        h.itemView.setBackgroundColor(android.graphics.Color.parseColor("#FFF9C4")) // Light Yellow
                        h.alert.visibility = View.VISIBLE
                        h.alert.text = "EXPIRING SOON"
                        h.alert.setTextColor(android.graphics.Color.parseColor("#FBC02D")) // Darker Yellow
                    }
                    else -> {
                        // Healthy stock
                        h.itemView.setBackgroundColor(android.graphics.Color.parseColor("#C8E6C9")) // Light Green
                        h.alert.visibility = View.GONE
                    }
                }
            }
        } catch (e: Exception) {
            h.itemView.setBackgroundColor(android.graphics.Color.WHITE)
            h.alert.visibility = View.GONE
        }
    }

    override fun getItemCount() = list.size
}
