package com.hadat.aiyoga

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.firebase.FirebaseApp
import com.qamar.curvedbottomnaviagtion.CurvedBottomNavigation

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var bottomNavigation: CurvedBottomNavigation

    companion object {
        val HOME_ITEM = R.id.homeFragment
        val PRACTICE_ITEM = R.id.loginFragment
        val HISTORY_ITEM = R.id.detailYogaFragment
        val PROFILE_ITEM = R.id.yogaSkeletonFragment

        val SOCIAL_ITEM = R.id.yogaSkeletonFragment
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(applicationContext)
        Log.d("FCM", "Init OK")

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.navController

        bottomNavigation = findViewById(R.id.bottomNavigation)
        setUpBottomNavigation()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.navHostFragment)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        val navGraph = navController.navInflater.inflate(R.navigation.app_nav)
        navGraph.setStartDestination(R.id.loginFragment)
        navController.graph = navGraph
    }

    private fun setUpBottomNavigation() {
        // Tạo danh sách các Model theo đúng tài liệu
        val bottomNavigationItems = mutableListOf(
            CurvedBottomNavigation.Model(PRACTICE_ITEM, "Practice", R.drawable.ic_pratice),
            CurvedBottomNavigation.Model(HISTORY_ITEM, "History", R.drawable.ic_history),
            CurvedBottomNavigation.Model(HOME_ITEM, "Home", R.drawable.ic_home),
            CurvedBottomNavigation.Model(PROFILE_ITEM, "Profile", R.drawable.ic_benefit),
            CurvedBottomNavigation.Model(SOCIAL_ITEM, "Social", R.drawable.ic_benefit)
        )

        bottomNavigation.apply {
            bottomNavigationItems.forEach { add(it) }
            setOnClickMenuListener {
                navController.navigate(it.id)
            }
            show(HOME_ITEM)
            setupNavController(navController)
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.loginFragment || destination.id == R.id.yogaFragment) {
                bottomNavigation.visibility = View.GONE
            } else {
                bottomNavigation.visibility = View.VISIBLE
            }
        }
    }
    override fun onBackPressed() {
        if (navController.currentDestination?.id == HOME_ITEM) {
            super.onBackPressed()
        } else {
            navController.popBackStack(HOME_ITEM, false)
        }
    }
}