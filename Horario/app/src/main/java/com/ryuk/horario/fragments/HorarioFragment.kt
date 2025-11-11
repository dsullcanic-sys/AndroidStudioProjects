package com.ryuk.horario.fragments
// HorarioFragment.kt
class HorarioFragment(private val viewModel: HorarioViewModel) : Fragment() {

    private var _binding: FragmentHorarioBinding? = null
    private val binding get() = _binding!!

    private lateinit var horarioAdapter: HorarioAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHorarioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()
    }

    private fun setupRecyclerView() {
        horarioAdapter = HorarioAdapter()

        binding.rvHorario.apply {
            adapter = horarioAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupObservers() {
        viewModel.materiasInscritas.observe(viewLifecycleOwner) { materias ->
            val horario = viewModel.generarHorarioSemanal()
            horarioAdapter.submitList(horario.entries.toList())

            binding.tvMateriasInscritas.text = "Materias inscritas: ${materias.size}/8"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(viewModel: HorarioViewModel): HorarioFragment {
            return HorarioFragment(viewModel)
        }
    }
}