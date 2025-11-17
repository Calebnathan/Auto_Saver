package com.example.auto_saver.ui.race

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.auto_saver.R

class RaceFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Layout already contains the text from strings.xml
        return inflater.inflate(R.layout.fragment_race, container, false)
    }
}
