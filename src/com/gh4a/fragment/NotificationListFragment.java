package com.gh4a.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.Loader;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.gh4a.BackgroundTask;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.adapter.NotificationAdapter;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.loader.NotificationHolder;
import com.gh4a.loader.NotificationListLoader;
import com.gh4a.utils.IntentUtils;

import org.eclipse.egit.github.core.Notification;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.service.NotificationService;

import java.io.IOException;
import java.util.List;

public class NotificationListFragment extends LoadingListFragmentBase implements
        RootAdapter.OnItemClickListener<NotificationHolder>,NotificationAdapter.OnNotificationActionCallback {
    private NotificationAdapter mAdapter;

    public static NotificationListFragment newInstance() {
        return new NotificationListFragment();
    }

    private final LoaderCallbacks<List<NotificationHolder>> mNotificationsCallback =
            new LoaderCallbacks<List<NotificationHolder>>(this) {
        @Override
        protected Loader<LoaderResult<List<NotificationHolder>>> onCreateLoader() {
            return new NotificationListLoader(getContext());
        }

        @Override
        protected void onResultReady(List<NotificationHolder> result) {
            mAdapter.clear();
            mAdapter.addAll(result);
            setContentShown(true);
            mAdapter.notifyDataSetChanged();
            updateEmptyState();
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setContentShown(false);
        getLoaderManager().initLoader(0, null, mNotificationsCallback);
    }

    @Override
    protected int getEmptyTextResId() {
        return R.string.no_notifications_found;
    }

    @Override
    public void onRefresh() {
        if (mAdapter != null) {
            mAdapter.clear();
        }
        hideContentAndRestartLoaders(0);
    }

    @Override
    protected void onRecyclerViewInflated(RecyclerView view, LayoutInflater inflater) {
        super.onRecyclerViewInflated(view, inflater);
        mAdapter = new NotificationAdapter(getActivity(), this);
        mAdapter.setOnItemClickListener(this);
        view.setAdapter(mAdapter);
        updateEmptyState();
    }

    @Override
    protected boolean hasDividers() {
        return false;
    }

    @Override
    protected boolean hasCards() {
        return true;
    }

    @Override
    public void onItemClick(NotificationHolder item) {
        if (item.notification == null) {
            IntentUtils.openRepositoryInfoActivity(getActivity(), item.repository);
        } else {
            // TODO: Parse url
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.notification_list_menu, menu);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mark_all_as_read:
                new MarkReadTask(null, null).schedule();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void markAsRead(NotificationHolder notificationHolder) {
        if (notificationHolder.notification == null) {
            new MarkReadTask(notificationHolder.repository, null).schedule();
        } else {
            new MarkReadTask(null, notificationHolder.notification).schedule();
        }
    }

    @Override
    public void unsubscribe(NotificationHolder notificationHolder) {
        new UnsubscribeTask(notificationHolder.notification).schedule();
    }

    private class MarkReadTask extends BackgroundTask<Void> {
        @Nullable
        private final Repository mRepository;
        @Nullable
        private final Notification mNotification;

        public MarkReadTask(@Nullable Repository repository, @Nullable Notification notification) {
            super(getActivity());
            mRepository = repository;
            mNotification = notification;
        }

        @Override
        protected Void run() throws IOException {
            NotificationService notificationService = (NotificationService)
                    Gh4Application.get().getService(Gh4Application.NOTIFICATION_SERVICE);

            if (mNotification != null) {
                notificationService.markThreadAsRead(mNotification.getId());
            } else if (mRepository != null) {
                notificationService.markNotificationsAsRead(mRepository);
            } else {
                notificationService.markNotificationsAsRead();
            }

            return null;
        }

        @Override
        protected void onSuccess(Void result) {
            mAdapter.markAsRead(mRepository, mNotification);
        }
    }

    private class UnsubscribeTask extends BackgroundTask<Void> {
        private final Notification mNotification;

        public UnsubscribeTask(Notification notification) {
            super(getActivity());
            mNotification = notification;
        }

        @Override
        protected Void run() throws IOException {
            NotificationService notificationService = (NotificationService)
                    Gh4Application.get().getService(Gh4Application.NOTIFICATION_SERVICE);

            notificationService.setThreadSubscription(mNotification.getId(), false, true);
            return null;
        }

        @Override
        protected void onSuccess(Void result) {
            mAdapter.markAsRead(null, mNotification);
        }
    }
}
