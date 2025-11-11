package com.ryuk.horarioumsa.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ryuk.horarioumsa.R
import com.ryuk.horarioumsa.models.Materia

class MateriaAdapter(
    private val materias: List<Materia>,
    private val onAgregarClick: (Materia) -> Unit,
    private val onQuitarClick: (Materia) -> Unit,
    private val estaInscrita: (Materia) -> Boolean
) : RecyclerView.Adapter<MateriaAdapter.MateriaViewHolder>() {

    class MateriaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvSigla: TextView = itemView.findViewById(R.id.tvSigla)
        val tvNombre: TextView = itemView.findViewById(R.id.tvNombre)
        val tvDocente: TextView = itemView.findViewById(R.id.tvDocente)
        val tvHorario: TextView = itemView.findViewById(R.id.tvHorario)
        val btnAccion: Button = itemView.findViewById(R.id.btnAgregar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MateriaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_materia, parent, false)
        return MateriaViewHolder(view)
    }

    override fun onBindViewHolder(holder: MateriaViewHolder, position: Int) {
        val materia = materias[position]

        holder.tvSigla.text = materia.sigla
        holder.tvNombre.text = materia.nombre
        holder.tvDocente.text = "${materia.docente} - Paralelo ${materia.paralelo}"
        holder.tvHorario.text = "${materia.dias.joinToString(", ")} ${materia.horaInicio}-${materia.horaFin} (${materia.aula})"

        if (estaInscrita(materia)) {
            holder.btnAccion.text = "−"
            holder.btnAccion.setOnClickListener { onQuitarClick(materia) }
        } else {
            holder.btnAccion.text = "+"
            holder.btnAccion.setOnClickListener { onAgregarClick(materia) }
        }
    }

    override fun getItemCount(): Int = materias.size
}

