package gd.phacsin.mdstech;

import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import java.util.List;

/**
 * Created by GD on 3/24/2016.
 */
public class DataAdapter extends RecyclerView.Adapter<DataAdapter.DataViewHolder> {

    private List<DataDetails> contactList;

    public DataAdapter(List<DataDetails> contactList) {
        this.contactList = contactList;
    }


    @Override
    public int getItemCount() {
        return contactList.size();
    }

    @Override
    public void onBindViewHolder(DataViewHolder contactViewHolder, int i) {
        DataDetails ci = contactList.get(i);
        contactViewHolder.FileID.setText(ci.file_id);
        contactViewHolder.PieceID.setText(ci.piece_id);

    }

    @Override
    public DataViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View itemView = LayoutInflater.
                from(viewGroup.getContext()).
                inflate(R.layout.topic_layout, viewGroup, false);
        return new DataViewHolder(itemView);
    }


    public static class DataViewHolder extends RecyclerView.ViewHolder{

        protected TextView FileID;
        protected TextView PieceID;


        public DataViewHolder(View v) {
            super(v);
            FileID = (TextView) v.findViewById(R.id.topic);
            PieceID = (TextView) v.findViewById(R.id.sub_topic);

        }


    }


}