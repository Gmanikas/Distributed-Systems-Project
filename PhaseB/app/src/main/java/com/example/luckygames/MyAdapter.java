package com.example.luckygames;

import com.example.luckygames.shared.models.Game;

import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import androidx.annotation.NonNull;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;

import java.util.List;

public class MyAdapter extends Adapter<MyAdapter.MyViewHolder> {

    private List<Game> games;

    public MyAdapter(List<Game> games) {
        this.games = games;
    }

    public static class MyViewHolder extends ViewHolder {

        ImageView logo;
        TextView name, risk, stars, jackpot;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            logo = itemView.findViewById(R.id.ivGameIcon);
            name = itemView.findViewById(R.id.tvGameName);
            risk = itemView.findViewById(R.id.tvGameRisk);
            stars = itemView.findViewById(R.id.tvGameRating);
            jackpot = itemView.findViewById(R.id.tvGameJackpot);
        }
    }

    // Otan ftiaxnoume neo instance tou MyAdapter, ftiaxnete gia kathe game mesa sto games list ena neo item_game.xml
    @NonNull // Leei sto susthma oti den prepei na einai pote null
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_game, parent, false);
        return new MyViewHolder(view);
    }

    // Otan prospathhsomue na baloume auta ta item_games mesa sto activity_results, tha perasoume prwta apo edw, apo opou thetontai oles oi plhrifories (logo, name, ...)
    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Game currentGame = games.get(position);

        //Thetoume to logo tou game
        String logoPath = currentGame.getLogoPath();
        String drawableName = "default_logo"; // default_logo = fallback
        if (logoPath != null && !logoPath.isEmpty()) {
            // Bgazoume to data/logo/, gia na meinei mono to cyber_poker.png
            String logoFileName = logoPath.substring(logoPath.lastIndexOf('/') + 1);
            // Bgazoume to .png an uparxei
            if (logoFileName.contains(".")) {
                drawableName = logoFileName.substring(0, logoFileName.lastIndexOf('.'));
            } else {
                drawableName = logoFileName;
            }
        }
        // Psaxnoume to id tou cyber_poker.png file mesa sto res.drawable
        int logoFileId = holder.itemView.getContext().getResources()
                .getIdentifier(drawableName, "drawable", holder.itemView.getContext().getPackageName());
        // Topothetoume to logo
        if (logoFileId != 0) {
            holder.logo.setImageResource(logoFileId);
        }

        // Thetoume ta asteria
        int stars = currentGame.getStars();
        switch (stars) {
            case 1 -> holder.stars.setText("⭐");
            case 2 -> holder.stars.setText("⭐⭐");
            case 3 -> holder.stars.setText("⭐⭐⭐");
            case 4 -> holder.stars.setText("⭐⭐⭐⭐");
            case 5 -> holder.stars.setText("⭐⭐⭐⭐⭐");
        }

        // Thetoume to name, to risk kai to jackpot
        holder.name.setText(currentGame.getName());
        holder.risk.setText(currentGame.getRisk());
        holder.jackpot.setText(String.valueOf(currentGame.getJackpot()));

    }

    @Override
    public int getItemCount() {
        return games.size();
    }

}
