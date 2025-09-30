package com.cl.myapplication.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import androidx.databinding.DataBindingUtil;

import com.cl.myapplication.R;
import com.cl.myapplication.databinding.ItemDeviceBinding;


public class SpAdapter extends BaseAdapter {

    String[] datas;
    Context mContext;


    public SpAdapter(Context context) {
        this.mContext = context;
    }

    public void setDatas(String[] datas) {
        this.datas = datas;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return datas == null ? 0 : datas.length;
    }

    @Override
    public Object getItem(int position) {
        return datas == null ? null : datas[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ItemDeviceBinding binding;
        if (convertView == null) {
            binding = DataBindingUtil.inflate(LayoutInflater.from(mContext), R.layout.item_device, parent, false);
            convertView = binding.getRoot();
            convertView.setTag(binding);
        } else {
            binding = (ItemDeviceBinding) convertView.getTag();
        }

        binding.tvDevice.setText(datas[position]);

        return convertView;
    }
}
