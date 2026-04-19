package com.hadat.aiyoga

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.os.LocaleListCompat
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
        val PRACTICE_ITEM = R.id.workoutOverviewFragment
        val SEQUENCES_ITEM = R.id.communityMySequenceFragment
        val PROFILE_ITEM = R.id.profileFragment
        val MAP_ITEM = R.id.mapFragment
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val currentLang = AppPreferences.getLanguageCode(this)
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(currentLang)
        )
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
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
            if (AppPreferences.isHealthProfileCompleted(this)) {
                R.id.homeFragment
            } else {
                R.id.informationFragment
            }
        } else {
            R.id.loginFragment
        }
        navGraph.setStartDestination(startDestination)
        navController.graph = navGraph
    }

    private var currentSelectedBottomItem = HOME_ITEM

    private fun setUpBottomNavigation() {
        val bottomNavigationItems = mutableListOf(
            CurvedBottomNavigation.Model(MAP_ITEM, getString(R.string.map), R.drawable.ic_social),
            CurvedBottomNavigation.Model(PRACTICE_ITEM, getString(R.string.history), R.drawable.ic_history),
            CurvedBottomNavigation.Model(HOME_ITEM, getString(R.string.home), R.drawable.ic_home),
            CurvedBottomNavigation.Model(SEQUENCES_ITEM, getString(R.string.sequences), R.drawable.ic_pratice),
            CurvedBottomNavigation.Model(PROFILE_ITEM, getString(R.string.profile), R.drawable.ic_my_profile)
        )

        bottomNavigation.apply {
            bottomNavigationItems.forEach { add(it) }

            setOnClickMenuListener { model ->
                currentSelectedBottomItem = model.id
                navigate(model.id)
            }
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val fragmentsToHideNavigation = setOf(
                R.id.loginFragment,
                R.id.singleYogaFragment,
                R.id.informationFragment,
                R.id.yogaFragment
            )

            if (fragmentsToHideNavigation.contains(destination.id)) {
                bottomNavigation.visibility = View.GONE
            } else {
                bottomNavigation.visibility = View.VISIBLE

                when (destination.id) {
                    MAP_ITEM,
                    PRACTICE_ITEM,
                    HOME_ITEM,
                    SEQUENCES_ITEM,
                    PROFILE_ITEM -> {
                        currentSelectedBottomItem = destination.id
                    }
                }

                bottomNavigation.show(currentSelectedBottomItem, true)
            }
        }
    }
}