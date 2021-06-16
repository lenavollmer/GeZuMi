package de.htw.gezumi

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import de.htw.gezumi.databinding.FragmentMainMenuBinding


class MainMenuFragment : Fragment() {

    private val SHARED_KEY = "SHARED_PREFS"

    private lateinit var _binding: FragmentMainMenuBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_main_menu, container, false)

        return _binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if(arguments?.getBoolean("gameEnded") != null && requireArguments().getBoolean("gameEnded")){
            Toast.makeText(context, R.string.game_closed, Toast.LENGTH_LONG).show()
        }

        _binding.inputName.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                val imm: InputMethodManager =
                    requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(_binding.inputName.windowToken, 0)
                _binding.inputName.clearFocus()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }

        view.findViewById<Button>(R.id.button_host).setOnClickListener {
            val bundle = bundleOf("playerName" to getInputText())
            findNavController().navigate(R.id.action_MainMenuFragment_to_Host, bundle)
        }
        view.findViewById<Button>(R.id.button_join).setOnClickListener {
            val bundle = bundleOf("playerName" to getInputText())
            findNavController().navigate(R.id.action_MainMenuFragment_to_Client, bundle)
        }
        view.findViewById<Button>(R.id.button_game).setOnClickListener {
            findNavController().navigate(R.id.action_MainMenuFragment_to_MockedGame)
        }

    }

    private fun getInputText() : String{
        val text = _binding.inputName.text.toString()

        if(text == "") return "Gustav"
        return text
    }

    override fun onStop() {
        super.onStop()
        val prefs: SharedPreferences = requireActivity().getSharedPreferences(SHARED_KEY, 0)
        val editor: SharedPreferences.Editor = prefs.edit()
        editor.putString("playerName", _binding.inputName.text.toString())
        editor.apply()
    }

    override fun onStart() {
        super.onStart()
        val prefs: SharedPreferences = requireActivity().getSharedPreferences(SHARED_KEY, 0)
        _binding.inputName.setText(prefs.getString("playerName", ""))
    }

}