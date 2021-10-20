package com.cl.myapplication.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.cl.myapplication.R;
import com.cl.myapplication.message.ConversionNoticeEvent;
import com.cl.myapplication.message.IMessage;
import com.cl.myapplication.message.LogManager;
import com.cl.myapplication.util.ListViewHolder;

import org.greenrobot.eventbus.EventBus;


public class LogFragment extends Fragment {

    private Button mBtnClear;
    private ListView mLvLogs;
    private LogAdapter mAdapter;

    private Button mBtnAutoEnd;
    private Button mWhetherHexadecimal;

    private boolean mConversionNotice = true;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_log, container, false);

        mBtnClear = (Button) view.findViewById(R.id.btn_clear_log);
        mBtnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 清空列表
                LogManager.instance().clear();
                updateList();
            }
        });
        mBtnAutoEnd = (Button) view.findViewById(R.id.btn_auto_end);
        mWhetherHexadecimal = (Button) view.findViewById(R.id.btn_whether_hexadecimal);
        mBtnAutoEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LogManager.instance().changAutoEnd();
                updateAutoEndButton();
            }
        });
        mWhetherHexadecimal.setOnClickListener((view1) -> {
            if (mConversionNotice){
                EventBus.getDefault().post(new ConversionNoticeEvent("1"));
                mConversionNotice=false;
            }else {
                EventBus.getDefault().post(new ConversionNoticeEvent("2"));
                mConversionNotice=true;
            }
        });

        mLvLogs = (ListView) view.findViewById(R.id.lv_logs);
        mAdapter = new LogAdapter();
        mLvLogs.setAdapter(mAdapter);

        updateAutoEndButton();
        return view;
    }

    public void updateAutoEndButton() {
        if (getView() != null) {
            if (LogManager.instance().isAutoEnd()) {
                mBtnAutoEnd.setText("禁止自动显示最新日志");
                mLvLogs.setSelection(mAdapter.getCount() - 1);
            } else {
                mBtnAutoEnd.setText("自动显示最新日志");
            }
        }
    }

    private static class LogAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return LogManager.instance().messages.size();
        }

        @Override
        public IMessage getItem(int position) {
            return LogManager.instance().messages.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            IMessage message = getItem(position);

            ListViewHolder holder;
            if (convertView == null) {
                holder = new ListViewHolder(R.layout.item_log, parent);
                convertView = holder.getItemView();
            } else {
                holder = (ListViewHolder) convertView.getTag();
            }

            TextView tvLog = holder.getText(R.id.tv_log);
            TextView tvNum = holder.getText(R.id.tv_num);

            tvLog.setText(message.getMessage());
            tvLog.setEnabled(message.isToSend());

            tvNum.setText(String.valueOf(position + 1));

            return convertView;
        }
    }

    public void add(IMessage message) {
        LogManager.instance().add(message);
        updateList();
    }

    public void updateList() {
        if (getView() != null) {
            mAdapter.notifyDataSetChanged();
            if (LogManager.instance().isAutoEnd()) {
                mLvLogs.setSelection(mAdapter.getCount() - 1);
            }
        }
    }
}

