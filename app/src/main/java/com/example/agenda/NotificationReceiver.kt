package com.example.agenda

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.util.Log

//deklarasi kelas NotificationReceiver yang mengimplementasikan BroadcastReceiver
class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        //menerima data dari intent
        val agendaTitle = intent.getStringExtra("agendaTitle") ?: "Agenda Reminder"
        val agendaTime = intent.getStringExtra("agendaTime") ?: "No Time Set"
        // logging informasi
        Log.d("NotificationReceiver", "Received alarm for agenda: $agendaTitle at $agendaTime")

        //suara notifikasi
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        //Builder Notifikasi
        val builder = NotificationCompat.Builder(context, "agendaChannel")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Agenda Reminder")
            .setContentText("Jangan Lupa: $agendaTitle at $agendaTime")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setSound(alarmSound)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
        //menampilkan notifikasi
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}
