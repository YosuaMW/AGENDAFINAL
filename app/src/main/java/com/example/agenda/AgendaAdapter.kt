package com.example.agenda

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

//class declaration
class AgendaAdapter(
    private val context: Context,
    private val agendaList: List<Agenda>,
    private val onDeleteClick: (Agenda) -> Unit, // Callback untuk tombol delete
    private val onEditClick: (Agenda) -> Unit // Callback untuk tombol edit
) : RecyclerView.Adapter<AgendaAdapter.AgendaViewHolder>() {
// onCreateViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AgendaViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_agenda, parent, false)
        return AgendaViewHolder(view)
    }
// onBindViewHolder
    override fun onBindViewHolder(holder: AgendaViewHolder, position: Int) {
        val agenda = agendaList[position]
        holder.bind(agenda)

        // Listener untuk tombol delete
        holder.deleteButton.setOnClickListener {
            onDeleteClick(agenda)
        }

        // Listener untuk tombol edit
        holder.editButton.setOnClickListener {
            onEditClick(agenda)
        }
    }

    override fun getItemCount(): Int = agendaList.size

    class AgendaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val agendaTitle: TextView = itemView.findViewById(R.id.agendaTitle)
        private val agendaDateTime: TextView = itemView.findViewById(R.id.agendaDateTime)
        val deleteButton: Button = itemView.findViewById(R.id.deleteButton)
        val editButton: Button = itemView.findViewById(R.id.editButton)

        fun bind(agenda: Agenda) {
            agendaTitle.text = agenda.title
            agendaDateTime.text = agenda.dateTime
        }
    }
}
