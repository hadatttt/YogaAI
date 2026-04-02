package hoang.dqm.codebase.base.activity

import android.content.Context
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.viewbinding.ViewBinding
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import hoang.dqm.codebase.service.network.NetworkStatusReceiver
import hoang.dqm.codebase.utils.BindingReflex
import java.lang.reflect.ParameterizedType

abstract class BaseActivity<VB: ViewBinding, VM: BaseViewModel>: AppCompatActivity(), View.OnClickListener {
    protected val binding: VB by lazy(mode = LazyThreadSafetyMode.NONE) {
        BindingReflex.reflexViewBinding(javaClass, layoutInflater)
    }

    open var onPermissionsGranted: (() -> Unit)? = null

    protected open val viewModelFactory: ViewModelProvider.Factory? = null
    protected val viewModel: VM by lazy {
        val clazz = getGenericSuperclass<VM>(1)
        if (viewModelFactory != null) {
            ViewModelProvider(this, viewModelFactory!!)[clazz]
        } else {
            ViewModelProvider(this)[clazz]
        }
    }

    private fun <KClass> Context.getGenericSuperclass(position: Int): Class<KClass> {
        val baseType = (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments
        return baseType[position] as Class<KClass>
    }

    private var networkStatusReceiver: NetworkStatusReceiver? = null

    private fun registerNetwork() {
        try {
            if (null == networkStatusReceiver) {
                networkStatusReceiver = NetworkStatusReceiver()
            }
            networkStatusReceiver?.let { networkStatusReceiver ->
                if (networkStatusReceiver.isOrderedBroadcast.not()) {
                    val filter = IntentFilter("android.net.conn.CONNECTIVITY_CHANGE")
                    registerReceiver(networkStatusReceiver, filter)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            networkStatusReceiver?.let {
                unregisterReceiver(networkStatusReceiver)
                networkStatusReceiver = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStart() {
        super.onStart()
        registerNetwork()
    }

    abstract fun initView()
    abstract fun initListener()
    abstract fun initData()

    override fun onClick(v: View?) {

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            ActivityManager.addActivity(this)
            lifecycle.addObserver(viewModel)
            setContentView(binding.root)
            if (isHideNavBar()) {
                hideNavBar()
            }
            initView()
            initListener()
            initData()
            val className = this@BaseActivity::class.java.name
//            if (appInfo().isDebug) {
//                logApp("SCREEN_APP $className")
//            }
            onBackPressedDispatcher()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            lifecycle.removeObserver(viewModel)
            ActivityManager.removeActivity(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            handleBackExit()
        }
    }

    open fun handleBackExit() {
        finish()
    }

    open fun onBackPressedDispatcher() {
        onBackPressedDispatcher.addCallback(onBackPressedCallback)
    }

    open fun handleOnBackPressed() {
        onBackPressedCallback.handleOnBackPressed()
    }

    protected fun <T> MutableLiveData<T>.observe(function: (T) -> Unit) {
        this.observe(this@BaseActivity) {
            function.invoke(it)
        }
    }

    protected fun <T> MutableLiveData<T>.notifyObserver() {
        this.value = this.value
    }

    protected fun hideNavBar() {
        try {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowInsetsControllerCompat(window, window.decorView).also { controller ->
                controller.hide(WindowInsetsCompat.Type.navigationBars())
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    protected fun adjustInsetsForBottomNavigation(viewBottom: View) {
        ViewCompat.setOnApplyWindowInsetsListener(viewBottom) { view, insets ->
            try {
                val params = view.layoutParams as ViewGroup.MarginLayoutParams
                val displayCutout =
                    insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemBars())
                params.topMargin = (displayCutout.top + viewBottom.top / 5f).toInt()
                view.layoutParams = params
            } catch (e: Exception) {
                e.printStackTrace()
            }
            insets
        }
    }

    protected fun adjustInsetsFullBottomNavigation(viewBottom: View) {
        ViewCompat.setOnApplyWindowInsetsListener(viewBottom) { view, insets ->
            try {
                val params = view.layoutParams as ViewGroup.MarginLayoutParams
                val displayCutout =
                    insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemBars())
                params.topMargin = displayCutout.top
                params.leftMargin = displayCutout.left
                params.rightMargin = displayCutout.right
                params.bottomMargin = displayCutout.bottom
                view.layoutParams = params
            } catch (e: Exception) {
                e.printStackTrace()
            }
            insets
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && isHideNavBar()) {
            hideNavBar()
        }
    }

    protected fun showNavBar() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
    }

    protected fun isHideNavBar(): Boolean {
        return true
    }

    protected fun currentLanguage(): String {
        return AppCompatDelegate.getApplicationLocales().toLanguageTags()
    }
}