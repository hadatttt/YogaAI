package com.hadat.aiyoga

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import com.google.firebase.FirebaseApp

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(applicationContext)
        Log.d("FCM", "Init OK")
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        val navHostFragmentView = findViewById<android.view.View>(R.id.navHostFragment)
        ViewCompat.setOnApplyWindowInsetsListener(navHostFragmentView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController
        val navGraph = navController.navInflater.inflate(R.navigation.app_nav)
        val startDestinationId = R.id.yogaFragment

        navGraph.setStartDestination(startDestinationId)
        navController.graph = navGraph
    }
}
