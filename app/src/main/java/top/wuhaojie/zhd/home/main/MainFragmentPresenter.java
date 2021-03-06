package top.wuhaojie.zhd.home.main;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import rx.Subscriber;
import top.wuhaojie.zhd.base.interfaces.BasePresenter;
import top.wuhaojie.zhd.base.interfaces.BaseView;
import top.wuhaojie.zhd.data.HttpUtils;
import top.wuhaojie.zhd.entities.BeforeMessageResponse;
import top.wuhaojie.zhd.entities.LatestMessageResponse;
import top.wuhaojie.zhd.home.main.adapter.MainContentListAdapter;
import top.wuhaojie.zhd.manager.IntentManager;
import top.wuhaojie.zhd.utils.StringUtils;

/**
 * Created by wuhaojie on 17-2-9.
 */

public class MainFragmentPresenter implements BasePresenter {

    private static final String TAG = "MainFragmentPresenter";
    private Context mContext;
    private MainFragmentView mView;
    private ArrayList<String> mStoryIds;

    public MainFragmentPresenter(Context context) {
        mContext = context;
    }

    @Override
    public void bindView(BaseView view) {
        mView = (MainFragmentView) view;
    }

    private void loadTodayData() {
        HttpUtils.getLatestMessages(new Subscriber<LatestMessageResponse>() {
            @Override
            public void onCompleted() {
                mView.loadCompleted();
            }

            @Override
            public void onError(Throwable e) {
                Log.e(TAG, "onError: ", e);
                // show error
//                mView.showSnackBar("网络错误...");
                mView.loadCompleted();
            }

            @Override
            public void onNext(LatestMessageResponse latestMessageResponse) {
                Log.d(TAG, "onNext: Message = " + latestMessageResponse);
                handleResult(latestMessageResponse);
            }
        });
    }

    private void handleResult(LatestMessageResponse latestMessageResponse) {
        List<LatestMessageResponse.TopStoriesBean> topStories = latestMessageResponse.getTop_stories();

        ArrayList<MainContentListAdapter.Item> items = new ArrayList<>();
        for (LatestMessageResponse.TopStoriesBean story : topStories) {
            MainContentListAdapter.Item item = new MainContentListAdapter.Item();
            item.imgUrl = story.getImage();
            item.title = story.getTitle();
            item.id = story.getId();
            items.add(item);
        }
        mView.setBanner(items);

        List<LatestMessageResponse.StoriesBean> stories = latestMessageResponse.getStories();
        mStoryIds = new ArrayList<>();
        items.clear();
        for (LatestMessageResponse.StoriesBean st : stories) {
            MainContentListAdapter.Item item = new MainContentListAdapter.Item();
            item.title = st.getTitle();
            item.imgUrl = st.getImages().get(0);
            item.id = st.getId();
            items.add(item);

            mStoryIds.add(String.valueOf(item.id));
        }
        mView.setListContent(items);
        topStories = null;
        items = null;
    }

    public void onRefresh() {
        Log.d(TAG, "onRefresh: onRefresh()");
        mView.resetPage();
        loadTodayData();
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        loadTodayData();
    }

    public void onMainContentListItemClick(int adapterPosition, MainContentListAdapter.Item item) {
        // mark as read
        item.state = MainContentListAdapter.Item.STATE.READ;
        mView.notifyListItemChanged(adapterPosition);
        // intent
        Intent intent = IntentManager.toDetailActivity(mContext, mStoryIds, String.valueOf(item.id));
        mContext.startActivity(intent);
    }

    public void onLoadMore(int page) {
        Log.d(TAG, "load more: " + page);
        loadMoreDate(page);
    }

    private void loadMoreDate(int page) {
        HttpUtils.getBeforeMessage(page, new Subscriber<BeforeMessageResponse>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
            }

            @Override
            public void onNext(BeforeMessageResponse beforeMessageResponse) {
                Log.d(TAG, "onNext: " + beforeMessageResponse);
                List<BeforeMessageResponse.StoriesBean> stories = beforeMessageResponse.getStories();
                ArrayList<MainContentListAdapter.Item> list = new ArrayList<>();
                for (BeforeMessageResponse.StoriesBean story : stories) {
                    MainContentListAdapter.Item item = new MainContentListAdapter.Item();
                    item.imgUrl = story.getImages().get(0);
                    item.title = story.getTitle();
                    item.id = story.getId();
                    list.add(item);

                    mStoryIds.add(String.valueOf(item.id));

                }
                String date = beforeMessageResponse.getDate();
                try {
                    date = StringUtils.str2DateWeek(date);
                } catch (ParseException e) {
                    Log.e(TAG, "onNext: ", e);
                }
                mView.appendListContent(list, date);
                list = null;
                stories = null;
            }
        });
    }

    public void onHeaderClickListener(String clickedStoryId, ArrayList<String> bannerIds) {
        Intent intent = IntentManager.toDetailActivity(mContext, bannerIds, clickedStoryId);
        mContext.startActivity(intent);
    }
}
