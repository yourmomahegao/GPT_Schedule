package com.example.gptschedule

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.google.gson.Gson
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.time.LocalDate


class MainActivity : AppCompatActivity() {
    // Window resolution
    private var metrics = DisplayMetrics()
    private var screenWidth = metrics.widthPixels
    private var screenHeight = metrics.heightPixels

    // Default elements
    private var currentDate: TextView? = null
    private var currentDatePlusDays: Long? = null
    private var saveGroup: Button? = null
    private var groupsChoose: Spinner? = null
    private var cardWindow: LinearLayout? = null

    // Schedule text views
    private var teacherText1: TextView? = null
    private var teacherText2: TextView? = null
    private var teacherText3: TextView? = null
    private var teacherText4: TextView? = null
    private var lessonText1: TextView? = null
    private var lessonText2: TextView? = null
    private var lessonText3: TextView? = null
    private var lessonText4: TextView? = null

    // Other elements
    private var firstUpdateGroup: Boolean = true  // Is group updated for the first time

    // Changeable elements
    private var currentChosenGroup: Int? = null
    private var currentChosenGroupID: Int? = null
    private var groupsClassList: GroupsClass? = null
    private var isCardWindowMaximized: Boolean = false

    // Python integration
    private var py: Python? = null
    private var politehParser: PyObject? = null

    // Getting guspoliteh page
    private fun getGroups(): GroupsClass {
        val jsonGroups = politehParser!!["getGroups"]?.call().toString()
        val gson = Gson()

        return gson.fromJson(jsonGroups, GroupsClass::class.java)
    }

