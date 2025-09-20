package com.example.myapplication.ui.rewards

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentRewardsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.*

data class Drink(
    val name: String,
    val points: Int,
    val buttonId: Int
)

class RewardsFragment : Fragment() {

    private var _binding: FragmentRewardsBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var userDataListener: ListenerRegistration? = null
    private var currentPoints: Double = 0.0

    private val drinks = listOf(
        Drink("Kopi O", 50, R.id.btnKopiO),
        Drink("Hot Kopi", 75, R.id.btnHotKopi),
        Drink("Hot Teh", 75, R.id.btnHotTeh),
        Drink("Kopi Peng", 100, R.id.btnKopiPeng),
        Drink("Teh Peng", 100, R.id.btnTehPeng)
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRewardsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Navigation buttons
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.btnHome.setOnClickListener {
            findNavController().navigate(R.id.nav_dashboard)
        }

        // Setup drink redemption buttons
        setupDrinkButtons()

        // Load user data
        loadUserData()
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            binding.tvPointsBalance.text = "0 Points"
            // Disable all drink buttons
            binding.btnKopiO.isEnabled = false
            binding.btnHotKopi.isEnabled = false
            binding.btnHotTeh.isEnabled = false
            binding.btnKopiPeng.isEnabled = false
            binding.btnTehPeng.isEnabled = false
            return
        }

