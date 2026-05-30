package com.jushi.demo.main.ui.imports;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.jushi.demo.main.R;

import java.util.List;

public class SourceAdapter extends RecyclerView.Adapter<SourceAdapter.SourceViewHolder> {
    private final List<SourceItem> items;

    public SourceAdapter(List<SourceItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public SourceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_source, parent, false);
        return new SourceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SourceViewHolder holder, int position) {
        SourceItem item = items.get(position);
        holder.nameText.setText(item.getName());
        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(item.isSelected());

        View.OnClickListener clickListener = v -> {
            boolean next = !item.isSelected();
            item.setSelected(next);
            holder.checkBox.setChecked(next);
        };
        holder.itemView.setOnClickListener(clickListener);
        holder.checkBox.setOnClickListener(clickListener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class SourceViewHolder extends RecyclerView.ViewHolder {
        final TextView nameText;
        final CheckBox checkBox;

        SourceViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.tvSourceName);
            checkBox = itemView.findViewById(R.id.cbSelected);
        }
    }
}
