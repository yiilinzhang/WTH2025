package com.example.myapplication

import android.os.Bundle
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.myapplication.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Toolbar from app_bar_main.xml (Navigation Drawer Activity template)
        setSupportActionBar(binding.toolbar)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)

        // Make Home, Scan, Rewards all top-level so hamburger stays visible
        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow, R.id.nav_walkingMain),
            drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // Ensure drawer "Home" always returns to Home (pop if present, else navigate)
        navView.setNavigationItemSelectedListener { item ->
            val handled = when (item.itemId) {
                R.id.nav_home -> {
                    val popped = navController.popBackStack(R.id.nav_home, false)
                    if (!popped) navController.navigate(R.id.nav_home)
                    true
                }
                R.id.nav_gallery -> {
                    navController.navigate(R.id.nav_gallery); true
                }
                R.id.nav_slideshow -> {
                    navController.navigate(R.id.nav_slideshow); true
                }
                R.id.nav_walkingMain -> {
                    navController.navigate(R.id.nav_walkingMain); true
                }
                else -> false
            }
            if (handled) {
                item.isChecked = true
                drawerLayout.closeDrawers()
            }
            handled
        }

        // (Optional) keep an eye on where you are
        // navController.addOnDestinationChangedListener { _, d, _ ->
        //     android.util.Log.d("NAV", "dest=${resources.getResourceEntryName(d.id)}")
        // }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }
}
