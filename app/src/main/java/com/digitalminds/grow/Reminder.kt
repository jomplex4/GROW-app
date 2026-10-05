package com.digitalminds.grow

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.Notification
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object Reminder {
    private const val CH = "grow_reminder"

    private fun pi(ctx: Context): PendingIntent =
        PendingIntent.getBroadcast(ctx, 7, Intent(ctx, ReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    fun schedule(ctx: Context, minutes: Int) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pi(ctx))
        if (minutes < 0) return
        var t = LocalDateTime.of(LocalDate.now(), LocalTime.of(minutes / 60, minutes % 60))
        if (!t.isAfter(LocalDateTime.now().plusSeconds(5))) t = t.plusDays(1)
        val at = t.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        try {
            if (android.os.Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms())
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi(ctx))
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi(ctx))
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi(ctx))
        }
    }

    fun notify(ctx: Context) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel(CH, "Daily reminder", NotificationManager.IMPORTANCE_HIGH))
        val day = Data.dayOf(LocalDate.now())
        val open = PendingIntent.getActivity(ctx, 8, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n = Notification.Builder(ctx, CH)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle("GROW")
            .setContentText(if (day in 1..Data.TOTAL) "Day $day is ready. Stand tall and start." else "Time for your GROW session.")
            .setContentIntent(open).setAutoCancel(true).build()
        nm.notify(1, n)
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val store = Store(ctx)
        val mins = store.reminder
        if (mins < 0) return
        Data.load(ctx)
        val day = Data.dayOf(LocalDate.now())
        val rest = day in 1..Data.TOTAL && Data.isRest(day)
        if (day in 1..Data.TOTAL && !rest && !store.isDone(day, false)) Reminder.notify(ctx)
        Reminder.schedule(ctx, mins)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val mins = Store(ctx).reminder
        if (mins >= 0) Reminder.schedule(ctx, mins)
    }
}
