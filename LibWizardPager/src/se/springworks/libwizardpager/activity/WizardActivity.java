package se.springworks.libwizardpager.activity;

import java.util.List;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import co.juliansuarez.libwizardpager.R;
import co.juliansuarez.libwizardpager.wizard.model.AbstractWizardModel;
import co.juliansuarez.libwizardpager.wizard.model.ModelCallbacks;
import co.juliansuarez.libwizardpager.wizard.model.Page;
import co.juliansuarez.libwizardpager.wizard.ui.PageFragmentCallbacks;
import co.juliansuarez.libwizardpager.wizard.ui.ReviewFragment;
import co.juliansuarez.libwizardpager.wizard.ui.StepPagerStrip;

public abstract class WizardActivity extends FragmentActivity implements PageFragmentCallbacks, ReviewFragment.Callbacks, ModelCallbacks {
	private ViewPager mPager;
	private WizardPagerAdapter mPagerAdapter;

	private boolean mEditingAfterReview;

	private AbstractWizardModel mWizardModel;

	private boolean mConsumePageSelectedEvent;

	protected Button mNextButton;
	protected Button mPrevButton;

	private List<Page> mCurrentPageSequence;
	private StepPagerStrip mStepPagerStrip;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_wizard);

		mWizardModel = createWizardModel();
		if (savedInstanceState != null) {
			mWizardModel.load(savedInstanceState.getBundle("model"));
		}
		mWizardModel.registerListener(this);


		mPagerAdapter = new WizardPagerAdapter(getSupportFragmentManager());
		mPager = (ViewPager) findViewById(R.id.pager);
		mPager.setAdapter(mPagerAdapter);
		mStepPagerStrip = (StepPagerStrip) findViewById(R.id.strip);
		mStepPagerStrip.setOnPageSelectedListener(new StepPagerStrip.OnPageSelectedListener() {
			@Override
			public void onPageStripSelected(int position) {
				position = Math.min(mPagerAdapter.getCount() - 1, position);
				if (mPager.getCurrentItem() != position) {
					mPager.setCurrentItem(position);
				}
			}
		});

		mNextButton = (Button) findViewById(R.id.next_button);
		mPrevButton = (Button) findViewById(R.id.prev_button);

		mPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				mStepPagerStrip.setCurrentPage(position);

				if (mConsumePageSelectedEvent) {
					mConsumePageSelectedEvent = false;
					return;
				}

				mEditingAfterReview = false;
				updateBottomBar();
			}
		});

		mNextButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (atLastPage()) {
					onNextAtLastPage();
				}
				else {
					onNextPage();
				}
			}
		});

		mPrevButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				onPreviousPage();
			}
		});

		onPageTreeChanged();
		updateBottomBar();
	}
	
	protected abstract AbstractWizardModel createWizardModel();
	
	protected void hideStepPagerStrip() {
		mStepPagerStrip.setVisibility(View.GONE);
	}
	
	protected void onPreviousPage() {
		mPager.setCurrentItem(mPager.getCurrentItem() - 1);		
	}
	
	protected void onNextPage() {
		if (mEditingAfterReview) {
			moveToLastPage();
		}
		else {
			moveToNextPage();
		}
	}
	
	protected void onNextAtLastPage() {
		// override and take appropriate action
	}

	@Override
	public void onPageTreeChanged() {
		mCurrentPageSequence = mWizardModel.getCurrentPageSequence();
		recalculateCutOffPage();
		mStepPagerStrip.setPageCount(mCurrentPageSequence.size() + (mWizardModel.hasReviewPage() ? 1 : 0));
		mPagerAdapter.notifyDataSetChanged();
		updateBottomBar();
	}
	
	protected boolean atFirstPage() {
		return mPager.getCurrentItem() <= 0;
	}
	
	protected boolean atLastPage() {
		return mPager.getCurrentItem() == (mCurrentPageSequence.size() + (mWizardModel.hasReviewPage() ? 0 : -1));
	}
	
	protected boolean atCutOffPage() {
		return mPager.getCurrentItem() == mPagerAdapter.getCutOffPage();
	}

	protected void updateBottomBar() {
		if (atLastPage()) {
//			mNextButton.setTextAppearance(this, R.style.TextAppearanceFinish);
			mNextButton.setText(R.string.wizard_last);
			mNextButton.setBackgroundResource(R.drawable.finish_background);
		}
		else {
			mNextButton.setText(mEditingAfterReview ? R.string.wizard_review : R.string.wizard_next);
			mNextButton.setBackgroundResource(R.drawable.selectable_item_background);
			TypedValue v = new TypedValue();
			getTheme().resolveAttribute(android.R.attr.textAppearanceMedium, v, true);
			mNextButton.setTextAppearance(this, v.resourceId);
			mNextButton.setEnabled(!atCutOffPage());
		}

		mPrevButton.setVisibility(atFirstPage() ? View.INVISIBLE : View.VISIBLE);
	}
	
	public void moveToLastPage() {
		mPager.setCurrentItem(mPagerAdapter.getCount() - 1);
	}
	
	public void moveToNextPage() {
		mPager.setCurrentItem(mPager.getCurrentItem() + 1);
	}
	
	public void moveToPreviousPage() {
		mPager.setCurrentItem(mPager.getCurrentItem() - 1);
	}
	
	public void moveToFirstPage() {
		mPager.setCurrentItem(0);
	}


	@Override
	protected void onDestroy() {
		super.onDestroy();
		mWizardModel.unregisterListener(this);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBundle("model", mWizardModel.save());
	}

	@Override
	public AbstractWizardModel onGetModel() {
		return mWizardModel;
	}

	@Override
	public void onEditScreenAfterReview(String key) {
		for (int i = mCurrentPageSequence.size() - 1; i >= 0; i--) {
			if (mCurrentPageSequence.get(i).getKey().equals(key)) {
				mConsumePageSelectedEvent = true;
				mEditingAfterReview = true;
				mPager.setCurrentItem(i);
				updateBottomBar();
				break;
			}
		}
	}

	@Override
	public void onPageDataChanged(Page page) {
		if (page.isRequired()) {
			if (recalculateCutOffPage()) {
				mPagerAdapter.notifyDataSetChanged();
				updateBottomBar();
			}
		}
	}

	@Override
	public Page onGetPage(String key) {
		return mWizardModel.findByKey(key);
	}

	private boolean recalculateCutOffPage() {
		// Cut off the pager adapter at first required page that isn't completed
		int cutOffPage = mCurrentPageSequence.size() + 1;
		for (int i = 0; i < mCurrentPageSequence.size(); i++) {
			Page page = mCurrentPageSequence.get(i);
			if (page.isRequired() && !page.isCompleted()) {
				cutOffPage = i;
				break;
			}
		}

		if (mPagerAdapter.getCutOffPage() != cutOffPage) {
			mPagerAdapter.setCutOffPage(cutOffPage);
			return true;
		}

		return false;
	}

	
	
	
	
	
	
	public class WizardPagerAdapter extends FragmentStatePagerAdapter {
		private int mCutOffPage;
		private Fragment mPrimaryItem;

		public WizardPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int i) {
			if (i >= mCurrentPageSequence.size()) {
				return new ReviewFragment();
			}

			return mCurrentPageSequence.get(i).createFragment();
		}

		@Override
		public int getItemPosition(Object object) {
			// TODO: be smarter about this
			if (object == mPrimaryItem) {
				// Re-use the current fragment (its position never changes)
				return POSITION_UNCHANGED;
			}

			return POSITION_NONE;
		}

		@Override
		public void setPrimaryItem(ViewGroup container, int position, Object object) {
			super.setPrimaryItem(container, position, object);
			mPrimaryItem = (Fragment) object;
		}

		@Override
		public int getCount() {
			int count = Math.min(mCutOffPage + 1, mCurrentPageSequence == null ? 1 : mCurrentPageSequence.size());
			if(mWizardModel.hasReviewPage()) {
				count++;
			}
			return count;
		}

		public void setCutOffPage(int cutOffPage) {
			if (cutOffPage < 0) {
				cutOffPage = Integer.MAX_VALUE;
			}
			mCutOffPage = cutOffPage;
		}

		public int getCutOffPage() {
			return mCutOffPage;
		}
	}
}
