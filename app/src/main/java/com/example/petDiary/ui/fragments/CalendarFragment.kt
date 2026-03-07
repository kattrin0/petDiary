package com.example.petDiary.ui.fragments

import android.Manifest
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Observer
import com.example.petDiary.R

import com.example.petDiary.domain.model.Event
import com.example.petDiary.ui.adapter.GroupedEventAdapter
import com.example.petDiary.ui.viewmodel.CalendarViewModel
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*

class CalendarFragment : Fragment() {

    private lateinit var tvTodayDate: TextView
    private lateinit var listView: ListView
    private lateinit var btnAddEvent: Button
    private lateinit var tvNoEvents: TextView

    private lateinit var adapter: GroupedEventAdapter
    private lateinit var viewModel: CalendarViewModel

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Уведомления отключены", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_calendar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[CalendarViewModel::class.java]
        initViews(view)
        checkNotificationPermission()
        setupObservers()
        setupAdapter()
        setupClickListeners()
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // Можно показать/скрыть ProgressBar
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    private fun initViews(view: View) {
        tvTodayDate = view.findViewById(R.id.tvTodayDate)
        listView = view.findViewById(R.id.listViewEvents)
        btnAddEvent = view.findViewById(R.id.btnAddEvent)
        tvNoEvents = view.findViewById(R.id.tvNoEvents)
    }

    private fun setupObservers() {
        viewModel.todayDate.observe(viewLifecycleOwner, Observer { date ->
            tvTodayDate.text = date
        })

        viewModel.events.observe(viewLifecycleOwner, Observer { events ->
            updateEventsList(events)
        })
    }

    private fun setupAdapter() {
        adapter = GroupedEventAdapter(
            requireContext(),
            onEventClick = { event -> showEventDetails(event) },
            onEventToggleComplete = { event -> toggleEventComplete(event) },
            onEventDelete = { event -> deleteEvent(event) }
        )
        listView.adapter = adapter
    }

    private fun setupClickListeners() {
        btnAddEvent.setOnClickListener {
            showAddDialog()
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun updateEventsList(events: List<Event>) {
        adapter.updateList(events)

        if (events.isEmpty()) {
            tvNoEvents.visibility = View.VISIBLE
            listView.visibility = View.GONE
        } else {
            tvNoEvents.visibility = View.GONE
            listView.visibility = View.VISIBLE
        }
    }

    private fun toggleEventComplete(event: Event) {
        viewModel.toggleEventComplete(event)
        val message = if (event.isCompleted) "Отмечено как невыполненное" else "Выполнено!"
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun deleteEvent(event: Event) {
        AlertDialog.Builder(requireContext())
            .setTitle("Удалить?")
            .setMessage("Удалить \"${event.title}\"?")
            .setPositiveButton("Да") { _, _ ->
                viewModel.deleteEvent(event)
                Toast.makeText(context, "Удалено", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Нет", null)
            .show()
    }

    private fun showEventDetails(event: Event) {
        val message = buildString {
            append("📅 ${event.date}")
            val time = event.time ?: ""
            if (time.isNotEmpty()) {
                append("\n⏰ $time")
            }
            val desc = event.description ?: ""
            if (desc.isNotEmpty()) {
                append("\n\n📝 $desc")
            }
            append("\n\nСтатус: ${if (event.isCompleted) "✅ Выполнено" else "⏳ Ожидает"}")
        }

        AlertDialog.Builder(requireContext())
            .setTitle(event.title)
            .setMessage(message)
            .setPositiveButton("Закрыть", null)
            .setNeutralButton(if (event.isCompleted) "Отменить выполнение" else "Отметить выполненным") { _, _ ->
                toggleEventComplete(event)
            }
            .show()
    }

    private fun showAddDialog() {
        val dialogView = LayoutInflater.from(context)
            .inflate(R.layout.dialog_add_event, null)

        val etEventTitle = dialogView.findViewById<EditText>(R.id.etEventTitle)
        val etEventDescription = dialogView.findViewById<TextInputEditText>(R.id.etEventDescription)
        val tvSelectedTime = dialogView.findViewById<TextView>(R.id.tvSelectedTime)
        val btnSelectTime = dialogView.findViewById<Button>(R.id.btnSelectTime)
        val calendarView = dialogView.findViewById<CalendarView>(R.id.calendarViewDialog)

        var selectedDateMillis = System.currentTimeMillis()
        var selectedDateString = formatDate(selectedDateMillis)
        var selectedHour = 9
        var selectedMinute = 0
        var timeSelected = false

        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)
        calendarView.minDate = today.timeInMillis

        val maxCal = Calendar.getInstance()
        maxCal.add(Calendar.YEAR, 10)
        calendarView.maxDate = maxCal.timeInMillis

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val cal = Calendar.getInstance().apply {
                set(year, month, dayOfMonth, 0, 0, 0)  // Обнуляем время
                set(Calendar.MILLISECOND, 0)
            }
            selectedDateMillis = cal.timeInMillis
            selectedDateString = formatDate(selectedDateMillis)
        }

        btnSelectTime?.setOnClickListener {
            TimePickerDialog(
                requireContext(),
                { _, hourOfDay, minute ->
                    selectedHour = hourOfDay
                    selectedMinute = minute
                    timeSelected = true
                    tvSelectedTime?.text = String.format("%02d:%02d", hourOfDay, minute)
                    tvSelectedTime?.visibility = View.VISIBLE
                },
                selectedHour,
                selectedMinute,
                true
            ).show()
        }

        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Сохранить") { _, _ ->
                val title = etEventTitle.text.toString().trim()
                val description = etEventDescription?.text?.toString()?.trim() ?: ""

                if (title.isNotEmpty()) {
                    val timeString = if (timeSelected) {
                        String.format("%02d:%02d", selectedHour, selectedMinute)
                    } else ""

                    val event = Event(
                        title = title,
                        description = description,
                        date = selectedDateString,
                        time = timeString,
                        dateMillis = selectedDateMillis,
                        timeHour = selectedHour,
                        timeMinute = selectedMinute,
                        isCompleted = false
                    )
                    viewModel.addEvent(event)
                    Toast.makeText(context, "Событие добавлено!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Введите название", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun formatDate(millis: Long): String {
        val dateFormat = SimpleDateFormat("d MMMM yyyy", Locale("ru"))
        return dateFormat.format(Date(millis))
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshEvents()
    }

    companion object {
        @JvmStatic
        fun newInstance() = CalendarFragment()
    }
}

