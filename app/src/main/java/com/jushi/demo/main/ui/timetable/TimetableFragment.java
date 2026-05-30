package com.jushi.demo.main.ui.timetable;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.jushi.demo.main.R;
import com.jushi.demo.main.data.CourseDatabase;
import com.jushi.demo.main.data.entity.Course;
import com.jushi.demo.main.ui.imports.ImportTimetableActivity;

import java.util.List;

public class TimetableFragment extends Fragment {

    private TimetableGridView gridView;
    private PeriodLabelView periodLabelView;
    private TextView tvWeekInfo;
    private View emptyState;
    private View weekSelector;
    private View timetableContainer;

    private int currentWeek;
    private long semesterStart;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_timetable, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        gridView = view.findViewById(R.id.timetableGrid);
        periodLabelView = view.findViewById(R.id.periodLabelColumn);
        tvWeekInfo = view.findViewById(R.id.tvWeekInfo);
        emptyState = view.findViewById(R.id.emptyState);
        weekSelector = view.findViewById(R.id.weekSelector);
        timetableContainer = view.findViewById(R.id.timetableContainer);
        TextView btnPrevWeek = view.findViewById(R.id.btnPrevWeek);
        TextView btnNextWeek = view.findViewById(R.id.btnNextWeek);

        gridView.setShowLabelColumn(false);

        semesterStart = WeekUtils.getSemesterStart(requireContext());
        currentWeek = WeekUtils.getCurrentWeek(requireContext());

        btnPrevWeek.setOnClickListener(v -> changeWeek(-1));
        btnNextWeek.setOnClickListener(v -> changeWeek(1));

        view.findViewById(R.id.btnGoImport).setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), ImportTimetableActivity.class));
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAndDisplay();
    }

    private void loadAndDisplay() {
        new Thread(() -> {
            CourseDatabase db = CourseDatabase.getInstance(requireContext());
            List<Course> courses = db.courseDao().getAllCourses();

            requireActivity().runOnUiThread(() -> {
                if (courses.isEmpty()) {
                    emptyState.setVisibility(View.VISIBLE);
                    weekSelector.setVisibility(View.GONE);
                    timetableContainer.setVisibility(View.GONE);
                } else {
                    emptyState.setVisibility(View.GONE);
                    weekSelector.setVisibility(View.VISIBLE);
                    timetableContainer.setVisibility(View.VISIBLE);

                    gridView.setCourses(courses);
                    updateWeekDisplay();
                }
            });
        }).start();
    }

    private void changeWeek(int delta) {
        int newWeek = currentWeek + delta;
        if (newWeek < 1) newWeek = 1;
        if (newWeek > 30) newWeek = 30;
        currentWeek = newWeek;
        WeekUtils.setCurrentWeek(requireContext(), currentWeek);
        updateWeekDisplay();
    }

    private void updateWeekDisplay() {
        gridView.setCurrentWeek(currentWeek, semesterStart);
        String range = WeekUtils.getWeekDateRange(currentWeek, semesterStart);
        tvWeekInfo.setText(getString(R.string.timetable_week_label, currentWeek, range));
    }
}
