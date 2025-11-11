package com.ryuk.horario.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ryuk.horario.Materia
import com.ryuk.horario.databinding.ItemDiaBinding

class HorarioAdapter : ListAdapter<Map.Entry<String, List<Materia>>, HorarioAdapter.DiaViewHolder>(DiaDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiaViewHolder {
        val binding = ItemDiaBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return DiaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DiaViewHolder, position: Int) {
        val entry = getItem(position)
        holder.bind(entry.key, entry.value)
    }

    inner class DiaViewHolder(
        private val binding: ItemDiaBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(dia: String, materias: List<Materia>) {
            binding.apply {
                tvDia.text = dia

                // Limpiar materias anteriores
                layoutMaterias.removeAllViews()

                // Agregar cada materia al layout
                materias.forEach { materia ->
                    val materiaView = LayoutInflater.from(binding.root.context)
                        .inflate(R.layout.item_materia_horario, layoutMaterias, false)

                    val tvMateria = materiaView.findViewById<TextView>(R.id.tvMateriaHorario)
                    val tvHorario = materiaView.findViewById<TextView>(R.id.tvHorarioCompleto)
                    val tvAula = materiaView.findViewById<TextView>(R.id.tvAulaHorario)

                    tvMateria.text = "${materia.sigla} - ${materia.nombre}"
                    tvHorario.text = "${materia.horaInicio} - ${materia.horaFin}"
                    tvAula.text = materia.aula

                    layoutMaterias.addView(materiaView)
                }

                // Si no hay materias, mostrar mensaje
                if (materias.isEmpty()) {
                    val emptyView = TextView(binding.root.context).apply {
                        text = "No hay materias este día"
                        setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
                        gravity = android.view.Gravity.CENTER
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            val padding16dp = 16.dpToPx(context)
                            setPadding(0, padding16dp, 0, padding16dp)
                        }
                    }
                    layoutMaterias.addView(emptyView)
                }
            }
        }
    }
}

class DiaDiffCallback : DiffUtil.ItemCallback<Map.Entry<String, List<Materia>>>() {
    override fun areItemsTheSame(
        oldItem: Map.Entry<String, List<Materia>>,
        newItem: Map.Entry<String, List<Materia>>
    ): Boolean {
        return oldItem.key == newItem.key
    }

    override fun areContentsTheSame(
        oldItem: Map.Entry<String, List<Materia>>,
        newItem: Map.Entry<String, List<Materia>>
    ): Boolean {
        return oldItem.value == newItem.value
    }
}

// Extensión para convertir dp a píxeles
fun Int.dpToPx(context: Context): Int {
    return (this * context.resources.displayMetrics.density).toInt()
}