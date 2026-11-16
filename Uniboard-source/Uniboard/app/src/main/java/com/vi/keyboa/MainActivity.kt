package com.vi.keyboa

import android.app.Activity
import android.os.Bundle
import android.provider.Settings
import android.content.Intent
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.*

class MainActivity : Activity() {
    private fun dp(v:Int)= (v*resources.displayMetrics.density).toInt()
    override fun onCreate(savedInstanceState:Bundle?) { super.onCreate(savedInstanceState)
        window.statusBarColor=Color.rgb(247,248,252); window.decorView.systemUiVisibility=android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        val root=LinearLayout(this).apply { orientation=1; setPadding(dp(24),dp(34),dp(24),dp(24)); setBackgroundColor(Color.rgb(247,248,252)) }
        root.addView(TextView(this).apply { text="Uniboard"; textSize=32f; setTextColor(Color.rgb(29,35,58)); typeface=Typeface.DEFAULT_BOLD })
        root.addView(TextView(this).apply { text="Bàn phím gõ tiếng Việt cho Android"; textSize=16f; setTextColor(Color.DKGRAY); setPadding(0,dp(6),0,dp(24)) })
        val card=LinearLayout(this).apply { orientation=1; setPadding(dp(18),dp(18),dp(18),dp(18)); background=bg(Color.WHITE,dp(20)) }
        card.addView(TextView(this).apply { text="BẮT ĐẦU"; textSize=12f; letterSpacing=.08f; setTextColor(0xff667085.toInt()); typeface=Typeface.DEFAULT_BOLD })
        val enable=button("1. Bật Uniboard",0xff5266d8.toInt()) { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }
        val choose=button("2. Chọn Uniboard",0xff27314f.toInt()) { (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker() }
        card.addView(enable); card.addView(choose); root.addView(card)
        val info=LinearLayout(this).apply { orientation=1; setPadding(dp(4),dp(24),dp(4),0) }
        info.addView(TextView(this).apply { text="Có sẵn trong bàn phím"; textSize=18f; typeface=Typeface.DEFAULT_BOLD; setTextColor(0xff29314a.toInt()) })
        info.addView(TextView(this).apply { text="• Gõ tiếng Việt bằng Telex hoặc VNI\n• Chuyển nhanh Việt / English\n• Hàng số 0–9 luôn hiển thị\n• Tương thích ứng dụng Android và Samsung S25\n• Phím rộng, dễ chạm; hỗ trợ giao diện sáng/tối"; textSize=15f; setTextColor(0xff4c5367.toInt()); setLineSpacing(dp(6).toFloat(),1f); setPadding(0,dp(12),0,0) })
        root.addView(info); root.addView(TextView(this).apply { text="Cách gõ: aa → â · aw → ă · dd → đ · s/f/r/x/j → dấu thanh"; textSize=13f; setTextColor(0xff697187.toInt()); setPadding(0,dp(26),0,0) })
        setContentView(ScrollView(this).apply { addView(root) })
    }
    private fun button(label:String,color:Int,action:()->Unit)=Button(this).apply { text=label; textSize=15f; isAllCaps=false; setTextColor(Color.WHITE); background=bg(color,dp(14)); setPadding(dp(8),dp(5),dp(8),dp(5)); val p=LinearLayout.LayoutParams(-1,dp(54)); p.topMargin=dp(12); layoutParams=p; setOnClickListener{action()} }
    private fun bg(color:Int,r:Int)=GradientDrawable().apply { setColor(color); cornerRadius=r.toFloat() }
}
