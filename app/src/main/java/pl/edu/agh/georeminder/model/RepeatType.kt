package pl.edu.agh.georeminder.model

/**
 * Defines how a task repeats.
 * 
 * NONE - Task does not repeat (one-time task)
 * DAILY - Task repeats every day
 * EVERY_N_DAYS - Task repeats every N days (use repeatInterval field)
 * WEEKLY - Task repeats on specific days of the week (use repeatDaysOfWeek field)
 */
enum class RepeatType {
    NONE,
    DAILY,
    EVERY_N_DAYS,
    WEEKLY
}
