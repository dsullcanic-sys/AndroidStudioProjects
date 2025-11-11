package com.ryuk.kotlin1

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var n1: EditText
    private lateinit var n2: EditText
    private lateinit var resultado: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        inicializarVistas()
        configurarListeners()
    }

    private fun inicializarVistas() {
        n1 = findViewById(R.id.editTextNumero1)
        n2 = findViewById(R.id.editTextNumero2)
        resultado = findViewById(R.id.textViewResultado)
    }

    private fun configurarListeners() {
        findViewById<Button>(R.id.btnSumar).setOnClickListener { sumar() }
        findViewById<Button>(R.id.btnRestar).setOnClickListener { restar() }
        findViewById<Button>(R.id.btnMultiplicar).setOnClickListener { multiplicar() }
        findViewById<Button>(R.id.btnDividir).setOnClickListener { dividir() }
    }

    private fun obtenerNumeros(): Pair<Int, Int> {
        val a = n1.text.toString().toIntOrNull() ?: 0
        val b = n2.text.toString().toIntOrNull() ?: 0
        return Pair(a, b)
    }

    private fun sumar() {
        val (a, b) = obtenerNumeros()
        val res = a + b
        mostrarResultado(res)
    }

    private fun restar() {
        val (a, b) = obtenerNumeros()
        val res = a - b
        mostrarResultado(res)
    }

    private fun multiplicar() {
        val (a, b) = obtenerNumeros()
        val res = a * b
        mostrarResultado(res)
    }

    private fun dividir() {
        val (a, b) = obtenerNumeros()
        val res = if (b != 0) a / b else null
        if (res != null) {
            mostrarResultado(res)
        } else {
            resultado.text = "Error: División por cero"
        }
    }

    private fun mostrarResultado(res: Int) {
        resultado.text = "Resultado: $res"
    }
}
