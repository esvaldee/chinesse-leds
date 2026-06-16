package xyz.esvalde.myapplication.adapter;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.os.Build;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.*;
import xyz.esvalde.myapplication.R;
import java.util.List;

public class BleDeviceAdapter extends RecyclerView.Adapter<BleDeviceAdapter.ViewHolder> {

    public interface OnDeviceClickListener {
        void onDeviceClick(BluetoothDevice device);
    }

    private final List<BluetoothDevice> devices;
    private final OnDeviceClickListener listener;

    public BleDeviceAdapter(List<BluetoothDevice> devices, OnDeviceClickListener listener) {
        this.devices  = devices;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_device, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BluetoothDevice device = devices.get(position);
        String name = null;
        if (ActivityCompat.checkSelfPermission(holder.itemView.getContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            name = device.getName();
        }

        holder.tvName.setText(name != null ? name : "Nieznane urządzenie");
        holder.tvAddress.setText(device.getAddress());

        boolean likelyLed = name != null && (
                name.toUpperCase().contains("LED") ||
                        name.toUpperCase().contains("BLE") ||
                        name.toUpperCase().contains("STRIP") ||
                        name.toUpperCase().contains("LIGHT") ||
                        name.toUpperCase().contains("ELK") ||
                        name.toUpperCase().contains("HAPPY") ||
                        name.toUpperCase().contains("TRIONES") ||
                        name.toUpperCase().contains("QHM")
        );
        holder.tvTag.setVisibility(likelyLed ? View.VISIBLE : View.GONE);
        holder.itemView.setOnClickListener(v -> listener.onDeviceClick(device));
    }

    @Override
    public int getItemCount() { return devices.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvAddress, tvTag;
        ViewHolder(View v) {
            super(v);
            tvName    = v.findViewById(R.id.tv_device_name);
            tvAddress = v.findViewById(R.id.tv_device_address);
            tvTag     = v.findViewById(R.id.tv_device_tag);
        }
    }
}