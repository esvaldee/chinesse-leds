package xyz.esvalde.myapplication.adapter;

import android.graphics.Color;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.*;
import xyz.esvalde.myapplication.R;

public class ColorTableAdapter extends RecyclerView.Adapter<ColorTableAdapter.ViewHolder> {

    public interface OnColorPickedListener {
        void onColorPicked(int r, int g, int b);
    }

    private final int[][] colors;
    private final String[] names;
    private final OnColorPickedListener listener;
    private int selectedPos = -1;

    public ColorTableAdapter(int[][] colors, String[] names, OnColorPickedListener listener) {
        this.colors   = colors;
        this.names    = names;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_color, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        int[] c = colors[position];
        int col = Color.rgb(c[0], c[1], c[2]);
        holder.colorSwatch.setBackgroundColor(col);
        holder.tvName.setText(names[position]);

        double lum = 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2];
        holder.tvName.setTextColor(lum > 128 ? Color.BLACK : Color.WHITE);

        holder.itemView.setAlpha(selectedPos == position ? 1.0f : 0.88f);
        holder.itemView.setScaleX(selectedPos == position ? 1.05f : 1.0f);
        holder.itemView.setScaleY(selectedPos == position ? 1.05f : 1.0f);

        holder.itemView.setOnClickListener(v -> {
            int prev = selectedPos;
            selectedPos = holder.getAdapterPosition();
            notifyItemChanged(prev);
            notifyItemChanged(selectedPos);
            listener.onColorPicked(c[0], c[1], c[2]);
        });
    }

    @Override
    public int getItemCount() { return colors.length; }

    static class ViewHolder extends RecyclerView.ViewHolder {
        View colorSwatch;
        TextView tvName;
        ViewHolder(View v) {
            super(v);
            colorSwatch = v.findViewById(R.id.color_swatch);
            tvName      = v.findViewById(R.id.tv_color_name);
        }
    }
}