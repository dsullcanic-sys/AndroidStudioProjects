package com.ryuk.sispreventas;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlatoAdapter extends RecyclerView.Adapter<PlatoAdapter.PlatoViewHolder> {

    private Context context;
    private List<Plato> listaPlatos;
    // Guarda cantidades elegidas: key = idPlato (p01, p02...), value = cantidad
    private Map<String, Integer> cantidadesSeleccionadas = new HashMap<>();

    public PlatoAdapter(Context context, List<Plato> listaPlatos) {
        this.context = context;
        this.listaPlatos = listaPlatos;
    }

    public Map<String, Integer> getCantidadesSeleccionadas() {
        return cantidadesSeleccionadas;
    }

    @NonNull
    @Override
    public PlatoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_plato, parent, false);
        return new PlatoViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull PlatoViewHolder holder, int position) {
        Plato p = listaPlatos.get(position);
        holder.txtNombre.setText(p.getNombre());
        holder.txtPrecio.setText("Bs. " + p.getPrecio());

        Glide.with(context)
                .load(p.getImagen())
                .into(holder.imgPlato);

        // Id del plato (si aún no tienes campo id en Plato, usamos posición: p01, p02...)
        String idPlato = "p" + String.format("%02d", position + 1);

        // Evitar múltiples TextWatchers acumulados
        if (holder.currentWatcher != null) {
            holder.edtCantidad.removeTextChangedListener(holder.currentWatcher);
        }

        int cant = cantidadesSeleccionadas.containsKey(idPlato)
                ? cantidadesSeleccionadas.get(idPlato)
                : 0;
        holder.edtCantidad.setText(String.valueOf(cant));

        // TextWatcher para capturar cambios manuales en el EditText
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                int nuevaCant = parseCantidad(holder.edtCantidad.getText().toString());
                cantidadesSeleccionadas.put(idPlato, nuevaCant);
            }
        };
        holder.edtCantidad.addTextChangedListener(watcher);
        holder.currentWatcher = watcher;

        // Botón +
        holder.btnMas.setOnClickListener(v -> {
            int actual = parseCantidad(holder.edtCantidad.getText().toString());
            actual++;
            holder.edtCantidad.setText(String.valueOf(actual));
            cantidadesSeleccionadas.put(idPlato, actual);
        });

        // Botón -
        holder.btnMenos.setOnClickListener(v -> {
            int actual = parseCantidad(holder.edtCantidad.getText().toString());
            if (actual > 0) actual--;
            holder.edtCantidad.setText(String.valueOf(actual));
            cantidadesSeleccionadas.put(idPlato, actual);
        });
    }

    @Override
    public int getItemCount() {
        return listaPlatos.size();
    }

    private int parseCantidad(String s) {
        try {
            if (s == null || s.isEmpty()) return 0;
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    static class PlatoViewHolder extends RecyclerView.ViewHolder {
        ImageView imgPlato;
        TextView txtNombre, txtPrecio;
        EditText edtCantidad;
        Button btnMas, btnMenos;
        TextWatcher currentWatcher;

        public PlatoViewHolder(@NonNull View itemView) {
            super(itemView);
            imgPlato = itemView.findViewById(R.id.imgPlato);
            txtNombre = itemView.findViewById(R.id.txtNombrePlato);
            txtPrecio = itemView.findViewById(R.id.txtPrecioPlato);
            edtCantidad = itemView.findViewById(R.id.edtCantidad);
            btnMas = itemView.findViewById(R.id.btnMas);
            btnMenos = itemView.findViewById(R.id.btnMenos);
        }
    }
}
