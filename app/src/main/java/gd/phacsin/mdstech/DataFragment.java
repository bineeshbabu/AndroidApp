package gd.phacsin.mdstech;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.snappydb.DB;
import com.snappydb.DBFactory;
import com.snappydb.SnappydbException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by GD on 3/24/2016.
 */
public class DataFragment extends Fragment {
    RecyclerView recyclerView;
    List<DataDetails> data = new ArrayList<>();
    DB snappydb;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_data, container, false);

        recyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerview);
        recyclerView.setHasFixedSize(true);

        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(llm);
        try {
            snappydb = DBFactory.open(getActivity());
            for(int i=0;i<snappydb.countKeys("file_id");i++)
            {
                DataDetails dataDetails = new DataDetails();
                dataDetails.file_id=snappydb.get("file_id:"+String.valueOf(i));
                dataDetails.piece_id=snappydb.get("piece_id:"+String.valueOf(i));
                data.add(dataDetails);
            }
            snappydb.close();

        } catch (SnappydbException e) {
        }
        DataAdapter ca = new DataAdapter(data);
        recyclerView.setAdapter(ca);
        return rootView;
    }
}
