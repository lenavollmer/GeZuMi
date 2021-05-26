package de.htw.gezumi

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

class MainMenuFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_main_menu, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        view.findViewById<Button>(R.id.button_host).setOnClickListener {
            findNavController().navigate(R.id.action_MainMenuFragment_to_Host)
        }
        view.findViewById<Button>(R.id.button_join).setOnClickListener {
            findNavController().navigate(R.id.action_MainMenuFragment_to_Client)
        }
        view.findViewById<Button>(R.id.button_game).setOnClickListener {
            findNavController().navigate(R.id.action_MainMenuFragment_to_Game)
        }

    }

}