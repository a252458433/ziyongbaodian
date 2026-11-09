package com.ziyongbaodian.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.ziyongbaodian.data.Db
import com.ziyongbaodian.databinding.FragmentMineBinding

class MineFragment : Fragment() {

    private var _binding: FragmentMineBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMineBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.menuImport.setOnClickListener { go(ImportActivity::class.java) }
        binding.menuBank.setOnClickListener { go(TikuActivity::class.java) }
        binding.menuGuide.setOnClickListener { go(GuideActivity::class.java) }
        binding.menuSettings.setOnClickListener { go(SettingsActivity::class.java) }
        binding.menuAbout.setOnClickListener { go(AboutActivity::class.java) }
        binding.tvVersion.text = "自用宝典 ${versionName()}"
        render()
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun render() {
        val b = _binding ?: return
        val s = Db.stats()
        b.statBank.text = s.tikuCount.toString()
        b.statQuestion.text = s.questionCount.toString()
        b.statFav.text = s.favCount.toString()
        b.statWrong.text = s.wrongCount.toString()
    }

    private fun versionName(): String = try {
        requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName ?: "1.0.0"
    } catch (e: Exception) {
        "1.0.0"
    }

    private fun go(cls: Class<*>) {
        startActivity(Intent(requireContext(), cls))
    }
}
