
package com.example.medicine.ui

import android.content.Intent
import android.net.Uri
import android.view.*
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.medicine.R
import com.example.medicine.model.Medicine
import java.text.SimpleDateFormat
import java.util.*

class MedicineAdapter(
    private var list: List<Medicine>,
    private val onEditClick: ((Medicine) -> Unit)? = null,
    private val onDeleteClick: ((Medicine) -> Unit)? = null
) :
    RecyclerView.Adapter<MedicineAdapter.ViewHolder>() {

    fun updateList(newList: List<Medicine>) {
        list = newList
        notifyDataSetChanged()
    }

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.name)
        val shopName: TextView = v.findViewById(R.id.shopName)
        val shopAddress: TextView = v.findViewById(R.id.shopAddress)
        val price: TextView = v.findViewById(R.id.price)
        val expiry: TextView = v.findViewById(R.id.expiry)
        val alert: TextView = v.findViewById(R.id.alert)
        val star: View = v.findViewById(R.id.lifeSavingBadge)
        val deleteBtn: ImageView = v.findViewById(R.id.deleteBtn)
        val distance: TextView = v.findViewById(R.id.distanceText)
        val locationContainer: View = v.findViewById(R.id.locationContainer)
        val navigateText: View = v.findViewById(R.id.navigateLink)
    }

    override fun onCreateViewHolder(p: ViewGroup, v: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(p.context)
            .inflate(R.layout.item_medicine, p, false))
    }

    override fun onBindViewHolder(h: ViewHolder, i: Int) {
        val item = list[i]
        h.name.text = item.name
        h.shopName.text = "Medical: ${item.shopName}"
        
        if (item.shopAddress.isNotEmpty()) {
            h.shopAddress.text = "Address: ${item.shopAddress}"
            h.shopAddress.visibility = View.VISIBLE
        } else {
            h.shopAddress.visibility = View.GONE
        }

        h.price.text = "Price: ₹${item.price}"
        h.expiry.text = "Exp: ${item.expiryDate}"
        h.star.visibility = if (item.lifeSaving) View.VISIBLE else View.GONE
        
        val hasCoordinates = item.latitude != 0.0 && item.longitude != 0.0
        val hasMapsLink = item.shopMapsLink.isNotEmpty() && item.shopMapsLink.startsWith("http")
        
        if (item.distance > 0) {
            h.distance.text = String.format(Locale.getDefault(), "📍 %.1f km away", item.distance)
            h.distance.visibility = View.VISIBLE
        } else if (hasCoordinates || hasMapsLink) {
            h.distance.text = "📍 View Location"
            h.distance.visibility = View.VISIBLE
        } else {
            h.distance.visibility = View.GONE
        }

        // NAVIGATION FOR BOTH CUSTOMER AND SHOP OWNER
        if (hasCoordinates || hasMapsLink) {
            h.navigateText.visibility = View.VISIBLE
            h.locationContainer.setOnClickListener {
                // Preference 1: Direct Google Maps Link if provided by shop owner
                if (hasMapsLink) {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(item.shopMapsLink))
                    h.itemView.context.startActivity(browserIntent)
                } 
                // Preference 2: GPS Coordinates navigation
                else if (hasCoordinates) {
                    val gmmIntentUri = Uri.parse("google.navigation:q=${item.latitude},${item.longitude}")
                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                    mapIntent.setPackage("com.google.android.apps.maps")
                    if (mapIntent.resolveActivity(h.itemView.context.packageManager) != null) {
                        h.itemView.context.startActivity(mapIntent)
                    } else {
                        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${item.latitude},${item.longitude}"))
                        h.itemView.context.startActivity(webIntent)
                    }
                }
            }
        } else {
            h.navigateText.visibility = View.GONE
            h.locationContainer.setOnClickListener(null)
        }
        
        // Show delete button only if callback is provided (Medical View)
        if (onDeleteClick != null) {
            h.deleteBtn.visibility = View.VISIBLE
            h.deleteBtn.setOnClickListener { onDeleteClick.invoke(item) }
        } else {
            h.deleteBtn.visibility = View.GONE
        }
        
        h.itemView.setOnClickListener {
            onEditClick?.invoke(item)
        }

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
