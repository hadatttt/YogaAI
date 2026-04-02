//package hoang.dqm.codebase.ui.features.splash
//
//import android.view.ViewGroup
//import androidx.lifecycle.lifecycleScope
//import androidx.viewbinding.ViewBinding
//import hoang.dqm.codebase.service.session.isFirst
//import hoang.dqm.codebase.service.session.isFirstSplash
//import hoang.dqm.codebase.service.session.setFirstSplash
//import com.language_onboard.data.model.Language
//import com.language_onboard.data.model.OnboardingConfig
//import com.language_onboard.data.model.OnboardingItem
//import com.language_onboard.data.model.OnboardingType
//import com.language_onboard.utils.openOnboarding
//import com.language_onboard.utils.tracking
//import hoang.dqm.codebase.base.activity.BaseFragment
//import hoang.dqm.codebase.base.viewmodel.BaseViewModel
//import hoang.dqm.codebase.event.subscribeEventNetwork
//import kotlinx.coroutines.launch
//
//abstract class BaseSplashFragment<VB : ViewBinding, VM : BaseViewModel> :
//    BaseFragment<VB, VM>() {
//
//    override fun initView() {
//        if (isFirstSplash()) {
//            setFirstSplash(false)
//        }
//    }
//
//    override fun initListener() {
//    }
//
//
//    override fun initData() {
//
//    }
//
//    override fun onResume() {
//        super.onResume()
//        subscribeEventNetwork { online ->
//            if (online && isAdded) {
////                openHome()
//                fetchAndInitAds()
//            }
//        }
//    }
//
//    private fun openOnboardingScreen() {
//        val onboardingItems = listOf(
//            OnboardingItem(
//                type = OnboardingType.IMAGE.type,
//                title = R.string.text_ob_1,
//                description = R.string.rate_app_description,
//                imageRes = R.drawable.img_onboard_1,
//                nativeAdsLayoutRes = com.bralydn.ads.R.layout.common_admob_layout_native_medium_onboarding,
//            ), OnboardingItem(
//                type = OnboardingType.IMAGE.type,
//                title = R.string.text_ob_2,
//                description = R.string.rate_app_description,
//                imageRes = R.drawable.img_onboard_2,
//                nativeAdsLayoutRes = com.bralydn.ads.R.layout.common_admob_layout_native_medium_onboarding
//            ), OnboardingItem(
//                type = OnboardingType.IMAGE.type,
//                title = R.string.text_ob_3,
//                description = R.string.rate_app_description,
//                imageRes = R.drawable.img_onboard_3,
//                nativeAdsLayoutRes = com.bralydn.ads.R.layout.common_admob_layout_native_medium_onboarding,
//            )
//        )
//        val languages = Language.entries
//
//        val onboardingConfig = OnboardingConfig(
//            languages = languages,
//            onboardingItems = onboardingItems,
//            languageNativeRes = R.layout.layout_native_onboard,
//            nativeFullRes = R.layout.layout_native_ads_onboarding_full,
//            isHideStatusBar = true,
//        )
//        openOnboarding(onboardingConfig)
//    }
//
//    private fun fetchAndInitAds() {
//        activity?.let {
//            AdManager.fetchAndShowAds(activity = it,
//                fragment = this,
//                onAdDismiss = {
//                    lifecycleScope.launch {
//                        if (isFirst()) {
//                            openOnboardingScreen()
//                        } else {
//                            openHome()
//                        }
//                    }
//                })
//        }
//    }
//
//    abstract fun bannerView(): ViewGroup?
//    abstract fun openHome()
//}
