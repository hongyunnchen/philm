package app.philm.in.fragments;

import com.google.common.base.Preconditions;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import app.philm.in.Constants;
import app.philm.in.PhilmApplication;
import app.philm.in.R;
import app.philm.in.controllers.MovieController;
import app.philm.in.fragments.base.BasePhilmMovieFragment;
import app.philm.in.model.PhilmMovie;
import app.philm.in.util.FlagUrlProvider;
import app.philm.in.util.ImageHelper;
import app.philm.in.util.PhilmCollections;
import app.philm.in.util.ViewUtils;
import app.philm.in.view.CheatSheet;
import app.philm.in.view.CheckableImageButton;
import app.philm.in.view.MovieDetailInfoLayout;
import app.philm.in.view.PhilmImageView;
import app.philm.in.view.RatingBarLayout;
import app.philm.in.view.ViewRecycler;

public class MovieDetailFragment extends BasePhilmMovieFragment
        implements MovieController.MovieDetailUi, View.OnClickListener {

    private static final Date DATE = new Date();

    private static final String LOG_TAG = MovieDetailFragment.class.getSimpleName();

    private static final String KEY_QUERY_MOVIE_ID = "movie_id";

    private PhilmMovie mMovie;

    @Inject ImageHelper mImageHelper;
    @Inject FlagUrlProvider mFlagUrlProvider;
    @Inject DateFormat mMediumDateFormatter;

    private TextView mTitleTextView;
    private TextView mSummaryTextView;
    private PhilmImageView mFanartImageView;
    private PhilmImageView mPosterImageView;

    private RatingBarLayout mRatingBarLayout;

    private ViewSwitcher mRelatedSwitcher;
    private LinearLayout mRelatedLayout;

    private MovieDetailInfoLayout mReleasedInfoLayout;
    private MovieDetailInfoLayout mRunTimeInfoLayout;
    private MovieDetailInfoLayout mCertificationInfoLayout;
    private MovieDetailInfoLayout mGenresInfoLayout;
    private MovieDetailInfoLayout mLanguageInfoLayout;

    private CheckableImageButton mSeenButton, mWatchlistButton, mCollectionButton;

    public static MovieDetailFragment create(String movieId) {
        Preconditions.checkArgument(!TextUtils.isEmpty(movieId), "movieId cannot be empty");

        Bundle bundle = new Bundle();
        bundle.putString(KEY_QUERY_MOVIE_ID, movieId);

        MovieDetailFragment fragment = new MovieDetailFragment();
        fragment.setArguments(bundle);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PhilmApplication.from(getActivity()).inject(this);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_movie_detail, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mFanartImageView = (PhilmImageView) view.findViewById(R.id.imageview_fanart);
        mPosterImageView = (PhilmImageView) view.findViewById(R.id.imageview_poster);
        mTitleTextView = (TextView) view.findViewById(R.id.textview_title);
        mRatingBarLayout = (RatingBarLayout) view.findViewById(R.id.rating_bar_layout);

        mSummaryTextView = (TextView) view.findViewById(R.id.textview_summary);
        mSummaryTextView.setOnClickListener(this);

        mSeenButton = (CheckableImageButton) view.findViewById(R.id.btn_seen);
        mSeenButton.setOnClickListener(this);
        CheatSheet.setup(mSeenButton);

        mWatchlistButton = (CheckableImageButton) view.findViewById(R.id.btn_watchlist);
        mWatchlistButton.setOnClickListener(this);
        CheatSheet.setup(mWatchlistButton);

        mCollectionButton = (CheckableImageButton) view.findViewById(R.id.btn_collection);
        mCollectionButton.setOnClickListener(this);
        CheatSheet.setup(mCollectionButton);

        mRelatedSwitcher = (ViewSwitcher) view.findViewById(R.id.viewswitcher_related);
        mRelatedLayout = (LinearLayout) view.findViewById(R.id.layout_related);

        mRunTimeInfoLayout = (MovieDetailInfoLayout) view.findViewById(R.id.layout_info_runtime);
        mCertificationInfoLayout = (MovieDetailInfoLayout)
                view.findViewById(R.id.layout_info_certification);
        mGenresInfoLayout = (MovieDetailInfoLayout) view.findViewById(R.id.layout_info_genres);
        mReleasedInfoLayout = (MovieDetailInfoLayout) view.findViewById(R.id.layout_info_released);
        mLanguageInfoLayout = (MovieDetailInfoLayout) view.findViewById(R.id.layout_info_language);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.movies_detail, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                if (hasCallbacks()) {
                    getCallbacks().refresh();
                    return true;
                }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void setMovie(PhilmMovie movie) {
        mMovie = movie;
        populateUi();

        if (movie != null && hasCallbacks()) {
            getCallbacks().onTitleChanged(movie.getTitle());
        }
    }

    @Override
    public void showRelatedMoviesLoadingProgress(final boolean visible) {
        mRelatedSwitcher.setDisplayedChild(visible ? 1 : 0);
    }

    @Override
    public void setRateCircleEnabled(boolean enabled) {
        mRatingBarLayout.setRatingCircleEnabled(enabled);
    }

    @Override
    public void setCollectionButtonEnabled(boolean enabled) {
        mCollectionButton.setEnabled(enabled);
    }

    @Override
    public void setWatchlistButtonEnabled(boolean enabled) {
        mWatchlistButton.setEnabled(enabled);
    }

    @Override
    public void setToggleWatchedButtonEnabled(boolean enabled) {
        mSeenButton.setEnabled(enabled);
    }

    @Override
    public MovieController.MovieQueryType getMovieQueryType() {
        return MovieController.MovieQueryType.DETAIL;
    }

    @Override
    public String getRequestParameter() {
        return getArguments().getString(KEY_QUERY_MOVIE_ID);
    }

    @Override
    public String getUiTitle() {
        if (mMovie != null) {
            return mMovie.getTitle();
        }
        return null;
    }

    @Override
    public boolean isModal() {
        return false;
    }

    private void populateUi() {
        if (mMovie == null) {
            return;
        }

        if (mFanartImageView.getDrawable() == null) {
            mFanartImageView.loadBackdropUrl(mMovie);
        }

        if (mPosterImageView.getDrawable() == null) {
            mPosterImageView.loadPosterUrl(mMovie);
        }

        mTitleTextView.setText(getString(R.string.movie_title_year, mMovie.getTitle(),
                mMovie.getYear()));

        if (ViewUtils.isEmpty(mSummaryTextView)) {
            mSummaryTextView.setText(mMovie.getOverview());
        }

        updateButtonState(mSeenButton, mMovie.isWatched(), R.string.action_mark_seen,
                R.string.action_mark_unseen);
        updateButtonState(mWatchlistButton, mMovie.inWatchlist(), R.string.action_add_watchlist,
                R.string.action_remove_watchlist);
        updateButtonState(mCollectionButton, mMovie.inCollection(), R.string.action_add_collection,
                R.string.action_remove_collection);

        if (mMovie.getUserRatingAdvanced() != PhilmMovie.NOT_SET) {
            mRatingBarLayout.showUserRating(mMovie.getUserRatingAdvanced());
        } else {
            mRatingBarLayout.showRatePrompt();
        }
        mRatingBarLayout.setRatingGlobalPercentage(mMovie.getAverageRatingPercent());
        mRatingBarLayout.setRatingGlobalVotes(mMovie.getAverageRatingVotes());
        mRatingBarLayout.setRatingCircleClickListener(this);

        final List<PhilmMovie> related = mMovie.getRelated();
        if (related == null || related.size() != mRelatedLayout.getChildCount()) {
            populateRelatedMovies();
        }

        if (mMovie.getRuntime() > 0) {
            mRunTimeInfoLayout.setContentText(
                    getString(R.string.movie_details_runtime_content, mMovie.getRuntime()));
            mRunTimeInfoLayout.setVisibility(View.VISIBLE);
        }
        if (!TextUtils.isEmpty(mMovie.getCertification())) {
            mCertificationInfoLayout.setContentText(mMovie.getCertification());
            mCertificationInfoLayout.setVisibility(View.VISIBLE);
        }
        if (!TextUtils.isEmpty(mMovie.getGenres())) {
            mGenresInfoLayout.setContentText(mMovie.getGenres());
            mGenresInfoLayout.setVisibility(View.VISIBLE);
        }
        if (mMovie.getReleasedTime() > 0) {
            DATE.setTime(mMovie.getReleasedTime());
            mReleasedInfoLayout.setContentText(mMediumDateFormatter.format(DATE));
            mReleasedInfoLayout.setVisibility(View.VISIBLE);

            final String countryCode = mMovie.getReleaseCountryCode();
            if (!TextUtils.isEmpty(countryCode)) {
                loadFlagImage(countryCode, mReleasedInfoLayout);
            }
        }

        if (!TextUtils.isEmpty(mMovie.getMainLanguageTitle())) {
            mLanguageInfoLayout.setContentText(mMovie.getMainLanguageTitle());
            mLanguageInfoLayout.setVisibility(View.VISIBLE);
        }
    }

    private void loadFlagImage(final String countryCode, final MovieDetailInfoLayout infoLayout) {
        final String flagUrl = mFlagUrlProvider.getCountryFlagUrl(countryCode);
        final int width = getResources().getDimensionPixelSize(R.dimen.movie_detail_flag_width);
        final int height = getResources().getDimensionPixelSize(R.dimen.movie_detail_flag_height);

        Picasso.with(getActivity())
                .load(mImageHelper.getResizedUrl(flagUrl, width, height, "gif"))
                .into(infoLayout);
    }

    private void populateRelatedMovies() {
        if (Constants.DEBUG) {
            Log.d(LOG_TAG, "populateRelatedMovies");
        }

        mRelatedLayout.removeAllViews();

        if (!PhilmCollections.isEmpty(mMovie.getRelated())) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());

            final View.OnClickListener clickListener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (hasCallbacks()) {
                        getCallbacks().showMovieDetail((PhilmMovie) view.getTag());
                    }
                }
            };

            final int relatedWidth = mRelatedLayout.getWidth();
            final int itemWidth = getResources().getDimensionPixelSize(R.dimen.movie_detail_related_item_width);

            final int maxNumItems = relatedWidth / itemWidth;
            final int maxNumMovieItems = maxNumItems - 1;

            final int numMovieItems = Math.min(maxNumMovieItems, mMovie.getRelated().size());

            for (int i = 0; i < numMovieItems; i++) {
                View view = inflater.inflate(R.layout.item_related_movie, mRelatedLayout, false);

                final PhilmMovie movie = mMovie.getRelated().get(i);

                final TextView title = (TextView) view.findViewById(R.id.textview_title);
                title.setText(movie.getTitle());

                final PhilmImageView imageView =
                        (PhilmImageView) view.findViewById(R.id.imageview_poster);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.loadPosterUrl(movie, new Callback() {
                    @Override
                    public void onSuccess() {
                        title.setVisibility(View.GONE);
                    }

                    @Override
                    public void onError() {
                        title.setVisibility(View.VISIBLE);
                    }
                });

                view.setOnClickListener(clickListener);
                view.setTag(movie);

                mRelatedLayout.addView(view);
            }

            View moreView = inflater.inflate(R.layout.item_more, mRelatedLayout, false);
            moreView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (hasCallbacks()) {
                        getCallbacks().showRelatedMovies(mMovie);
                    }
                }
            });
            mRelatedLayout.addView(moreView);
        }
    }

    private void updateButtonState(CheckableImageButton button, final boolean checked,
            final int toCheckDesc, final int toUncheckDesc) {
        button.setChecked(checked);
        if (checked) {
            button.setContentDescription(getString(toUncheckDesc));
        } else {
            button.setContentDescription(getString(toCheckDesc));
        }
    }

    @Override
    public final void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_seen:
                if (hasCallbacks()) {
                    getCallbacks().toggleMovieSeen(mMovie);
                }
                break;
            case R.id.btn_watchlist:
                if (hasCallbacks()) {
                    getCallbacks().toggleInWatchlist(mMovie);
                }
                break;
            case R.id.btn_collection:
                if (hasCallbacks()) {
                    getCallbacks().toggleInCollection(mMovie);
                }
                break;
            case R.id.textview_summary:
                final int defaultMaxLines = getResources()
                        .getInteger(R.integer.default_summary_maxlines);
                if (mSummaryTextView.getLineCount() == defaultMaxLines) {
                    mSummaryTextView.setMaxLines(Integer.MAX_VALUE);
                } else if (mSummaryTextView.getLineCount() > defaultMaxLines) {
                    mSummaryTextView.setMaxLines(defaultMaxLines);
                }
                break;
            case R.id.rcv_rating:
                if (hasCallbacks()) {
                    getCallbacks().showRateMovie(mMovie);
                }
                break;
        }
    }
}