        userDataListener = db.collection("kopi")
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(context, "Error loading data", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    currentPoints = snapshot.getDouble("points") ?: 0.0
                    val kopiRedeemed = snapshot.getLong("noOfKopiRedeemed") ?: 0

                    // Update points display
                    binding.tvPointsBalance.text = "${currentPoints.toInt()} Points"

                    // Update drink buttons based on available points
                    updateDrinkButtons()

                    // Display redeemed coupons
                    displayRedeemedCoupons(kopiRedeemed.toInt())
                } else {
                    // No data yet, create initial document
                    val newUser = hashMapOf(
                        "points" to 0.0,
                        "noOfKopiRedeemed" to 0,
                        "walkHistory" to emptyList<Map<String, Any>>()
                    )
                    db.collection("kopi").document(userId).set(newUser)
                }
            }
    }

    private fun setupDrinkButtons() {
        binding.btnKopiO.setOnClickListener { redeemDrink(drinks[0]) }
        binding.btnHotKopi.setOnClickListener { redeemDrink(drinks[1]) }
        binding.btnHotTeh.setOnClickListener { redeemDrink(drinks[2]) }
        binding.btnKopiPeng.setOnClickListener { redeemDrink(drinks[3]) }
        binding.btnTehPeng.setOnClickListener { redeemDrink(drinks[4]) }
    }

    private fun updateDrinkButtons() {
        drinks.forEach { drink ->
            val button = when (drink.buttonId) {
                R.id.btnKopiO -> binding.btnKopiO
                R.id.btnHotKopi -> binding.btnHotKopi
                R.id.btnHotTeh -> binding.btnHotTeh
                R.id.btnKopiPeng -> binding.btnKopiPeng
                R.id.btnTehPeng -> binding.btnTehPeng
                else -> null
            }

            button?.apply {
                isEnabled = currentPoints >= drink.points
                alpha = if (currentPoints >= drink.points) 1.0f else 0.5f
                text = if (currentPoints >= drink.points) {
                    "${drink.name}\n${drink.points} pts"
                } else {
                    "${drink.name}\n${drink.points} pts\n(${(drink.points - currentPoints).toInt()} more)"
                }
            }
        }
    }

    private fun redeemDrink(drink: Drink) {
        val userId = auth.currentUser?.uid ?: return

        if (currentPoints < drink.points) {
            Toast.makeText(context, "Not enough points for ${drink.name}!", Toast.LENGTH_SHORT).show()
            return
        }

        // Disable all buttons during redemption
        drinks.forEach { d ->
            when (d.buttonId) {
                R.id.btnKopiO -> binding.btnKopiO.isEnabled = false
                R.id.btnHotKopi -> binding.btnHotKopi.isEnabled = false
                R.id.btnHotTeh -> binding.btnHotTeh.isEnabled = false
                R.id.btnKopiPeng -> binding.btnKopiPeng.isEnabled = false
                R.id.btnTehPeng -> binding.btnTehPeng.isEnabled = false
            }
        }

        // Update Firestore
        val updates = hashMapOf<String, Any>(
            "points" to com.google.firebase.firestore.FieldValue.increment(-drink.points.toDouble()),
            "noOfKopiRedeemed" to com.google.firebase.firestore.FieldValue.increment(1),
            "lastRedemption" to System.currentTimeMillis()
        )

        db.collection("kopi")
            .document(userId)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(context, "${drink.name} redeemed successfully! ☕", Toast.LENGTH_LONG).show()
                // Create a coupon code
                val couponCode = generateCouponCode(drink.name)
                saveCoupon(couponCode, drink)
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to redeem: ${e.message}", Toast.LENGTH_SHORT).show()
                updateDrinkButtons()
            }
    }

    private fun generateCouponCode(drinkName: String): String {
        val timestamp = System.currentTimeMillis()
        val random = (1000..9999).random()
        val prefix = drinkName.replace(" ", "").take(4).uppercase()
        return "$prefix-${timestamp.toString().takeLast(6)}-$random"
    }

    private fun saveCoupon(couponCode: String, drink: Drink) {
        val userId = auth.currentUser?.uid ?: return

        val coupon = hashMapOf(
            "code" to couponCode,
            "drinkName" to drink.name,
            "pointsUsed" to drink.points,
            "redeemedAt" to System.currentTimeMillis(),
            "used" to false,
            "userId" to userId
        )

        db.collection("kopi")
            .document(userId)
            .collection("coupons")
            .add(coupon)
            .addOnSuccessListener {
                // Coupon saved successfully
            }
            .addOnFailureListener {
                // Handle error silently
            }
    }

    private fun displayRedeemedCoupons(kopiCount: Int) {
        val userId = auth.currentUser?.uid ?: return

        // Load coupons from subcollection
        db.collection("kopi")
            .document(userId)
            .collection("coupons")
            .orderBy("redeemedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener { documents ->
                binding.couponsContainer.removeAllViews()

                if (documents.isEmpty) {
                    // Show placeholder if no coupons
                    if (kopiCount > 0) {
                        addCouponPlaceholder(kopiCount)
                    }
                } else {
                    // Display actual coupons
                    for (document in documents) {
                        val code = document.getString("code") ?: "KOPI-XXXX"
                        val drinkName = document.getString("drinkName") ?: "Kopi"
                        val pointsUsed = document.getLong("pointsUsed")?.toInt() ?: 100
                        val used = document.getBoolean("used") ?: false
                        val redeemedAt = document.getLong("redeemedAt") ?: 0L
                        addCouponView(code, drinkName, pointsUsed, used, redeemedAt)
                    }
                }
            }
            .addOnFailureListener {
                // If loading fails, show count-based placeholder
                if (kopiCount > 0) {
                    addCouponPlaceholder(kopiCount)
                }
            }
    }

    private fun addCouponView(code: String, drinkName: String, pointsUsed: Int, used: Boolean, redeemedAt: Long) {
        val cardView = CardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 16)
            }
            radius = 12f
            cardElevation = 4f
            setCardBackgroundColor(if (used) 0xFFE0E0E0.toInt() else 0xFFFFFFFF.toInt())
        }

        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }

        val drinkText = TextView(requireContext()).apply {
            text = "$drinkName ($pointsUsed pts)"
            textSize = 20f
            setTextColor(if (used) 0xFF808080.toInt() else 0xFF6B4423.toInt())
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

        val codeText = TextView(requireContext()).apply {
            text = code
            textSize = 16f
            setTextColor(if (used) 0xFF808080.toInt() else 0xFF8B5A3C.toInt())
            setTypeface(typeface, android.graphics.Typeface.NORMAL)
        }

        val statusText = TextView(requireContext()).apply {
            text = if (used) "Used" else "Available for use at any kopi shop"
            textSize = 14f
            setTextColor(if (used) 0xFF808080.toInt() else 0xFF999999.toInt())
        }

        val dateText = TextView(requireContext()).apply {
            val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            text = "Obtained on: ${dateFormat.format(Date(redeemedAt))}"
            textSize = 12f
            setTextColor(0xFF999999.toInt())
        }

        container.addView(drinkText)
        container.addView(codeText)
        container.addView(statusText)
        container.addView(dateText)
        cardView.addView(container)
        binding.couponsContainer.addView(cardView)
    }

    private fun addCouponPlaceholder(count: Int) {
        val textView = TextView(requireContext()).apply {
            text = "You have redeemed $count kopi${if (count > 1) "s" else ""}!\nCoupons will appear here after redemption."
            textSize = 16f
            setTextColor(0xFF6B4423.toInt())
            setPadding(0, 24, 0, 24)
            gravity = android.view.Gravity.CENTER
        }
        binding.couponsContainer.addView(textView)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        userDataListener?.remove()
        _binding = null
    }
}