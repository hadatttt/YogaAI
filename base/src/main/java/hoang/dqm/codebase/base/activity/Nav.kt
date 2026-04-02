package hoang.dqm.codebase.base.activity


import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import hoang.dqm.codebase.R

import kotlinx.coroutines.launch

fun Fragment.navigate(
    destination: Int, extraData: Bundle? = null, isPop: Boolean = false
) {
    if (!isAdded || view == null) return
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            activity?.navigate(destination, extraData, isPop)
        }
    }
}

fun FragmentActivity.navigate(
    destination: Int, extraData: Bundle? = null, isPop: Boolean = false, isPopAll: Boolean = false
) {
    try {
        val navController = findNavController(R.id.navHostFragment)
        navController.navigate(destination, extraData, navOptions {
            anim {
                enter = R.anim.slide_in_right
                exit = android.R.anim.fade_out
                popEnter = android.R.anim.fade_in
                popExit = R.anim.slide_out_right
            }
            if (isPopAll) {
                popUpTo(navController.graph.startDestinationId) {
                    inclusive = true
                }
            } else if (isPop) {
                navController.currentDestination?.id?.let { currentDestination ->
                    popUpTo(currentDestination) { inclusive = true }
                }
            }
        })
    } catch (ex: Exception) {
        ex.printStackTrace()
    }
}


fun Fragment.popBackStack(destination: Int? = null) {
    activity?.popBackStack(destination)
}

fun FragmentActivity.popBackStack(destination: Int? = null) {
    try {
        val navController = findNavController(R.id.navHostFragment)
        if (destination != null) navController.popBackStack(destination, false)
        else navController.navigateUp()
    } catch (ex: Exception) {
        ex.printStackTrace()
    }
}

fun Fragment.popBackstack(destination: Int, inclusive: Boolean = false): Boolean {
    activity?.let {
        try {
            return findNavController().popBackStack(destination, inclusive)
        } catch (ex: IllegalStateException) {
            ex.printStackTrace()
        } catch (ex: IllegalArgumentException) {
            ex.printStackTrace()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
    return false
}

fun FragmentActivity.getCurrentFragment(): BaseFragment<*, *>? {
    val navHostFragment: Fragment? = supportFragmentManager.findFragmentById(R.id.navHostFragment)
    navHostFragment?.childFragmentManager?.fragments?.get(0)
    return navHostFragment as? BaseFragment<*, *>
}

fun Fragment.showDialog(dialogFragment: DialogFragment, tag: String? = null) {
    if (isAdded && !isDetached && activity != null) {
        try {
            dialogFragment.show(this.childFragmentManager, tag)
        } catch (ex: IllegalStateException) {
            ex.printStackTrace()
        }
    }
}
// Hàm cho Fragment
fun Fragment.navigateWithoutAnimation(
    destination: Int, extraData: Bundle? = null, isPop: Boolean = false
) {
    if (!isAdded || view == null) return
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            activity?.navigateWithoutAnimation(destination, extraData, isPop)
        }
    }
}

// Hàm cho FragmentActivity
fun FragmentActivity.navigateWithoutAnimation(
    destination: Int, extraData: Bundle? = null, isPop: Boolean = false, isPopAll: Boolean = false
) {
    try {
        val navController = findNavController(R.id.navHostFragment)
        navController.navigate(destination, extraData, navOptions {
            // Gán tất cả hiệu ứng bằng 0 để tắt chúng
            anim {
                enter = 0
                exit = 0
                popEnter = 0
                popExit = 0
            }
            if (isPopAll) {
                popUpTo(navController.graph.startDestinationId) {
                    inclusive = true
                }
            } else if (isPop) {
                navController.currentDestination?.id?.let { currentDestination ->
                    popUpTo(currentDestination) { inclusive = true }
                }
            }
        })
    } catch (ex: Exception) {
        ex.printStackTrace()
    }
}


