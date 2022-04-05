package com.htcindia.twilio.utils

import android.annotation.SuppressLint
import android.os.Build
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@SuppressLint("SimpleDateFormat")
class DateUtils {

    companion object {

        fun getTimeWithServerTimeStamp(dateString: String): String? {
            val dateFormat = SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss",
                Locale.getDefault()
            )
            val timeFormat = SimpleDateFormat("hh:mm a")
            dateFormat.timeZone = TimeZone.getTimeZone("GMT")
            try {
                return timeFormat.format(dateFormat.parse(dateString)!!)
            } catch (e: ParseException) {
                return null
            }
        }

        fun getTimeWithServerTimeStampClockIn(dateString: String): String? {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
            val timeFormat = SimpleDateFormat("hh:mm a")
            try {
                return timeFormat.format(dateFormat.parse(dateString)!!)
            } catch (e: ParseException) {
                return null
            }
        }

        fun getClockInDate(dateString: String): String? {
            val dateFormat = SimpleDateFormat(
                "d/M/yyyy",
                Locale.getDefault()
            )
            val timeFormat = SimpleDateFormat("dd/MM/yyyy")
            dateFormat.timeZone = TimeZone.getTimeZone("GMT")
            try {
                return timeFormat.format(dateFormat.parse(dateString)!!)
            } catch (e: ParseException) {
                return null
            }
        }

