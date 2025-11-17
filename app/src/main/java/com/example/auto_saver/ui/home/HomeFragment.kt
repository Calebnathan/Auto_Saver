package com.example.auto_saver.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.auto_saver.R

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        
        // Placeholder - will be replaced with actual dashboard implementation
        val textView = view.findViewById<TextView>(R.id.tv_placeholder)
        textView.text = "Home\n\nDashboard and Categories will be implemented here"
        
        return view
    }
}
