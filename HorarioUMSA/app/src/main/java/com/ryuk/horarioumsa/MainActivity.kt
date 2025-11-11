package com.ryuk.horarioumsa

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ryuk.horarioumsa.adapters.MateriaAdapter
import com.ryuk.horarioumsa.utils.InscripcionManager
import com.ryuk.horarioumsa.utils.MateriaRepository

class MainActivity : AppCompatActivity() {
    private val inscripcionManager = InscripcionManager()
    private lateinit var recyclerMaterias: RecyclerView
    private lateinit var btnVerHorario: Button
    private lateinit var tvContador: TextView
    private lateinit var adapter: MateriaAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupRecyclerView()
        setupClickListeners()
        actualizarContador()
    }

    private fun initViews() {
        recyclerMaterias = findViewById(R.id.recyclerMaterias)
        btnVerHorario = findViewById(R.id.btnVerHorario)
        tvContador = findViewById(R.id.tvContador)
    }

    private fun setupRecyclerView() {
        adapter = MateriaAdapter(
            MateriaRepository.materiasDisponibles,
            onAgregarClick = { materia ->
                val resultado = inscripcionManager.agregarMateria(materia)
                Toast.makeText(this, resultado.mensaje, Toast.LENGTH_SHORT).show()
                actualizarContador()
                adapter.notifyDataSetChanged()
            },
            onQuitarClick = { materia ->
                val exito = inscripcionManager.quitarMateria(materia.sigla)
                if (exito) {
                    Toast.makeText(this, "Se quitó ${materia.sigla}", Toast.LENGTH_SHORT).show()
                    actualizarContador()
                    adapter.notifyDataSetChanged()
                }
            },
            estaInscrita = { materia ->
                inscripcionManager.getMateriasInscritas().contains(materia)
            }
        )

        recyclerMaterias.layoutManager = LinearLayoutManager(this)
        recyclerMaterias.adapter = adapter
    }


    private fun setupClickListeners() {
        btnVerHorario.setOnClickListener {
            mostrarHorario()
        }
    }

    private fun actualizarContador() {
        val count = inscripcionManager.getMateriasInscritas().size
        tvContador.text = "Materias inscritas: $count/8"
    }

    private fun mostrarHorario() {
        val materiasInscritas = inscripcionManager.getMateriasInscritas()
        if (materiasInscritas.isEmpty()) {
            Toast.makeText(this, "No tienes materias inscritas", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(this, HorarioActivity::class.java)
        intent.putExtra("materias", ArrayList(materiasInscritas))
        startActivity(intent)
    }

}