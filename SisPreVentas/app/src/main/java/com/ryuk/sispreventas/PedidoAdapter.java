package com.ryuk.sispreventas;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;

public class PedidoAdapter extends RecyclerView.Adapter<PedidoAdapter.PedidoViewHolder> {

    public interface OnPedidoClickListener {
        void onItemClick(Pedido pedido);
        void onBotonClick(Pedido pedido);
    }

    private List<Pedido> lista;
    private Map<String, Plato> mapaPlatos; // idPlato -> Plato
    private OnPedidoClickListener listener;
    private String textoBoton;

    public PedidoAdapter(List<Pedido> lista,
                         Map<String, Plato> mapaPlatos,
                         String textoBoton,
                         OnPedidoClickListener listener) {
        this.lista = lista;
        this.mapaPlatos = mapaPlatos;
        this.textoBoton = textoBoton;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PedidoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pedido, parent, false);
        return new PedidoViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull PedidoViewHolder holder, int position) {
        Pedido p = lista.get(position);

        String idFecha = p.getId() + " - " + p.getFecha();
        holder.txtIdFecha.setText(idFecha);

        String cliente = "Cliente: " + p.getNombreCliente() + " "
                + p.getApePaternoCliente() + " " + p.getApeMaternoCliente();
        holder.txtCliente.setText(cliente);

        int total = calcularTotal(p);
        String estadoTotal = "Estado: " + p.getEstado() + " - Total: Bs. " + total;
        holder.txtEstadoTotal.setText(estadoTotal);

        holder.btnAccion.setText(textoBoton);

        // Bloquear botón si no está en PEDIDO
        if ("PEDIDO".equals(p.getEstado())) {
            holder.btnAccion.setEnabled(true);
            holder.btnAccion.setAlpha(1f);
        } else {
            holder.btnAccion.setEnabled(false);
            holder.btnAccion.setAlpha(0.5f); // se ve gris
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(p);
        });

        holder.btnAccion.setOnClickListener(v -> {
            if (listener != null) listener.onBotonClick(p);
        });
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    private int calcularTotal(Pedido p) {
        if (p.getItems() == null || mapaPlatos == null) return 0;

        int total = 0;
        for (Map.Entry<String, Integer> entry : p.getItems().entrySet()) {
            String idPlato = entry.getKey();
            int cant = entry.getValue();
            Plato plato = mapaPlatos.get(idPlato);
            if (plato != null) {
                total += plato.getPrecio() * cant;
            }
        }
        return total;
    }

    static class PedidoViewHolder extends RecyclerView.ViewHolder {
        TextView txtIdFecha, txtCliente, txtEstadoTotal;
        Button btnAccion;

        PedidoViewHolder(@NonNull View itemView) {
            super(itemView);
            txtIdFecha = itemView.findViewById(R.id.txtPedidoIdFecha);
            txtCliente = itemView.findViewById(R.id.txtPedidoCliente);
            txtEstadoTotal = itemView.findViewById(R.id.txtPedidoEstadoTotal);
            btnAccion = itemView.findViewById(R.id.btnAccionPedido);
        }
    }
}
