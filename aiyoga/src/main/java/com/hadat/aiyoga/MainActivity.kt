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
import com.hadat.aiyoga.service.AppPreferences
import hoang.dqm.codebase.base.activity.navigate

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

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.navController

        bottomNavigation = findViewById(R.id.bottomNavigation)
        setUpBottomNavigation()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.navHostFragment)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        val navGraph = navController.navInflater.inflate(R.navigation.app_nav)
        val startDestination = if (AppPreferences.isLoggedIn(this)) {
            R.id.homeFragment
        } else {
            R.id.loginFragment
        }
        navGraph.setStartDestination(startDestination)
        navController.graph = navGraph
    }

    private fun setUpBottomNavigation() {
        val bottomNavigationItems = mutableListOf(
            CurvedBottomNavigation.Model(HOME_ITEM, "Home", R.drawable.ic_home),
            CurvedBottomNavigation.Model(PRACTICE_ITEM, "Practice", R.drawable.ic_pratice),
            CurvedBottomNavigation.Model(HISTORY_ITEM, "History", R.drawable.ic_history),
            CurvedBottomNavigation.Model(PROFILE_ITEM, "Profile", R.drawable.ic_my_profile),
            CurvedBottomNavigation.Model(SOCIAL_ITEM, "Social", R.drawable.ic_social)
        )

        bottomNavigation.apply {
            bottomNavigationItems.forEach { add(it) }
            setOnClickMenuListener { model ->
                navigate(model.id)
            }
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val fragmentsToHideNavigation = setOf(
                R.id.loginFragment,
                R.id.singleYogaFragment,
                R.id.yogaFragment
            )

            if (fragmentsToHideNavigation.contains(destination.id)) {
                bottomNavigation.visibility = View.GONE
            } else {
                bottomNavigation.visibility = View.VISIBLE
                bottomNavigation.show(destination.id, true)
            }
        }
    }
}