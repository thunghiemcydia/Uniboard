package com.vi.keyboa

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.text.InputType
import android.view.Gravity
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.TextView

class VietIme : InputMethodService() {
    private enum class Mode { TELEX, VNI, EN }

    private var mode = Mode.TELEX
    private var caps = false
    private var capsLocked = false
    private var showingSymbols = false
    private var keyboardRoot: View? = null

    override fun onCreate() {
        super.onCreate()
        val savedMode = getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .getString(PREFERENCE_MODE, Mode.TELEX.name)
        mode = Mode.values().firstOrNull { it.name == savedMode } ?: Mode.TELEX
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (keyboardRoot != null) refreshKeyboard()
    }

    private fun isLandscape() = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    private fun standardRowHeight() = if (isLandscape()) 34 else 43

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun isDark() = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    private fun backgroundColor() = if (isDark()) 0xff22242a.toInt() else 0xffeff0f6.toInt()
    private fun keyColor() = if (isDark()) 0xff3a3d46.toInt() else Color.WHITE
    private fun textColor() = if (isDark()) 0xfff4f4f6.toInt() else 0xff202332.toInt()

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }

    override fun onCreateInputView(): View {
        val compact = isLandscape()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(4), dp(if (compact) 2 else 5), dp(4), dp(if (compact) 3 else 6))
            setBackgroundColor(backgroundColor())
            filterTouchesWhenObscured = true
        }
        keyboardRoot = root
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        top.addView(TextView(this).apply {
            text = "U"
            gravity = Gravity.CENTER
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = rounded(0xff5266d8.toInt(), 9)
            layoutParams = LinearLayout.LayoutParams(dp(if (compact) 30 else 35), dp(if (compact) 28 else 34))
        })
        top.addView(TextView(this).apply {
            text = "  Uniboard"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(textColor())
            layoutParams = LinearLayout.LayoutParams(0, dp(if (compact) 28 else 34), 1f)
        })
        top.addView(TextView(this).apply {
            text = modeName()
            gravity = Gravity.CENTER
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(0xff4358ca.toInt())
            background = rounded(keyColor(), 9)
            setPadding(dp(if (compact) 8 else 11), 0, dp(if (compact) 8 else 11), 0)
            setOnClickListener { cycleMode(); refreshKeyboard() }
        })
        root.addView(top)

        if (showingSymbols) {
            addNumberRow(root, literalOnly = true)
            addRow(root, listOf("!", "@", "#", "\$", "%", "^", "&", "*", "(", ")"))
            addRow(root, listOf("-", "_", "+", "=", "/", "\\", "|", "~", "[", "]"))
            val third = newRow(if (compact) 35 else 43)
            addKey(third, "ABC", 1.15f) { showingSymbols = false; refreshKeyboard() }
            listOf("{", "}", ":", ";", "\"", "'", "<", ">", "?").forEach { symbol ->
                addKey(third, symbol, 0.8f) { commit(symbol) }
            }
            addKey(third, "⌫", 1.15f, repeatWhileHeld = true) { deleteBackspace() }
            root.addView(third)
        } else {
            addNumberRow(root)
            addLetterRow(root, "qwertyuiop")
            addLetterRow(root, "asdfghjkl", inset = 10)
            val third = newRow(if (compact) 35 else 45)
            addKey(third, if (capsLocked) "⇪" else "⇧", 1.25f) {
                if (capsLocked) {
                    capsLocked = false
                    caps = false
                } else if (caps) {
                    capsLocked = true
                } else {
                    caps = true
                }
                refreshKeyboard()
            }
            "zxcvbnm".forEach { letter ->
                addKey(third, letterLabel(letter), 1f) {
                    typeChar(if (caps) letter.uppercaseChar() else letter)
                    if (caps && !capsLocked) { caps = false; refreshKeyboard() }
                }
            }
            addKey(third, "⌫", 1.35f, repeatWhileHeld = true) { deleteBackspace() }
            root.addView(third)
        }

        val bottom = newRow(if (compact) 38 else 47)
        addKey(bottom, if (showingSymbols) "ABC" else "?123", 1.15f) {
            showingSymbols = !showingSymbols
            refreshKeyboard()
        }
        addKey(bottom, ",", 0.7f) { typeChar(',') }
        addKey(bottom, "🌐", 0.75f) {
            (getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager)
                .showInputMethodPicker()
        }
        addKey(bottom, "Tiếng Việt", 3.5f, accent = true) { commit(" ") }
        addKey(bottom, ".", 0.7f) { typeChar('.') }
        addKey(bottom, "↵", 1.05f, accent = true) { enter() }
        root.addView(bottom)
        return root
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        secureImeWindow()
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        secureImeWindow()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        secureImeWindow()
    }

    override fun onEvaluateFullscreenMode() = false

    private fun secureImeWindow() {
        keyboardRoot?.filterTouchesWhenObscured = true
        window.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    private fun modeName() = when (mode) {
        Mode.TELEX -> "VI · Telex"
        Mode.VNI -> "VI · VNI"
        Mode.EN -> "EN"
    }

    private fun cycleMode() {
        mode = when (mode) {
            Mode.TELEX -> Mode.VNI
            Mode.VNI -> Mode.EN
            Mode.EN -> Mode.TELEX
        }
        getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(PREFERENCE_MODE, mode.name)
            .apply()
    }

    private fun refreshKeyboard() {
        setInputView(onCreateInputView())
    }

    private fun newRow(height: Int = standardRowHeight()) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        layoutParams = LinearLayout.LayoutParams(-1, dp(height)).apply {
            topMargin = dp(if (isLandscape()) 2 else 4)
        }
    }

    private fun addRow(parent: LinearLayout, labels: List<String>) {
        val row = newRow()
        labels.forEach { label -> addKey(row, label, 1f) { typeChar(label[0]) } }
        parent.addView(row)
    }

    private fun addNumberRow(parent: LinearLayout, literalOnly: Boolean = false) {
        val row = newRow()
        "1234567890".forEach { digit ->
            val label = digit.toString()
            addKey(row, label, 1f, onLongPress = { commit(label) }) { if (literalOnly) commit(label) else typeChar(digit) }
        }
        parent.addView(row)
    }

    private fun addLetterRow(parent: LinearLayout, letters: String, inset: Int = 0) {
        val row = newRow()
        row.setPadding(dp(inset), 0, dp(inset), 0)
        letters.forEach { letter ->
            addKey(row, letterLabel(letter), 1f) {
                typeChar(if (caps) letter.uppercaseChar() else letter)
                if (caps && !capsLocked) { caps = false; refreshKeyboard() }
            }
        }
        parent.addView(row)
    }

    private fun letterLabel(letter: Char) = if (caps) letter.uppercaseChar().toString() else letter.toString()

    private fun addKey(
        row: LinearLayout,
        label: String,
        weight: Float,
        accent: Boolean = false,
        onLongPress: (() -> Unit)? = null,
        repeatWhileHeld: Boolean = false,
        action: () -> Unit
    ) {
        val key = TextView(this).apply {
            text = label
            gravity = Gravity.CENTER
            textSize = if (label.length > 1) {
                if (isLandscape()) 11f else 13f
            } else {
                if (isLandscape()) 18f else 20f
            }
            typeface = if (accent) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            setTextColor(if (accent) Color.WHITE else textColor())
            background = rounded(if (accent) 0xff5266d8.toInt() else keyColor(), 8)
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(0, -1, weight).apply {
                leftMargin = dp(2)
                rightMargin = dp(2)
            }
            setOnClickListener { action() }
            if (onLongPress != null) setOnLongClickListener { onLongPress(); true }
            if (repeatWhileHeld) {
                val repeatTarget = this
                val handler = Handler(Looper.getMainLooper())
                var repeatStartedAt = 0L
                val repeater = object : Runnable {
                    override fun run() {
                        val withinTimeLimit = SystemClock.uptimeMillis() - repeatStartedAt < MAX_BACKSPACE_HOLD_MS
                        if (repeatTarget.isPressed && withinTimeLimit) {
                            action()
                            handler.postDelayed(this, BACKSPACE_REPEAT_MS)
                        }
                    }
                }
                setOnTouchListener { view, event ->
                    when (event.actionMasked) {
                        android.view.MotionEvent.ACTION_DOWN -> {
                            view.isPressed = true
                            repeatStartedAt = SystemClock.uptimeMillis()
                            action()
                            handler.postDelayed(repeater, BACKSPACE_INITIAL_DELAY_MS)
                            true
                        }
                        android.view.MotionEvent.ACTION_MOVE -> {
                            val stillOnKey = event.x >= 0 && event.x < view.width &&
                                event.y >= 0 && event.y < view.height
                            if (!stillOnKey) {
                                view.isPressed = false
                                handler.removeCallbacks(repeater)
                            }
                            true
                        }
                        android.view.MotionEvent.ACTION_UP,
                        android.view.MotionEvent.ACTION_CANCEL -> {
                            view.isPressed = false
                            handler.removeCallbacks(repeater)
                            true
                        }
                        else -> true
                    }
                }
                addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(view: View) = Unit
                    override fun onViewDetachedFromWindow(view: View) {
                        view.isPressed = false
                        handler.removeCallbacks(repeater)
                    }
                })
            }
        }
        row.addView(key)
    }

    private fun typeChar(char: Char) {
        try {
            typeCharSafely(char)
        } catch (_: Exception) {
            runCatching { commit(char.toString()) }
        }
    }

    private fun typeCharSafely(char: Char) {
        if (mode == Mode.EN || !char.isLetterOrDigit() || !allowsVietnameseComposition(currentInputEditorInfo)) {
            commit(char.toString())
            return
        }
        val before = currentInputConnection?.getTextBeforeCursor(MAX_CONTEXT_CHARS, 0)?.toString().orEmpty().takeLast(MAX_CONTEXT_CHARS)
        val word = before.takeLastWhile { it.isLetter() }
        if (word.isEmpty()) {
            commit(char.toString())
            return
        }
        val output = convert(word + char, mode)
        val connection = currentInputConnection ?: return
        connection.beginBatchEdit()
        var restoreOriginal = false
        try {
            if (!connection.deleteSurroundingText(word.length, 0)) {
                connection.commitText(char.toString(), 1)
                return
            }
            restoreOriginal = true
            if (connection.commitText(output, 1)) {
                restoreOriginal = false
            } else {
                if (!connection.commitText(word + char, 1)) {
                    throw IllegalStateException("Input connection rejected replacement text")
                }
                restoreOriginal = false
            }
        } finally {
            if (restoreOriginal) runCatching { connection.commitText(word, 1) }
            runCatching { connection.endBatchEdit() }
        }
    }

    // Composition is opt-in by editor metadata. Unknown, numeric, URL, and password fields
    // receive literal key events and never have surrounding text inspected.
    private fun allowsVietnameseComposition(info: EditorInfo?): Boolean {
        if (info == null) return false
        if ((info.inputType and InputType.TYPE_MASK_CLASS) != InputType.TYPE_CLASS_TEXT) return false
        if ((info.imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING) != 0) return false
        return when (info.inputType and InputType.TYPE_MASK_VARIATION) {
            InputType.TYPE_TEXT_VARIATION_NORMAL,
            InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE,
            InputType.TYPE_TEXT_VARIATION_LONG_MESSAGE,
            InputType.TYPE_TEXT_VARIATION_PERSON_NAME,
            InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT -> true
            else -> false
        }
    }

    private fun deleteBackspace() {
        val connection = currentInputConnection ?: return
        connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
        connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
    }

    private fun convert(raw: String, selectedMode: Mode): String {
        val letters = raw.lowercase()
        var existingTone = 0
        val normalized = StringBuilder(letters.length)
        for (char in letters) {
            val accented = vowelMarks.entries.firstOrNull { entry ->
                entry.value.drop(1).contains(char.toString())
            }
            if (accented == null) {
                normalized.append(char)
            } else {
                normalized.append(accented.key)
                existingTone = accented.value.indexOf(char.toString())
            }
        }
        var text = normalized.toString()
        val inputLetters = raw.filter { it.isLetter() }
        val uppercaseIdentifier = inputLetters.length > 1 && inputLetters.all { it.isUpperCase() }
        val toneKeys = if (selectedMode == Mode.TELEX) "sfrxj" else "12345"
        var requestedTone: Char? = null
        if (text.isNotEmpty() && text.last() in toneKeys) {
            requestedTone = text.last()
            text = text.dropLast(1)
        }

        if (selectedMode == Mode.TELEX) {
            text = text.replace("dd", "đ")
            text = text.replace("aa", "â").replace("ee", "ê").replace("oo", "ô")
            text = text.replace("uow", "ươ")
            text = text.replace("aw", "ă").replace("ow", "ơ").replace("uw", "ư")
        } else {
            text = text.replace("d9", "đ")
            val shapeKey = text.lastOrNull()
            if (shapeKey != null && shapeKey in "678" && !uppercaseIdentifier) {
                val withoutKey = text.dropLast(1)
                val targetIndex = when (shapeKey) {
                    '6' -> withoutKey.indexOfLast { it in "aeo" }
                    '7' -> withoutKey.indexOfLast { it in "ou" }
                    '8' -> withoutKey.indexOfLast { it == 'a' }
                    else -> -1
                }
                if (targetIndex >= 0) {
                    val original = withoutKey[targetIndex]
                    val shaped = when (shapeKey) {
                        '6' -> when (original) { 'a' -> 'â'; 'e' -> 'ê'; else -> 'ô' }
                        '7' -> if (original == 'o') 'ơ' else 'ư'
                        else -> 'ă'
                    }
                    text = withoutKey.substring(0, targetIndex) + shaped + withoutKey.substring(targetIndex + 1)
                }
            }
        }

        // A tone key is literal when there is no vowel to modify. In VNI, preserve
        // trailing digits on all-caps identifiers such as ABC1 or VCB2024.
        if (requestedTone != null &&
            (toneTargetIndex(text) < 0 || (selectedMode == Mode.VNI && uppercaseIdentifier))) {
            text += requestedTone
            requestedTone = null
        }
        val tone = requestedTone?.let(::toneIndex) ?: existingTone
        if (tone > 0) {
            val index = toneTargetIndex(text)
            if (index >= 0) {
                val vowel = text[index]
                text = text.substring(0, index) + vowelMarks.getValue(vowel)[tone] + text.substring(index + 1)
            }
        }
        return when {
            inputLetters.isNotEmpty() && inputLetters.all { it.isUpperCase() } -> text.uppercase()
            raw.firstOrNull()?.isUpperCase() == true -> text.replaceFirstChar { it.uppercase() }
            else -> text
        }
    }

    private fun toneTargetIndex(text: String): Int {
        val vowels = text.indices.filter { text[it] in vowelMarks }
        if (vowels.isEmpty()) return -1

        val nucleus = if (vowels.size > 1 &&
            ((text.startsWith("qu") && vowels.first() == 1) ||
                (text.startsWith("gi") && vowels.first() == 1))) {
            vowels.drop(1)
        } else {
            vowels
        }
        if (nucleus.isEmpty()) return vowels.first()

        val shapedVowel = nucleus.lastOrNull { text[it] in "ăâêôơư" }
        if (shapedVowel != null) return shapedVowel
        if (nucleus.size == 2 && nucleus.last() < text.length - 1) return nucleus.last()
        return if (nucleus.size >= 3) nucleus[1] else nucleus.first()
    }

    private fun toneIndex(key: Char) = when (key) {
        's', '1' -> 1
        'f', '2' -> 2
        'r', '3' -> 3
        'x', '4' -> 4
        else -> 5
    }

    private fun commit(text: String) {
        currentInputConnection?.commitText(text, 1)
    }

    private fun enter() {
        val connection = currentInputConnection ?: return
        val action = (currentInputEditorInfo?.imeOptions ?: 0) and EditorInfo.IME_MASK_ACTION
        if (action == EditorInfo.IME_ACTION_NONE || action == EditorInfo.IME_ACTION_UNSPECIFIED) {
            connection.commitText("\n", 1)
        } else {
            connection.performEditorAction(action)
        }
    }

    private companion object {
        const val MAX_CONTEXT_CHARS = 12
        const val BACKSPACE_INITIAL_DELAY_MS = 400L
        const val BACKSPACE_REPEAT_MS = 55L
        const val MAX_BACKSPACE_HOLD_MS = 10_000L
        const val PREFERENCES_NAME = "vietkey_settings"
        const val PREFERENCE_MODE = "input_mode"
        val vowelMarks = mapOf(
            'a' to listOf("a", "á", "à", "ả", "ã", "ạ"),
            'ă' to listOf("ă", "ắ", "ằ", "ẳ", "ẵ", "ặ"),
            'â' to listOf("â", "ấ", "ầ", "ẩ", "ẫ", "ậ"),
            'e' to listOf("e", "é", "è", "ẻ", "ẽ", "ẹ"),
            'ê' to listOf("ê", "ế", "ề", "ể", "ễ", "ệ"),
            'i' to listOf("i", "í", "ì", "ỉ", "ĩ", "ị"),
            'o' to listOf("o", "ó", "ò", "ỏ", "õ", "ọ"),
            'ô' to listOf("ô", "ố", "ồ", "ổ", "ỗ", "ộ"),
            'ơ' to listOf("ơ", "ớ", "ờ", "ở", "ỡ", "ợ"),
            'u' to listOf("u", "ú", "ù", "ủ", "ũ", "ụ"),
            'ư' to listOf("ư", "ứ", "ừ", "ử", "ữ", "ự"),
            'y' to listOf("y", "ý", "ỳ", "ỷ", "ỹ", "ỵ")
        )
    }
}





