package gd.phacsin.mdstech;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.snappydb.DB;
import com.snappydb.DBFactory;
import com.snappydb.SnappyDB;
import com.snappydb.SnappydbException;

import java.util.ArrayList;
import java.util.List;

import lecho.lib.hellocharts.model.PieChartData;
import lecho.lib.hellocharts.model.SliceValue;
import lecho.lib.hellocharts.util.ChartUtils;
import lecho.lib.hellocharts.view.PieChartView;

/**
 * Created by GD on 2/3/2016.
 */
public class UsageFragment extends Fragment {
    PieChartView pieChartView;
    DB snappydb;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_usage, container, false);
        pieChartView = (PieChartView)rootView. findViewById(R.id.chart);
        List<SliceValue> values = new ArrayList<SliceValue>();
        try {
            snappydb = DBFactory.open(getActivity());
            SliceValue sliceValue = new SliceValue(snappydb.countKeys("data"), ChartUtils.pickColor());
            values.add(sliceValue);
            sliceValue = new SliceValue(20, ChartUtils.pickColor());
            values.add(sliceValue);
            PieChartData data = new PieChartData(values);
            data.setHasLabels(true);
            pieChartView.setPieChartData(data);
            snappydb.close();

        } catch (SnappydbException e) {
        }
        return rootView;
    }

}
