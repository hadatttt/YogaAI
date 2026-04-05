package com.hadat.aiyoga.login

import com.hadat.aiyoga.R
import com.hadat.aiyoga.databinding.FragmentLoginBinding
import hoang.dqm.codebase.base.activity.BaseFragment
import hoang.dqm.codebase.base.activity.navigate
import hoang.dqm.codebase.utils.singleClick

class LoginFragment : BaseFragment<FragmentLoginBinding, LoginViewModel>() {
    override fun initView() {
    }
    override fun initListener() {
        binding.btnGoogle.singleClick {
            navigate(R.id.homeFragment)
        }
    }
    override fun initData() {
    }
}