package com.ryuk.horario.adapters
// MateriasAdapter.kt
class MateriasAdapter(
    private val onAgregarClick: (Materia) -> Unit
) : ListAdapter<Materia, MateriasAdapter.MateriaViewHolder>(MateriaDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MateriaViewHolder {
        val binding = ItemMateriaBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MateriaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MateriaViewHolder, position: Int) {
        val materia = getItem(position)
        holder.bind(materia)
    }

    inner class MateriaViewHolder(
        private val binding: ItemMateriaBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(materia: Materia) {
            binding.apply {
                tvSigla.text = materia.sigla
                tvNombre.text = materia.nombre
                tvDocente.text = materia.docente
                tvHorario.text = "${materia.dias.joinToString()}, ${materia.horaInicio} - ${materia.horaFin}"
                tvAula.text = materia.aula

                btnAgregar.setOnClickListener {
                    onAgregarClick(materia)
                }
            }
        }
    }
}

class MateriaDiffCallback : DiffUtil.ItemCallback<Materia>() {
    override fun areItemsTheSame(oldItem: Materia, newItem: Materia): Boolean {
        return oldItem.sigla == newItem.sigla && oldItem.paralelo == newItem.paralelo
    }

    override fun areContentsTheSame(oldItem: Materia, newItem: Materia): Boolean {
        return oldItem == newItem
    }
}