        fun getTimeWithServerTimeZone(dateString: String): String? {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
            val timeFormat = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                SimpleDateFormat("hh:mm a ('UTC'XXX)")
            } else {
                SimpleDateFormat("hh:mm a ('UTC'Z)")
            }
            try {
                return timeFormat.format(dateFormat.parse(dateString)!!)
            } catch (e: ParseException) {
                return null
            }
        }

        fun getTimeWithServerTimeZoneSos(dateString: String): String? {
            val dateFormat = SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss",
                Locale.getDefault()
            )
            val timeFormat = SimpleDateFormat("dd MMM yyyy | hh:mm:ss a (z)")
            dateFormat.timeZone = TimeZone.getTimeZone("GMT")
            try {
                return timeFormat.format(dateFormat.parse(dateString)!!)
            } catch (e: ParseException) {
                return null
            }
        }

        fun getDateWithServerTimeStamp(dateString: String): String? {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
            val timeFormat = SimpleDateFormat("dd MMM yyyy")
            try {
                return timeFormat.format(dateFormat.parse(dateString)!!)
            } catch (e: ParseException) {
                return null
            }
        }

        fun getDateWithServerTimeStampChat(dateString: String): String? {
            val dateFormat = SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss",
                Locale.getDefault()
            )
            val timeFormat = SimpleDateFormat("dd MMM yyyy")
            dateFormat.timeZone = TimeZone.getTimeZone("GMT")
            try {
                return timeFormat.format(dateFormat.parse(dateString)!!)
            } catch (e: ParseException) {
                return null
            }
        }

        fun getChatDateWithServerTimeStamp(dateString: String): String? {
            val dateFormat = SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss",
                Locale.getDefault()
            )
            val timeFormat = SimpleDateFormat("EEEE, MMMM dd, yyyy")
            dateFormat.timeZone = TimeZone.getTimeZone("GMT")
            try {
                return timeFormat.format(dateFormat.parse(dateString)!!)
            } catch (e: ParseException) {
                return null
            }
        }

        fun getDayWithServerTimeStamp(dateString: String): String? {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
            val timeFormat = SimpleDateFormat("EEEE")
            try {
                return timeFormat.format(dateFormat.parse(dateString)!!)
            } catch (e: ParseException) {
                return null
            }
        }

        fun getDate(dateString: String): String? {
            val dateFormat = SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.getDefault()
            )
            val timeFormat = SimpleDateFormat("dd MMM yyyy")
            dateFormat.timeZone = TimeZone.getTimeZone("GMT")
            try {
                return timeFormat.format(dateFormat.parse(dateString)!!)
            } catch (e: ParseException) {
                return null
            }
        }

        fun getTimeInMillis(date: String): Long {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
            sdf.timeZone = TimeZone.getTimeZone("GMT")
            val d = sdf.parse(date)
            return d.time
        }

        fun getTimeInMillisClock(date: String): Long {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm")
            sdf.timeZone = TimeZone.getTimeZone("GMT")
            val d = sdf.parse(date)
            return d.time
        }

        fun convertDateToTimeStamp(dateString: String): String {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy hh:mm a")
            val timeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm")
            return try {
                timeFormat.format(dateFormat.parse(dateString))
            } catch (e: ParseException) {
                "Received Null"
            }
        }

        fun getCurrentDate(): String {
            val sdf = SimpleDateFormat("dd MMM yyyy")
            val currentDate = sdf.format(Date())
            return currentDate
        }

        fun getCurrentDateTime(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
            val currentDate = sdf.format(Date())
            return currentDate
        }

        fun getCurrentDateTimeClock(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm")
            val currentDate = sdf.format(Date())
            return currentDate
        }

        fun dateToUTCString(toServer: String): String {
            val dateFormat = SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss"
            )
            val newDate = dateFormat.parse(toServer)
            val convertedDate = Date(
                newDate.getTime() - Calendar.getInstance().getTimeZone()
                    .getOffset(newDate.getTime())
            )
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
            val currentDate = sdf.format(convertedDate)
            return currentDate
        }

        fun dateToUTCString2(toServer: String): String {
            val dateFormat = SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss"
            )
            val newDate = dateFormat.parse(toServer)
            val convertedDate = Date(
                newDate.getTime() - Calendar.getInstance().getTimeZone()
                    .getOffset(newDate.getTime())
            )
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm")
            val currentDate = sdf.format(convertedDate)
            return currentDate
        }

        fun getHMSDifference(startTimeFromServer: String): TimeDiff {
            try {
                val dateFormat = SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss"
                )
                val newDate = dateFormat.parse(startTimeFromServer)
                val convertedStartDate = newDate.getTime()
                val currentTimeFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
                val currentDate = getCurrentDateTime()
                val currentDateObj = currentTimeFormatter.parse(currentDate)

                var different = if (currentDateObj != null && convertedStartDate != null) {
                    currentDateObj.time - convertedStartDate
                } else {
                    0
                }

                val secondsInMilli: Long = 1000
                val minutesInMilli = secondsInMilli * 60
                val hoursInMilli = minutesInMilli * 60
                val elapsedHours = different / hoursInMilli
                different %= hoursInMilli
                val elapsedMinutes = different / minutesInMilli
                different %= minutesInMilli
                val elapsedSeconds = different / secondsInMilli
                return TimeDiff(elapsedHours, elapsedMinutes, elapsedSeconds)
            } catch (e: Exception) {
                return TimeDiff(0, 0, 0)
            }
        }

        fun calculateTimeDifference(clockInTime: String, clockOutTime: String): String {
            val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
            val simpleDateFormat1 = SimpleDateFormat("HH:mm")

            val startDate = simpleDateFormat.parse(clockInTime)
            val endDate = simpleDateFormat.parse(clockOutTime)

            var difference = endDate.time - startDate.time
            if (difference < 0) {
                val dateMax = simpleDateFormat1.parse("24:00")
                val dateMin = simpleDateFormat1.parse("00:00")
                difference = dateMax.time - startDate.time + (endDate.time - dateMin.time)
            }
            val days = (difference / (1000 * 60 * 60 * 24)).toInt()
            val hours = ((difference - 1000 * 60 * 60 * 24 * days) / (1000 * 60 * 60)).toInt()
            val min =
                (difference - 1000 * 60 * 60 * 24 * days - 1000 * 60 * 60 * hours).toInt() / (1000 * 60)


            var formattedHrs = hours.toString()
            var formattedMins = min.toString()

            if (days != 0) {
                formattedHrs = (days * 24 + hours).toString()
            }
            if (formattedHrs.length == 1) {
                formattedHrs = "0$hours"

            }
            if (formattedMins.length == 1) {
                formattedMins = "0$min"
            }
            return "$formattedHrs:$formattedMins"
        }

        fun getFormattedStopWatch(ms: Long): String {
            var milliseconds = ms
            val hours = TimeUnit.MILLISECONDS.toHours(milliseconds)
            milliseconds -= TimeUnit.HOURS.toMillis(hours)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds)
            milliseconds -= TimeUnit.MINUTES.toMillis(minutes)
            val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds)

            return "${if (hours < 10) "0" else ""}$hours:" +
                    "${if (minutes < 10) "0" else ""}$minutes:" +
                    "${if (seconds < 10) "0" else ""}$seconds"
        }
    }
}

data class TimeDiff(
    val hours: Long,
    val minutes: Long,
    val seconds: Long
)