package com.lako.moclock;

import android.app.Service;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Locale;

public class TimerFragment extends Fragment {

    private EditText mEditTextInput;
    private TextView mTextViewCountDown;
    private Button mButtonStartPause;
    private Button mButtonReset;
    private Button mButtonSet;

    private CountDownTimer mCountDownTimer;

    private boolean mTimerRunning;
    private boolean mTimerStopped;

    private long mStartTimeInMillis;
    private long mTimeLeftInMillis = mStartTimeInMillis;
    private long mEndTime;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_timer, container, false);

        onStart();
        mButtonSet = view.findViewById(R.id.setbutton);
        mTextViewCountDown =  view.findViewById(R.id.textfortimer);
        mButtonStartPause =  view.findViewById(R.id.start_pause);
        mButtonReset =  view.findViewById(R.id.reset);
        mEditTextInput = view.findViewById(R.id.edit_text_input);
        mTimerStopped = true;
        updateInterface();
        mButtonStartPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mTimerRunning) {
                    pauseTimer();
                } else {
                    startTimer();
                    mTimerStopped = false;
                }
            }
        });


        mButtonReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetTimer();
            }
        });

        mButtonSet.setOnClickListener(new View.OnClickListener() {
            @Override
                public void onClick(View view) {
                String input = mEditTextInput.getText().toString();
                if (input.length() == 0){
                    return;
                }
                long millisInput = Long.parseLong(input) * 60000;
                if (millisInput == 0){
                    return;
                }
                setTime(millisInput);
                mEditTextInput.setText("");
                mEditTextInput.setVisibility(View.INVISIBLE);
                mTextViewCountDown.setVisibility(View.VISIBLE);
                mButtonSet.setVisibility(View.INVISIBLE);
            }
        });

        return view;
    }

    private void setTime(long milliseconds){
        mStartTimeInMillis = milliseconds;
        resetTimerText();

    }

    private void startTimer() {
        mEndTime = System.currentTimeMillis() + mTimeLeftInMillis;

        mCountDownTimer = new CountDownTimer(mTimeLeftInMillis, 1000) {
            @Override
            public void onTick(long l) {
                mTimeLeftInMillis = mTimeLeftInMillis - 1000;
                updateCountDownText();
            }

            @Override
            public void onFinish() {
                mTimerRunning = false;
                mButtonStartPause.setText("Start");
                Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                Ringtone r = RingtoneManager.getRingtone(getActivity().getApplicationContext(), notification);
                r.play();
                updateInterface();
            }
        }.start();
        mTimerRunning = true;
        updateInterface();
    }

    private void pauseTimer() {
        mCountDownTimer.cancel();
        mTimerRunning = false;
        updateInterface();
    }

    private void resetTimer() {
        mTimeLeftInMillis = mStartTimeInMillis;
        mTimerStopped = true;
        mEditTextInput.setVisibility(View.VISIBLE);
        mButtonSet.setVisibility(View.VISIBLE);
        mTextViewCountDown.setVisibility(View.INVISIBLE);
        mButtonStartPause.setVisibility(View.INVISIBLE);
        updateCountDownText();
        updateInterface();
    }

    private void resetTimerText() {
        mTimeLeftInMillis = mStartTimeInMillis;
        updateCountDownText();
        updateInterface();
    }

    private void updateCountDownText() {
        int minutes = (int) ((mTimeLeftInMillis / 1000) % 3600) / 60;
        int seconds = (int) (mTimeLeftInMillis / 1000) % 60;
        int hours = (int) (mTimeLeftInMillis /1000) / 3600;

        String timeLeftFormatted;
        if (hours > 0) {
            timeLeftFormatted = String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds);
        }else{
            timeLeftFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        }


        mTextViewCountDown.setText(timeLeftFormatted);
    }

    private void updateInterface() {
        if (mTimerRunning) {
            mEditTextInput.setVisibility(View.INVISIBLE);
            mButtonSet.setVisibility(View.INVISIBLE);
            mTextViewCountDown.setVisibility(View.VISIBLE);
            mButtonReset.setVisibility(View.INVISIBLE);
            mButtonStartPause.setText("Pause");
        } else {
            mButtonStartPause.setText("Start");

            if (mTimeLeftInMillis < 1000) {
                mButtonStartPause.setVisibility(View.INVISIBLE);

            } else {
                mButtonStartPause.setVisibility(View.VISIBLE);
            }

            if (mTimeLeftInMillis < mStartTimeInMillis) {
                mButtonReset.setVisibility(View.VISIBLE);
            } else {
                mButtonReset.setVisibility(View.INVISIBLE);
            }
        }
    }
    public void onStop(){
        super.onStop();

        SharedPreferences prefs = this.getActivity().getSharedPreferences("pref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong("startTimeInMillis", mStartTimeInMillis);
        editor.putLong("millisLeft", mTimeLeftInMillis);
        editor.putBoolean("timerRunning", mTimerRunning);
        editor.putLong("endTime", mEndTime);

        editor.apply();

        if(mCountDownTimer != null){
            mCountDownTimer.cancel();
        }
    }

    public void OnAttach(Context context){
        super.onAttach(context);

        SharedPreferences prefs = this.getActivity().getSharedPreferences("prefs", Context.MODE_PRIVATE);

        mStartTimeInMillis = prefs.getLong("startTimeInMillis", 600000);
        mTimeLeftInMillis = prefs.getLong("millisLeft", mStartTimeInMillis);
        mTimerRunning = prefs.getBoolean("timerRunning", false);

        updateCountDownText();
        updateInterface();

        if(mTimerRunning){
            mEndTime = prefs.getLong("endTime", 0);
            mTimeLeftInMillis = mEndTime - System.currentTimeMillis();

            if(mTimeLeftInMillis < 0){
                mTimeLeftInMillis = 0;
                mTimerRunning = false;
                updateCountDownText();
                updateInterface();
            }else{
                startTimer();
            }
        }
    }
}