    // Updating groups to show
    private fun updateGroups() {
        groupsClassList = getGroups()
        val arrayList = groupsClassList!!.group_names
        val arrayAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, arrayList)
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        groupsChoose!!.adapter = arrayAdapter
        groupsChoose!!.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View,
                position: Int,
                id: Long
            ) {
                try {
                    // Setting up current group ID
                    currentChosenGroupID = id.toInt()
                    currentChosenGroup = groupsClassList!!.group_ids[currentChosenGroupID!!].toInt()

                    // Updating schedule
                    updateSchedule()

                    // Showing save button if not the first run

                    if (!firstUpdateGroup) {
                        saveGroup?.visibility = View.VISIBLE
                    } else {
                        firstUpdateGroup = false
                    }
                }

                catch (_: Throwable) {

                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    // Updating current date on screen
    @RequiresApi(Build.VERSION_CODES.O)
    private fun setCurrentDatePlusDays(plusDays: Long) {
        currentDatePlusDays = plusDays
        currentDate?.text = LocalDate.now().plusDays(currentDatePlusDays!!).toString()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun plusCurrentDatePlusDay() {
        currentDatePlusDays = currentDatePlusDays?.plus(1)
        currentDate?.text = LocalDate.now().plusDays(currentDatePlusDays!!).toString()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun minusCurrentDatePlusDay() {
        currentDatePlusDays = currentDatePlusDays?.minus(1)
        currentDate?.text = LocalDate.now().plusDays(currentDatePlusDays!!).toString()
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun updateSchedule() {
        GlobalScope.launch {
            val jsonGroups = politehParser!!["getSchedule"]?.call(currentChosenGroup, currentDate?.text, false).toString()
            val gson = Gson()

            Log.d("MyLog", jsonGroups)

            val schedule = gson.fromJson(jsonGroups, ScheduleClass::class.java)

            runOnUiThread {
                teacherText1!!.text = schedule.teachers[0]
                teacherText2!!.text = schedule.teachers[1]
                teacherText3!!.text = schedule.teachers[2]
                teacherText4!!.text = schedule.teachers[3]

                lessonText1!!.text = schedule.lessons[0]
                lessonText2!!.text = schedule.lessons[1]
                lessonText3!!.text = schedule.lessons[2]
                lessonText4!!.text = schedule.lessons[3]
            }
        }
    }

    // Saving user group
    private fun saveUserGroup() {
        try {
            // Saving value to directory
            val file = File(filesDir, "saved_group.txt")

            val fos = FileOutputStream(file)
            val osw = OutputStreamWriter(fos)

            osw.write(currentChosenGroupID!!.toString())
            osw.flush()
            osw.close()
            fos.close()

            // Hiding save button
            saveGroup?.visibility = View.GONE
        }

        catch (e: Throwable) {
            return
        }
    }

    // Saving user group
    private fun loadUserGroup() {
        try {
            val file = File(filesDir, "saved_group.txt")

            val fis = FileInputStream(file)
            val isr = InputStreamReader(fis)
            val br = BufferedReader(isr)

            val sb = StringBuilder()
            var line: String?

            while (br.readLine().also { line = it } != null) {
                sb.append(line)
            }

            // Getting and setting last used group
            currentChosenGroupID = sb.toString().toInt()
            currentChosenGroup = groupsClassList!!.group_ids[currentChosenGroupID!!].toInt()

            // Applying used group into list
            groupsChoose!!.post { groupsChoose!!.setSelection(currentChosenGroupID!!) }

            // Updating schedule automatically
            updateSchedule()

            br.close()
            isr.close()
            fis.close()
        }

        catch (e: Throwable) {
            return
        }
    }

    private fun initializeScheduleTextViews() {
        teacherText1 = findViewById<TextView>(R.id.teacher_1) as TextView
        teacherText2 = findViewById<TextView>(R.id.teacher_2) as TextView
        teacherText3 = findViewById<TextView>(R.id.teacher_3) as TextView
        teacherText4 = findViewById<TextView>(R.id.teacher_4) as TextView

        lessonText1 = findViewById<TextView>(R.id.lesson_1) as TextView
        lessonText2 = findViewById<TextView>(R.id.lesson_2) as TextView
        lessonText3 = findViewById<TextView>(R.id.lesson_3) as TextView
        lessonText4 = findViewById<TextView>(R.id.lesson_4) as TextView
    }

    // Updating screen metrics
    @RequiresApi(Build.VERSION_CODES.R)
    private fun updateSreenMetrics() {
        screenWidth = windowManager.currentWindowMetrics.bounds.width()
        screenHeight = windowManager.currentWindowMetrics.bounds.height()
    }

    // Initialing app functions
    @RequiresApi(Build.VERSION_CODES.O)
    private fun initialize() {
        // Python boot up
        if (! Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        // Python modules initialization
        py = Python.getInstance()
        politehParser = py!!.getModule("politeh_parser")

        currentDate = findViewById<TextView>(R.id.currentDate) as TextView
        saveGroup = findViewById<Button>(R.id.saveGroup) as Button
        groupsChoose = findViewById<Spinner>(R.id.groupsChoose) as Spinner
        cardWindow = findViewById<LinearLayout>(R.id.cardWindow) as LinearLayout

        // Updating current date
        setCurrentDatePlusDays(0)

        initializeScheduleTextViews()

        // Click listeners
        saveGroup?.setOnClickListener { saveUserGroup() }

        // Loading user group
        updateGroups()
        loadUserGroup()

        // Hiding save button
        saveGroup?.visibility = View.GONE
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun update() {
        // Python boot up
        if (! Python.isStarted()) {
            Python.start(AndroidPlatform(this))

            // Python modules initialization
            py = Python.getInstance()
            politehParser = py!!.getModule("politeh_parser")
        }

        // Updating sreen metrics
        updateSreenMetrics()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initialize()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onStart() {
        super.onStart()
        update()
    }

    // Swipe settings parameters
    private var startSwipePosX: Float = 0.0f
    private var startSwipePosY: Float = 0.0f
    private var endSwipePosX: Float = 0.0f
    private var endSwipePosY: Float = 0.0f
    private var leftRightSwipeEnded: Boolean = false

    @SuppressLint("ObjectAnimatorBinding")
    private fun translateMainCardMoving(translationNormal: Float, delayTime: Long, durationTime: Long) {
        val translation = screenWidth * translationNormal
        val animationMoving = ObjectAnimator.ofFloat(cardWindow, "translationX", translation)
        val set = AnimatorSet()

        set.play(animationMoving)

        set.startDelay = delayTime
        set.duration = durationTime
        set.interpolator = DecelerateInterpolator(0.9f)
        set.start()
    }

    @SuppressLint("ObjectAnimatorBinding")
    private fun translateMainCardScale(scale: Float, delayTime: Long, durationTime: Long) {
        val animationScaleX = ObjectAnimator.ofFloat(cardWindow, "scaleX", scale)
        val animationScaleY = ObjectAnimator.ofFloat(cardWindow, "scaleY", scale)
        val set = AnimatorSet()

        set.play(animationScaleX)
            .with(animationScaleY)

        set.startDelay = delayTime
        set.duration = durationTime
        set.interpolator = DecelerateInterpolator(0.9f)
        set.start()
    }

    // Animation for right swipe schedule
    @OptIn(DelicateCoroutinesApi::class)
    private fun rightSwipeScheduleAnimation() {
        translateMainCardScale(0.90f, 0, 200)
        translateMainCardScale(1f, 300, 200)
        GlobalScope.launch { delay(250); cardWindow?.translationX = -screenWidth.toFloat() }
        translateMainCardMoving(1.0f, 0, 200)
        translateMainCardMoving(0.0f, 300, 200)
    }

    // Animation for left swipe schedule
    @OptIn(DelicateCoroutinesApi::class)
    private fun leftSwipeScheduleAnimation() {
        translateMainCardScale(0.90f, 0, 200)
        translateMainCardScale(1f, 300, 200)
        GlobalScope.launch { delay(250); cardWindow?.translationX = screenWidth.toFloat() }
        translateMainCardMoving(-1.0f, 0, 200)
        translateMainCardMoving(0.0f, 300, 200)
    }

    // Animation for swipe return
    @OptIn(DelicateCoroutinesApi::class)
    private fun returnNotEndedSwipeAnimation() {
        val animationMoving = ObjectAnimator.ofFloat(cardWindow, "translationX", 0.0f)
        val set = AnimatorSet()

        set.play(animationMoving)

        set.duration = 350
        set.interpolator = DecelerateInterpolator(0.9f)
        set.start()
    }

    private fun restoreCardWindowSwipeParameters() {
        // Return position of not ended swipe
        if (!leftRightSwipeEnded) { returnNotEndedSwipeAnimation() }

        // Resetting all swipe values
        startSwipePosX = 0.0f
        startSwipePosY = 0.0f
        endSwipePosX = 0.0f
        endSwipePosY = 0.0f
        leftRightSwipeEnded = false
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun checkForSwipe(e: MotionEvent, swipeThreshold: Long) {
        // Setting end swipe position
        endSwipePosX = e.x
        endSwipePosY = e.y

        val diffY = endSwipePosY - startSwipePosY
        val diffX = endSwipePosX - startSwipePosX

        // Checking swipe ending
        if (leftRightSwipeEnded) { return }

        // Adding before swipe motion
        cardWindow?.translationX = (endSwipePosX - startSwipePosX)

        // On swipe right
        if (diffX > swipeThreshold) {
            try {
                // Changing current day
                minusCurrentDatePlusDay()
                updateSchedule()

                // Animate swipe for schedule
                rightSwipeScheduleAnimation ()

                // Resetting swipe gesture
                leftRightSwipeEnded = true
            }

            catch (e: Throwable) {
                teacherText1?.text = ""
                teacherText2?.text = ""
                teacherText3?.text = ""
                teacherText4?.text = ""
                lessonText1?.text = "Нет данных"
                lessonText2?.text = "Нет данных"
                lessonText3?.text = "Нет данных"
                lessonText4?.text = "Нет данных"
            }
        }

        // On swipe left
        else if (diffX < -swipeThreshold) {
            try {
                // Changing current day
                plusCurrentDatePlusDay()
                updateSchedule()

                // Animate swipe for schedule
                leftSwipeScheduleAnimation ()

                // Resetting swipe gesture
                leftRightSwipeEnded = true
            }

            catch (e: Throwable) {
                teacherText1?.text = ""
                teacherText2?.text = ""
                teacherText3?.text = ""
                teacherText4?.text = ""
                lessonText1?.text = "Нет данных"
                lessonText2?.text = "Нет данных"
                lessonText3?.text = "Нет данных"
                lessonText4?.text = "Нет данных"
            }
        }

        // On swipe bottom
        else if (diffY > swipeThreshold) {
            // Resetting swipe gesture
            leftRightSwipeEnded = true
        }

        // On swipe top
        else if (diffY < -swipeThreshold) {
            // Resetting swipe gesture
            leftRightSwipeEnded = true
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("ObjectAnimatorBinding")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.action == MotionEvent.ACTION_DOWN) {
            // Setting up start swipe pos
            startSwipePosX = e.x
            startSwipePosY = e.y
        }

        else if (e.action == MotionEvent.ACTION_UP) {
            restoreCardWindowSwipeParameters()
        }

        else if (e.action == MotionEvent.ACTION_MOVE) {
            checkForSwipe(e, 250)
        }

        return true
    }
}