package io.turkmensms.aigateway.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.turkmensms.aigateway.R
import io.turkmensms.aigateway.data.BridgeLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LogAdapter : RecyclerView.Adapter<LogAdapter.Holder>() {
    private val items = ArrayList<BridgeLog>()
    private val fmt = SimpleDateFormat("MM-dd HH:mm:ss", Locale.US)

    fun submit(list: List<BridgeLog>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_log, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.meta.text = "${fmt.format(Date(item.timestamp))}  ${item.direction}  ${item.status}  ${item.peer}"
        holder.subject.text = item.subject
        holder.snippet.text = item.snippet
    }

    override fun getItemCount(): Int = items.size

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val meta: TextView = view.findViewById(R.id.tvLogMeta)
        val subject: TextView = view.findViewById(R.id.tvLogSubject)
        val snippet: TextView = view.findViewById(R.id.tvLogSnippet)
    }
}
