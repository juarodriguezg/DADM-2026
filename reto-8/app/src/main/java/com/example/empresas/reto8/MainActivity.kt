package com.example.empresas.reto8

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.empresas.reto8.ui.CompanyFormScreen
import com.example.empresas.reto8.ui.CompanyFormViewModel
import com.example.empresas.reto8.ui.CompanyListScreen
import com.example.empresas.reto8.ui.theme.Reto8Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Reto8Theme {
                val nav = rememberNavController()
                NavHost(navController = nav, startDestination = "list") {
                    composable("list") {
                        CompanyListScreen(
                            onAdd = { nav.navigate("form/${CompanyFormViewModel.NEW_COMPANY}") },
                            onOpen = { nav.navigate("form/${it.id}") },
                        )
                    }
                    composable(
                        "form/{${CompanyFormViewModel.ARG_ID}}",
                        arguments = listOf(navArgument(CompanyFormViewModel.ARG_ID) { type = NavType.LongType }),
                    ) {
                        CompanyFormScreen(onDone = { nav.popBackStack("list", inclusive = false) })
                    }
                }
            }
        }
    }
}
