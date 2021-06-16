package de.htw.gezumi

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import de.htw.gezumi.databinding.FragmentMainMenuBinding

class MainMenuFragment : Fragment() {

    private lateinit var _binding: FragmentMainMenuBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_main_menu, container, false)

        return _binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if(arguments?.getBoolean("gameEnded") != null && requireArguments().getBoolean("gameEnded")){
            Toast.makeText(context, R.string.game_closed, Toast.LENGTH_LONG).show()
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

    fun getInputText() : String{
        var text = _binding.inputName.text.toString()

        if(text == "") return "Gustav"
        return text
    }

}