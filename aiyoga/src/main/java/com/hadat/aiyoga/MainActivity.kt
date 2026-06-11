package com.hadat.aiyoga

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.firebase.FirebaseApp
import com.qamar.curvedbottomnaviagtion.CurvedBottomNavigation
import com.hadat.aiyoga.utils.service.AppPreferences
import hoang.dqm.codebase.base.activity.navigate
import com.airbnb.lottie.LottieAnimationView
import com.hadat.aiyoga.data.remoteconfig.YogaDataUtils

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var bottomNavigation: CurvedBottomNavigation
    private lateinit var loadingView: View
    private var currentSelectedBottomItem = HOME_ITEM
    companion object {
        val HOME_ITEM = R.id.homeFragment
        val PRACTICE_ITEM = R.id.workoutOverviewFragment
        val SEQUENCES_ITEM = R.id.communityMySequenceFragment
        val PROFILE_ITEM = R.id.profileFragment
        val MAP_ITEM = R.id.mapFragment
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppPreferences.syncLanguageFromPreferences(this)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(applicationContext)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            AppPreferences.updateLastAppOpenDate(this)
        }
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        loadingView = findViewById(R.id.loadingView)

        if (AppPreferences.isLoggedIn(this)) {
            loadingView.visibility = View.VISIBLE
            (loadingView as? LottieAnimationView)?.playAnimation()
        }
        loadingView.postDelayed({
            YogaDataUtils.prefetchData(this) { success ->
                Log.d("YogaData", "Prefetch: $success")
                runOnUiThread { loadingView.visibility = View.GONE }
            }
        }, 800)
        setupNavigation(savedInstanceState)
    }

    private fun setupNavigation(savedInstanceState: Bundle?) {
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

        if (savedInstanceState == null) {
            val navGraph = navController.navInflater.inflate(R.navigation.app_nav)

            val startDestination = when {
                !AppPreferences.isLoggedIn(this) -> R.id.loginFragment
                !AppPreferences.isHealthProfileCompleted(this) -> R.id.informationFragment
                else -> R.id.homeFragment
            }

            navGraph.setStartDestination(startDestination)
            navController.graph = navGraph
        }
    }

    private fun setUpBottomNavigation() {
        val items = listOf(
            CurvedBottomNavigation.Model(MAP_ITEM, getString(R.string.map), R.drawable.ic_social),
            CurvedBottomNavigation.Model(PRACTICE_ITEM, getString(R.string.history), R.drawable.ic_history),
            CurvedBottomNavigation.Model(HOME_ITEM, getString(R.string.home), R.drawable.ic_home),
            CurvedBottomNavigation.Model(SEQUENCES_ITEM, getString(R.string.sequences), R.drawable.ic_pratice),
            CurvedBottomNavigation.Model(PROFILE_ITEM, getString(R.string.profile), R.drawable.ic_my_profile)
        )

        bottomNavigation.apply {
            items.forEach { add(it) }
            setOnClickMenuListener { item ->
                if (currentSelectedBottomItem != item.id) {
                    currentSelectedBottomItem = item.id
                    navigate(item.id)
                }
            }
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val hideNav = setOf(
                R.id.loginFragment,
                R.id.singleYogaFragment,
                R.id.informationFragment,
                R.id.multiModeYogaFragment,
                R.id.multiNormalYogaFragment,
                R.id.yogaFragment,
                R.id.aiCameraGuideFragment,
                R.id.chooseModeFragment,
                R.id.singleNormalYogaFragment,
            )

            if (hideNav.contains(destination.id)) {
                bottomNavigation.visibility = View.GONE
            } else {
                bottomNavigation.visibility = View.VISIBLE

                val bottomItems = setOf(MAP_ITEM, PRACTICE_ITEM, HOME_ITEM, SEQUENCES_ITEM, PROFILE_ITEM)

                if (bottomItems.contains(destination.id)) {
                    currentSelectedBottomItem = destination.id
                    bottomNavigation.show(destination.id, true)
                }
            }
        }
    }

}
