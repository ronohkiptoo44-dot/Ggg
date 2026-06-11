package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.SimChatMainRouter
import com.example.viewmodel.SimChatViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup edge-to-edge transparent drawing
        enableEdgeToEdge()
        
        // Instantiate our View Model with our central Application container
        val viewModel: SimChatViewModel by viewModels {
            SimChatViewModel.Factory(application)
        }
        
        setContent {
            SimChatMainRouter(viewModel = viewModel)
        }
    }
}
