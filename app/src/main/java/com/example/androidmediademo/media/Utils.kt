package com.example.androidmediademo.media

import android.content.Context
import android.widget.Toast

object Utils {

    fun showToast(context: Context, content:String){
        Toast.makeText(context, content, Toast.LENGTH_SHORT).show()
    }
}