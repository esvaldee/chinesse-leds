package xyz.esvalde.myapplication.adapter.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.*;
import xyz.esvalde.myapplication.MainActivity;
import xyz.esvalde.myapplication.R;
import xyz.esvalde.myapplication.adapter.ColorTableAdapter;

public class ControllerFragment extends Fragment {

    private MainActivity mainActivity;
    private SeekBar seekR, seekG, seekB, seekBrightness;
    private View colorPreview;
    private TextView tvR, tvG, tvB;

    private final int[][] COLOR_TABLE = {
            {255,   0,   0}, {255, 127,   0}, {255, 255,   0}, {  0, 255,   0},
            {  0, 255, 255}, {  0,   0, 255}, {127,   0, 255}, {255,   0, 255},
            {255, 255, 255}, {255, 200, 150}, {150, 200, 255}, {  0,   0,   0},
            {255,  20,  60}, {  0, 255, 100}, { 60,  60, 255}, {255, 100,   0},
            {200, 255,   0}, { 80,   0, 200}, {255, 150, 200}, {150, 255, 200}
    };

    private final String[] COLOR_NAMES = {
            "Czerwony", "Pomarańczowy", "Żółty", "Zielony", "Cyjan",
            "Niebieski", "Fioletowy", "Różowy", "Biały", "Ciepła biel",
            "Zimna biel", "Wyłącz", "Neon czerw.", "Neon ziel.", "Neon nieb.",
            "Bursztynowy", "Limonkowy", "Indygo", "Pastelowy róż", "Miętowy"
    };

    private final String[] EFFECT_NAMES = { "Tęcza", "Pulsowanie", "Miganie", "Oddychanie", "Fala", "Strobe" };
    private final int[] EFFECT_CODES = { 0x85, 0x97, 0x87, 0x88, 0x89, 0x8d };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_controller, container, false);
        mainActivity = (MainActivity) getActivity();

        colorPreview    = view.findViewById(R.id.color_preview);
        seekR           = view.findViewById(R.id.seek_r);
        seekG           = view.findViewById(R.id.seek_g);
        seekB           = view.findViewById(R.id.seek_b);
        seekBrightness  = view.findViewById(R.id.seek_brightness);
        tvR             = view.findViewById(R.id.tv_r);
        tvG             = view.findViewById(R.id.tv_g);
        tvB             = view.findViewById(R.id.tv_b);

        SeekBar.OnSeekBarChangeListener rgbListener = new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean user) { updateColorFromSliders(); }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) { sendCurrentColor(); }
        };
        seekR.setOnSeekBarChangeListener(rgbListener);
        seekG.setOnSeekBarChangeListener(rgbListener);
        seekB.setOnSeekBarChangeListener(rgbListener);

        seekBrightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean user) {}
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {
                if(mainActivity != null) mainActivity.sendBrightness(s.getProgress());
            }
        });

        view.findViewById(R.id.btn_on).setOnClickListener(v -> { if(mainActivity != null) mainActivity.sendPower(true); });
        view.findViewById(R.id.btn_off).setOnClickListener(v -> { if(mainActivity != null) mainActivity.sendPower(false); });

        RecyclerView rvColors = view.findViewById(R.id.rv_colors);
        rvColors.setLayoutManager(new GridLayoutManager(getContext(), 4));
        rvColors.setAdapter(new ColorTableAdapter(COLOR_TABLE, COLOR_NAMES, (r, g, b) -> {
            seekR.setProgress(r);
            seekG.setProgress(g);
            seekB.setProgress(b);
            updateColorFromSliders();
            if(mainActivity != null) mainActivity.sendColor(r, g, b);
        }));

        LinearLayout layoutEffects = view.findViewById(R.id.layout_effects);
        for (int i = 0; i < EFFECT_NAMES.length; i++) {
            final int code = EFFECT_CODES[i];
            Button btn = new Button(getContext());
            btn.setText(EFFECT_NAMES[i]);
            btn.setTextSize(11f);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(2, 2, 2, 2);
            btn.setLayoutParams(lp);
            btn.setOnClickListener(v -> { if(mainActivity != null) mainActivity.sendEffect(code); });
            layoutEffects.addView(btn);
        }

        return view;
    }

    private void updateColorFromSliders() {
        int r = seekR.getProgress();
        int g = seekG.getProgress();
        int b = seekB.getProgress();
        tvR.setText("R: " + r);
        tvG.setText("G: " + g);
        tvB.setText("B: " + b);
        colorPreview.setBackgroundColor(Color.rgb(r, g, b));
    }

    private void sendCurrentColor() {
        if(mainActivity != null) mainActivity.sendColor(seekR.getProgress(), seekG.getProgress(), seekB.getProgress());
    }
}