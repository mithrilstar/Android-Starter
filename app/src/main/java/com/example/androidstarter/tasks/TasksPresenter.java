package com.example.androidstarter.tasks;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.OnLifecycleEvent;
import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.Nullable;

import com.example.androidstarter.custom.DataViewState;
import com.example.androidstarter.data.database.AppDatabase;
import com.example.androidstarter.data.models.Task;

import java.util.List;

import timber.log.Timber;

/**
 * Created by samvedana on 3/1/18.
 */

public class TasksPresenter implements TasksContract.Presenter, LifecycleObserver {
    private static TasksPresenter instance;

    private AppDatabase appDatabase;
    private static TasksContract.View tasksView;

    private TasksPresenter(AppDatabase database, TasksContract.View view) {
        Timber.d("TasksPresenter constructor");
        appDatabase = database;
        attachView(view);
        loadTasks(false);
    }

    public  static TasksPresenter getInstance(AppDatabase database, TasksContract.View view) {
        if (instance == null || tasksView == null) {
            //seems hacky for some reason.. dunno why though
            instance = new TasksPresenter(database, view);
        }
        return instance;
    }

    @Override
    public void attachView(TasksContract.View view) {
        if (tasksView == null) {
            tasksView = view;
            // Initialize this presenter as a lifecycle-aware when a view is a lifecycle owner.
            if (tasksView instanceof LifecycleOwner) {
                ((LifecycleOwner) tasksView).getLifecycle().addObserver(this);
                Timber.d("TasksPresenter added as observer of %s",
                        tasksView.getClass().toString());
            }
        }
    }

    @Override
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void onAttach() {
        Timber.d("ON_RESUME event");
        //loadTasks(false);
    }

    @Override
    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void onDetach() {
        //next time a fresh view ought to be used
        if (tasksView != null) {
            ((LifecycleOwner) tasksView).getLifecycle().removeObserver(this);
            tasksView = null;
        }
    }

    @Override
    public void loadTasks(boolean onlineRequired) {
        Timber.d("in loadTasks");
        //query db for tasks
        LiveData<List<Task>> tasksLiveData = appDatabase.taskDao().getAll();

        // Create the observer which updates the UI.
        final Observer<List<Task>> observer = new Observer<List<Task>>() {
            @Override
            public void onChanged(@Nullable final List<Task> taskList) {
                Timber.d("Task list updated fromdb");
                // Update the UI
                if (taskList == null) {
                    Timber.e("empty task list instance from db");
                    tasksView.switchState(DataViewState.NO_DATA);
                    return; //this is error or no data condition todo communicate better
                }
                //tasksView.stopLoadingIndicator();
                // no longer needs to be handled since state changes will be handled in showTasks
                tasksView.showTasks(taskList);

            }
        };

        // Observe the LiveData, passing in this activity as the LifecycleOwner and the observer.
        tasksLiveData.observe(tasksView, observer);
    }

}
