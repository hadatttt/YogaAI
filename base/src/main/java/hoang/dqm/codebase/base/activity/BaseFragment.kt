package hoang.dqm.codebase.base.activity

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.viewbinding.ViewBinding
import hoang.dqm.codebase.base.viewmodel.BaseViewModel
import hoang.dqm.codebase.utils.BindingReflex
import java.lang.reflect.ParameterizedType

@Suppress("unused")
abstract class BaseFragment<VB : ViewBinding, VM : BaseViewModel> : Fragment(),
    View.OnClickListener, DefaultLifecycleObserver {

    private var isLoaded = false
    private var lastClickTime = 0L
    private val interval by lazy { 500L }
    protected var _binding: VB? = null
    protected val binding
        get() = _binding ?: BindingReflex.reflexViewBinding(
            javaClass, layoutInflater
        )

    @Suppress("UNCHECKED_CAST")
    private fun <KClass> genericViewModel(): Class<KClass> {
        return (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[1] as Class<KClass>
    }

    protected open val viewModelFactory: ViewModelProvider.Factory? = null

    protected val viewModel: VM by lazy {
        val clazz = genericViewModel<VM>()
        if (viewModelFactory != null) {
            ViewModelProvider(requireActivity(), viewModelFactory!!)[clazz]
        } else {
            ViewModelProvider(requireActivity())[clazz]
        }
    }
    protected var activity: Activity? = null

    abstract fun initView()
    abstract fun initListener()
    abstract fun initData()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = requireActivity()
        FragmentManager.addFragment(this)
    }

    override fun onPause() {
        super<Fragment>.onPause()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        try {
            lifecycle.addObserver(viewModel)
            _binding = BindingReflex.reflexViewBinding(javaClass, inflater)
            return _binding?.root
        } catch (e: Exception) {
            e.printStackTrace()
            return View(context)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("SCREEN_APP", this::class.java.name)
        try {
//            trackingScreen()
            initView()
            initListener()
            viewModel.isLoading.observe { showLoading(it) }
            if (!isLoaded) {
                initData()
                isLoaded = true
            }
//            logApp("onViewCreated $this")
//            if (appInfo().isHomeClass(this::class.java.name)) {
//                saveFirst(false)
//            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super<Fragment>.onResume()
    }

    open fun idFragmentContainer(): Int = 0

    override fun onDestroy() {
        isLoaded = false
        super<Fragment>.onDestroy()
        FragmentManager.removeFragment(this)
    }

    override fun onDetach() {
        _binding = null
        lifecycle.removeObserver(this)
        super.onDetach()
    }

    override fun onClick(v: View) {
        val nowTime = System.currentTimeMillis()
        if (nowTime - lastClickTime > interval) {
            onSingleClickFrag(v)
            lastClickTime = nowTime
        }
    }

    open fun onSingleClickFrag(v: View) {

    }

    protected fun <T> MutableLiveData<T>.observe(function: (T) -> Unit) {
        this.observe(viewLifecycleOwner) {
            function.invoke(it)
        }
    }

    protected fun <T> MutableLiveData<T>.notifyObserver() {
        this.value = this.value
    }

    fun showLoading(show: Boolean) {
    }

    fun showForceLoading(show: Boolean) {
    }

    override fun onDestroyView() {
        showLoading(false)
        super.onDestroyView()
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
}

fun Fragment.onBackPressed(runnable: Runnable? = null) {
    activity?.onBackPressedDispatcher?.addCallback(
        viewLifecycleOwner,
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                runnable?.run()
            }
        })
}
