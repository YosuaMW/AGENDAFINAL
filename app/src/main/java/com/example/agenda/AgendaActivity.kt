package com.example.agenda

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.agenda.databinding.ActivityAgendaBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class AgendaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAgendaBinding
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var agendaAdapter: AgendaAdapter
    private val agendaList = mutableListOf<Agenda>()


    // Menyimpan UID pengguna yang sedang login
    private var selectedDate: String = ""
    private var selectedTime: String = ""
    private lateinit var userId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAgendaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        createNotificationChannel()

        // Initialize Firebase Auth and UID
        auth = FirebaseAuth.getInstance()
        userId = auth.currentUser?.uid ?: "" // Ambil UID pengguna yang sedang login

        if (userId.isEmpty()) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
        }

        // Initialize Firebase Database Reference
        database = FirebaseDatabase.getInstance().reference.child("agenda").child(userId)

        // Setup RecyclerView
        agendaAdapter = AgendaAdapter(this, agendaList,
            onDeleteClick = { agenda -> deleteAgenda(agenda) },
            onEditClick = { agenda -> showEditDialog(agenda) }
        )
        binding.agendaRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.agendaRecyclerView.adapter = agendaAdapter

        // Load user-specific agenda data
        loadAgendaData()

        // Select Date Button
        binding.selectDateButton.setOnClickListener {
            showDatePickerDialog { date ->
                selectedDate = date
                updateSelectedDateTimeText()
            }
        }

        // Select Time Button
        binding.selectTimeButton.setOnClickListener {
            showTimePickerDialog { time ->
                selectedTime = time
                updateSelectedDateTimeText()
            }
        }

        // Add Agenda Button
        binding.addAgendaButton.setOnClickListener {
            val agendaTitle = binding.agendaTitleEdit.text.toString()
            val agendaDateTime = "$selectedDate $selectedTime"

            if (agendaTitle.isEmpty() || selectedDate.isEmpty() || selectedTime.isEmpty()) {
                Toast.makeText(this, "Please fill all fields and select date/time", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val agendaId = database.push().key
            val agenda = Agenda(agendaId, agendaTitle, agendaDateTime)

            agendaId?.let {
                database.child(it).setValue(agenda).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Agenda Added Successfully", Toast.LENGTH_SHORT).show()
                        scheduleNotification(agendaTitle, agendaDateTime)
                        clearInputs()
                    } else {
                        val errorMessage = task.exception?.localizedMessage ?: "Unknown error"
                        Toast.makeText(this, "Failed to add agenda: $errorMessage", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // Handle redirect to Login page
        binding.logoutButton.setOnClickListener {
            auth.signOut() // Logout dari Firebase
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish() // Tutup halaman ini
        }
    }
//
    private fun showEditDialog(agenda: Agenda) {
    // Membuat dialog untuk mengedit agenda
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle("Edit Agenda")

    // Membuat layout vertikal untuk menampung input
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 20)
        }

    // Input untuk judul agenda
        val titleInput = EditText(this).apply {
            hint = "Agenda Title"
            setText(agenda.title)// Menampilkan judul agenda yang ada
        }
// Input untuk tanggal agenda (tidak fokus dan menggunakan dialog tanggal)
        val dateInput = EditText(this).apply {
            hint = "Select Date"
            isFocusable = false
            setText(agenda.dateTime?.split(" ")?.get(0)) // Menampilkan tanggal yang ada
            setOnClickListener {
                // Menampilkan DatePickerDialog
                showDatePickerDialog { date -> this.setText(date) }
            }
        }
    // Input untuk waktu agenda (tidak fokus dan menggunakan dialog waktu)
        val timeInput = EditText(this).apply {
            hint = "Select Time"
            isFocusable = false// Agar hanya bisa diisi lewat TimePickerDialog
            setText(agenda.dateTime?.split(" ")?.get(1))// Menampilkan waktu yang ada
            setOnClickListener {
                // Menampilkan TimePickerDialog
                showTimePickerDialog { time -> this.setText(time) }
            }
        }
    // Menambahkan input ke layout dialog
        layout.addView(titleInput)
        layout.addView(dateInput)
        layout.addView(timeInput)

    // Menambahkan layout ke dialog
        dialogBuilder.setView(layout)

    // Tombol simpan untuk menyimpan perubahan
    dialogBuilder.setPositiveButton("Save") { _, _ ->
            val newTitle = titleInput.text.toString().trim()
            val newDate = dateInput.text.toString().trim()
            val newTime = timeInput.text.toString().trim()

        // Validasi input
            if (newTitle.isEmpty() || newDate.isEmpty() || newTime.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

        // Membuat string datetime baru
        val newDateTime = "$newDate $newTime"
            agenda.id?.let { agendaId ->

                // Membuat objek agenda baru
                val updatedAgenda = Agenda(agendaId, newTitle, newDateTime)

                // Menyimpan ke Firebase
                database.child(agendaId).setValue(updatedAgenda).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Agenda Updated Successfully", Toast.LENGTH_SHORT).show()
                        // Menjadwalkan notifikasi baru
                        scheduleNotification(newTitle, newDateTime)
                    } else {
                        val errorMessage = task.exception?.localizedMessage ?: "Unknown error"
                        Toast.makeText(this, "Failed to update agenda: $errorMessage", Toast.LENGTH_SHORT).show()
                        Log.e("AgendaActivity", "Error updating agenda: $errorMessage")
                    }
                }
            }
        }
    // Tombol batal untuk menutup dialog
        dialogBuilder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
    // Menampilkan dialog
        dialogBuilder.create().show()
    }

    private fun loadAgendaData() {
        // Mendapatkan data agenda dari Firebase
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                agendaList.clear()// Menghapus data lama
                for (data in snapshot.children) {
                    val agenda = data.getValue(Agenda::class.java)
                    agenda?.let { agendaList.add(it) }// Menambahkan agenda ke daftar
                }
                agendaAdapter.notifyDataSetChanged()// Memberitahu adapter bahwa data telah berubah

            }

            override fun onCancelled(error: DatabaseError) {
                // Menampilkan pesan jika gagal memuat data
                Toast.makeText(this@AgendaActivity, "Failed to load data: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun deleteAgenda(agenda: Agenda) {
        // Menghapus agenda dari Firebase berdasarkan ID
        agenda.id?.let {
            database.child(it).removeValue().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Membatalkan notifikasi terkait agenda
                    cancelNotification(agenda.title ?: "Unknown Agenda")
                    Toast.makeText(this, "Agenda Deleted Successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to delete agenda", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun cancelNotification(agendaTitle: String) {
        // Membatalkan notifikasi menggunakan PendingIntent
        val intent = Intent(this, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, agendaTitle.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }

    private fun scheduleNotification(agendaTitle: String, agendaDateTime: String) {
        // Menjadwalkan notifikasi untuk agenda
        val dateTimeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val agendaTimeInMillis = dateTimeFormat.parse(agendaDateTime)?.time ?: return

        if (agendaTimeInMillis > System.currentTimeMillis()) {
            val intent = Intent(this, NotificationReceiver::class.java).apply {
                putExtra("agendaTitle", agendaTitle)
                putExtra("agendaTime", agendaDateTime)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                this, agendaTitle.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, agendaTimeInMillis, pendingIntent)
        }
    }
//channel notifikasi
    private fun createNotificationChannel() {
    // Membuat channel notifikasi
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            val channel = NotificationChannel(
                "agendaChannel", "Agenda Notifications", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for Agenda Reminder Notifications"
                setSound(alarmSound, audioAttributes)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)// Mendaftarkan channel
        }
    }

    private fun showDatePickerDialog(onDateSelected: (String) -> Unit) {
        // Menampilkan DatePickerDialog untuk memilih tanggal
        val calendar = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, dayOfMonth ->
            val date = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year)
            onDateSelected(date)
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun showTimePickerDialog(onTimeSelected: (String) -> Unit) {
        // Menampilkan TimePickerDialog untuk memilih waktu
        val calendar = Calendar.getInstance()
        TimePickerDialog(this, { _, hourOfDay, minute ->
            val time = String.format("%02d:%02d", hourOfDay, minute)
            onTimeSelected(time)
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
    }

    private fun updateSelectedDateTimeText() {
        // Mengupdate teks pada UI untuk menampilkan tanggal dan waktu yang dipilih
        binding.selectedDateTimeText.text = "Selected Date and Time: $selectedDate $selectedTime"
    }

    private fun clearInputs() {
        // Membersihkan input pada form
        binding.agendaTitleEdit.text.clear()
        selectedDate = ""
        selectedTime = ""
        binding.selectedDateTimeText.text = ""
    }
}
